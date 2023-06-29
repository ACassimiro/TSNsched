package com.tsnsched.core.network;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.microsoft.z3.*;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.FlowFragment;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.interface_manager.Printer;
import com.tsnsched.core.nodes.*;
import com.tsnsched.core.nodes.Switch;
import com.tsnsched.core.nodes.TSNSwitch;

/**
 * [Class]: Network
 * [Usage]: Using this class, the user can specify the network
 * topology using switches and flows. The network will be given
 * to the scheduler generator so it can iterate over the network's
 * flows and switches setting up the scheduling rules.
 *
 */
public class Network implements Serializable {
	private NetworkModificationHandler netModHandler;
	private Boolean hasBeenModified = false;
	
	private transient Printer printer;

	private static final long serialVersionUID = 1L;
	String db_name;
	String file_id;
	
	private int networkFlowCount = 0;
	
	//TODO: Remove debugging variables:
    public transient RealExpr avgOfAllLatency;
    public transient ArrayList<RealExpr> avgLatencyPerDev = new ArrayList<RealExpr>();
    

    private ArrayList<Device> devices;
    private ArrayList<Switch> switches;
    private ArrayList<Flow> flows;
    private double timeToTravel;
    public transient ArrayList<RealExpr> allSumOfJitter = new ArrayList<RealExpr>();
    public ArrayList<Integer> numberOfNodes = new ArrayList<Integer>();
    
    public static int PACKETUPPERBOUNDRANGE = 5; // Limits the applications of rules to the packets
    public static int CYCLEUPPERBOUNDRANGE = 25; // Limits the applications of rules to the cycles
    
    private double jitterUpperBoundRange = -1;
	transient RealExpr jitterUpperBoundRangeZ3;

    /**
     * [Method]: Network
     * [Usage]: Default constructor method of class Network. Creates
     * the ArrayLists for the switches and flows. Sets up the default
     * time to travel for 2 (Needs revision).
     */
    public Network (double jitterUpperBoundRange) {
        this.jitterUpperBoundRange = jitterUpperBoundRange;
        this.switches = new ArrayList<Switch>();
        this.flows = new ArrayList<Flow>();
        this.devices = new ArrayList<Device>();
        this.timeToTravel = 2;        
        netModHandler = new NetworkModificationHandler();
    }
    
    
    /**
     * [Method]: Network
     * [Usage]: Default constructor method of class Network. Creates
     * the ArrayLists for the switches and flows. Sets up the default
     * time to travel for 2 (Needs revision).
     */
    public Network () {
        this.switches = new ArrayList<Switch>();
        this.flows = new ArrayList<Flow>();
        this.devices = new ArrayList<Device>();
        this.timeToTravel = 2;
        netModHandler = new NetworkModificationHandler();
    }
    
    /**
     * [Method]: Network
     * [Usage]: Overloaded constructor method of class Network. Will
     * create a network with the given ArrayLists for switches, flows
     * and time to travel.
     * 
     * @param switches      ArrayList with the instances of switches of the network
     * @param flows         ArrayList with the instances of flows of the network 
     * @param timeToTravel  Value used as travel time from a node to another in the network
     */
    public Network (ArrayList<Switch> switches, ArrayList<Flow> flows, double timeToTravel) {
        this.switches = switches;
        this.flows = flows;
        this.timeToTravel = timeToTravel;
        netModHandler = new NetworkModificationHandler();
    }
    
    /**
     * [Method]: SecureHC
     * [Usage]: Iterates over the flows of the network, assuring that
     * each flow will have its hard constraint established.
     * 
     * @param solver    z3 solver object used to discover the variables' values
     * @param ctx       z3 context which specify the environment of constants, functions and variables
     */
    public void secureHC(Solver solver, Context ctx) {
    	avgLatencyPerDev = new ArrayList<RealExpr>();
        
    	
        if(jitterUpperBoundRange != -1) { // If there is a value on the upperBoundRange, it was set through the network
            this.setJitterUpperBoundRangeZ3(ctx, this.jitterUpperBoundRange);
        }
        
        Stack<RealExpr> jitterList = new Stack<RealExpr>();
        int totalNumOfLeaves = 0;
        RealExpr sumOfAllJitter;
        // On every switch, set up the constraints of the schedule        
        //switch1.setupSchedulingRules(solver, ctx);
        
        
        for (Switch swt : this.getSwitches()) {
            ((TSNSwitch) swt).setupSchedulingRules(solver, ctx);;
        }
        
        /*
         *  Iterate over the flows. Get last and first fragment of the flow
         *  make sure last scheduled time minus first departure time is lesser
         *  than HC. For publish subscribe flows, make sure that every flow fragment
         *  of all fathers of all leaves have their scheduled time minus the 
         *  departure time of the first child of the root lesser than the hard
         *  constraint  
         */
        
        for(Flow flw : this.getFlows()) {
        	flw.setNumberOfPacketsSent(flw.getPathTree().getRoot());

            flw.bindAllFragments(solver, ctx);

            solver.add( // No negative cycle values constraint
                ctx.mkGe(
                    flw.getStartDevice().getFirstT1TimeZ3(),
                    (RealExpr) ctx.mkReal(0)
                )
            );
            solver.add( // Maximum transmission offset constraint
                ctx.mkLe(
                    flw.getStartDevice().getFirstT1TimeZ3(),
                    flw.getStartDevice().getPacketPeriodicityZ3() 
                )
            );
            
            
            
            if(flw.getType() == Flow.UNICAST) {
                
                ArrayList<FlowFragment> currentFrags = flw.getFlowFragments();
                ArrayList<Switch> path = flw.getPath();
                
                
                //Make sure that HC is respected
                for(int i = 0; i < flw.getNumOfPacketsSent(); i++) {
                    solver.add(
                            ctx.mkLe(
                                ctx.mkSub(
                                    ((TSNSwitch) path.get(path.size() - 1)).scheduledTime(ctx, i, currentFrags.get(currentFrags.size() - 1)),
                                    ((TSNSwitch) path.get(0)).departureTime(ctx, i, currentFrags.get(0))
                                ),
                                flw.getStartDevice().getHardConstraintTimeZ3()  
                            )                   
                      );
                }
               
            } else if (flw.getType() == Flow.PUBLISH_SUBSCRIBE) {
                PathNode root = flw.getPathTree().getRoot();
                ArrayList<PathNode> leaves = flw.getPathTree().getLeaves();
                ArrayList<PathNode> parents = new ArrayList<PathNode>();
                
                // Make list of parents of all leaves
                for(PathNode leaf : leaves) {
                    
                    if(!parents.contains(leaf.getParent())){
                        parents.add(leaf.getParent());
                    }
                    
                    
                    // Set the maximum allowed jitter
                    for(int index = 0; index < flw.getNumOfPacketsSent(); index++) {
                    	solver.add( // Maximum allowed jitter constraint
                            ctx.mkLe(
                                flw.getJitterZ3((Device) leaf.getNode(), solver, ctx, index),
                                (flw.getFlowMaximumJitter() < 0 ? this.jitterUpperBoundRangeZ3 : ctx.mkReal(Double.toString(flw.getFlowMaximumJitter())))
                            )
                        );
                    }
                    
                }
                
             // Iterate over the flows of each leaf parent, assert HC
                for(PathNode parent : parents) {
                    for(FlowFragment ffrag : parent.getFlowFragments()) {
                    	for(int i = 0; i < flw.getNumOfPacketsSent(); i++) {

                			solver.add( // Maximum Allowed Latency constraint
                                ctx.mkLe(
                                		ctx.mkAdd(
                                				ctx.mkReal(Double.toString(ffrag.getParent().getPacketSize()/
                                						((TSNSwitch) root.getChildren().get(0).getNode()).getPortOf(ffrag.getParent().getStartDeviceName()).getPortSpeed()))                              				
                                				,ctx.mkSub(
                                                        ((TSNSwitch) parent.getNode()).scheduledTime(ctx, i, ffrag),
                                                        ((TSNSwitch) root.getChildren().get(0).getNode()).departureTime(ctx, i, 
                                                            root.getChildren().get(0).getFlowFragments().get(0)
                                                        )
                                                    )
                                				)
                                    ,
                                    ctx.mkReal(Double.toString(flw.getFlowMaximumLatency()))
                                )                   
                            );

                        }
                    } 
                    
                }
                
                /*
                
                // TODO: CHECK FAIRNESS CONSTRAINT (?)
                
                sumOfAllJitter = flw.getSumOfAllDevJitterZ3(solver, ctx, Network.PACKETUPPERBOUNDRANGE - 1);
                
                jitterList.push(sumOfAllJitter);
                totalNumOfLeaves += flw.getPathTree().getLeaves().size();
                
                // SET THE MAXIMUM JITTER FOR THE FLOW    
                solver.add(
                    ctx.mkLe(
                        ctx.mkDiv(
                            sumOfAllJitter,
                            ctx.mkReal(flw.getPathTree().getLeaves().size() * (PACKETUPPERBOUNDRANGE))
                        ),  
                        jitterUpperBoundRangeZ3
                    )
                );
                */
                
                avgOfAllLatency = flw.getAvgLatency(solver, ctx);
                for(PathNode node : flw.getPathTree().getLeaves()) {
                    Device endDev = (Device) node.getNode();
                    
                    this.avgLatencyPerDev.add(
                        (RealExpr) ctx.mkDiv(
                            flw.getSumOfJitterZ3(endDev, solver, ctx, flw.getNumOfPacketsSent() - 1),
                            ctx.mkReal(flw.getNumOfPacketsSent())
                        )
                    );
                }
                
            }
        
        }
        
    }
    
    
    /**
     * [Method]: loadNetwork
     * [Usage]: From the primitive values retrieved in the object
     * deserialization process, instantiate the z3 objects that represent
     * the same properties.
     * 
     * @param ctx		Context object for the solver
     * @param solver	Solver object
     */
    public void loadNetwork(Context ctx, Solver solver) {
    	// TODO: Don't forget to load the values of this class
    	boolean hasFlow = false;
    	
    	if(this.jitterUpperBoundRange < 0) {
    		this.jitterUpperBoundRangeZ3 = ctx.mkRealConst("networkJitterUpperboundRange");
    	} else {
    		this.jitterUpperBoundRangeZ3 = ctx.mkReal(Double.toString(this.jitterUpperBoundRange));
    	}
    	
    	// On all network flows: Data given by the user will be converted to z3 values 
       for(Flow flw : this.flows) {
           // flw.toZ3(ctx);
    	   if(flw.getIsModifiedOrCreated())
    		   continue;
    	   
    	   
    	   flw.setFlowPriorityZ3(ctx.mkIntConst(flw.getName() + "Priority"));
    	   flw.setFlowFirstSendingTimeZ3(
			   ctx.mkReal(
				   Double.toString(flw.getFlowFirstSendingTime())
			   )
		   );
    	   flw.setFlowSendingPeriodicityZ3(
			   ctx.mkReal(
				   Double.toString(flw.getFlowSendingPeriodicity())
			   )
		   );
    	   ((Device) flw.getPathTree().getRoot().getNode()).toZ3(ctx);
       }
	       
       // On all network switches: Data given by the user will be converted to z3 values
        for(Switch swt : this.switches) {
        	if(swt instanceof TSNSwitch) { 
        		hasFlow = false;
        		for(Port port : ((TSNSwitch) swt).getPorts()) {
        			for(FlowFragment frag : port.getFlowFragments()) {
        				
        				if(frag.getIsModifiedOrCreated() || frag.getParent().getIsModifiedOrCreated()) {
        					continue;
        				}
        					
        				hasFlow = true;
        				
        				//frag.loadValuesFromParent();
        				
        				frag.createNewDepartureTimeZ3List();

                        if(!frag.getDepartureTimeList().isEmpty()) {
        					frag.addDepartureTimeZ3(ctx.mkReal(Double.toString(frag.getDepartureTime(0))));        					
        				}
        				frag.setFlowFirstSendingTimeZ3(ctx.mkReal(
    						Double.toString(frag.getParent().getFlowFirstSendingTime())
						));
        				frag.setPacketSizeZ3(ctx.mkReal(
    						Double.toString(frag.getParent().getPacketSize())
						));
        			}
        		}
        		
        		//((TSNSwitch) swt).toZ3(ctx, solver);
        		((TSNSwitch) swt).loadZ3(ctx, solver);        			
        		
        	}
        }
    }
    
    public void setSolverAndContextForNetModHandler(Solver solver, Context context) {
    	this.netModHandler.setCtx(context);
    	this.netModHandler.setSolver(solver);
    }

    public void modifyElement(Object element, NetworkProperties propertyID, double value) {

    	this.hasBeenModified = true;
    	
    	if(element instanceof Port) {
    		this.netModHandler.modifyProperty((Port)element, propertyID, value);
    	}
    	
    }

    public void preventCollisionOnFirstHop(Solver solver, Context context) {
        List<Flow> listOfFlows = null;

        for(Device dev : this.devices){
            listOfFlows = new ArrayList<Flow>();

            for(Flow flow : this.flows){

                if(flow.getStartDevice().getName().equals(dev.getName())){
                    listOfFlows.add(flow);
                }

            }

            this.assertRulesForCollisionPrevention(listOfFlows, solver, context);

        }

    }

    private void assertRulesForCollisionPrevention(List<Flow> listOfFlows, Solver solver, Context ctx) {

        double currentPortSpeed = -1;

        if(listOfFlows.size() > 1){

            currentPortSpeed =
                    ((TSNSwitch) listOfFlows // From the list of flows
                    .get(0)                  // give me the first one
                    .getPathTree()           // and give me its path tree
                    .getRoot()               // so I can get the source of the flow
                    .getChildren()           // and the first hops from the source
                    .get(0)                  // Since one flow connects to one device, give me the node device it talks to
                    .getNode())              // and get the object of that node, converting it to a tsnswitch
                    .getPortOf(listOfFlows.get(0).getStartDevice().getName()) // Now give me the port that the source device talks to
                    .getPortSpeed();         // and from the port, give me the speed.
        } else {
            return;
        }

        
        for(Flow flowA : listOfFlows) {
        	
        	if(flowA.getFlowFirstSendingTime() == -1) {
        		continue;
        	}
        	

    		this.printer.printIfLoggingIsEnabled("Looking for flows colliding on first hop.");
        	
        	for(Flow flowB : listOfFlows) {
        		
        		if(flowA.getName().equals(flowB.getName())) {
        			continue;
        		}
        		
        		if(flowA.getFlowFirstSendingTime()==flowB.getFlowFirstSendingTime() || 
    			   (
        				flowA.getFlowFirstSendingTime() <= flowB.getFlowFirstSendingTime() &&
        				flowB.getFlowFirstSendingTime() <= flowA.getFlowFirstSendingTime() + flowA.getPacketSize()/currentPortSpeed 
    				) || 
    			   (
           				flowB.getFlowFirstSendingTime() <= flowA.getFlowFirstSendingTime() &&
           				flowA.getFlowFirstSendingTime() <= flowB.getFlowFirstSendingTime() + flowB.getPacketSize()/currentPortSpeed 
       				) 
				) {
        			this.printer.printIfLoggingIsEnabled("ALERT: Collision found on flows with the same source device (" + listOfFlows.get(0).getStartDevice().getName() + "): " + flowA.getName() + ", and " + flowB.getName());
        		}
        		
        	}
        	
        }

      
        List<FlowFragment> listOfFragments = this.getFragmentsOfFirstHop(listOfFlows);

        for(FlowFragment fragA : listOfFragments) {

            for(int i = 0; i < fragA.getNumOfPacketsSent(); i++) {

                for(FlowFragment fragB : listOfFragments) {

                    for(int j = 0; j < fragB.getNumOfPacketsSent(); j++) {

                        if(fragA.getParent().getName().equals(fragB.getParent().getName())){
                            continue;
                        }

                        solver.add(
                            ctx.mkOr(
                                ctx.mkGe(
                                    fragA.getPort().departureTime(ctx, i, fragA),
                                    ctx.mkAdd(
                                        fragB.getPort().departureTime(ctx, j, fragB),
                                        ctx.mkDiv(
                                            fragA.getPacketSizeZ3(),
                                            ctx.mkReal(Double.toString(currentPortSpeed))
                                        )
                                    )
                                ),
                                ctx.mkGe(
                                    fragB.getPort().departureTime(ctx, i, fragB),
                                    ctx.mkAdd(
                                        fragA.getPort().departureTime(ctx, j, fragA),
                                        ctx.mkDiv(
                                            fragB.getPacketSizeZ3(),
                                            ctx.mkReal(Double.toString(currentPortSpeed))
                                        )
                                    )
                                )
                            )
                        );

                        /* FOR DEBUGGING PURPOSES
                        this.printer.printIfLoggingIsEnabled(
                            ctx.mkOr(
                                    ctx.mkGe(
                                            fragA.getPort().departureTime(ctx, i, fragA),
                                            ctx.mkAdd(
                                                    fragB.getPort().departureTime(ctx, j, fragB),
                                                    ctx.mkDiv(
                                                            fragA.getPacketSizeZ3(),
                                                            ctx.mkReal(Double.toString(currentPortSpeed))
                                                    )
                                            )
                                    ),
                                    ctx.mkGe(
                                            fragB.getPort().departureTime(ctx, i, fragB),
                                            ctx.mkAdd(
                                                    fragA.getPort().departureTime(ctx, j, fragA),
                                                    ctx.mkDiv(
                                                            fragB.getPacketSizeZ3(),
                                                            ctx.mkReal(Double.toString(currentPortSpeed))
                                                    )
                                            )
                                    )
                            )
                        );
                        /**/
                    }

                }

            }

        }


        /*
        for(Flow flowA : listOfFlows) {

            for(Flow flowB : listOfFlows) {

                if(flowA.getName().equals(flowB.getName())){
                    continue;
                }

                // THE PACKET LEAVES AFTER THE TRANSMISSION OF ANOTHER CONFLICTING PACKET
                // OR THE PACKET IS TRANSMITTED BEFORE THE TRANSMISSION OF ANOTHER
                solver.add(
                    ctx.mkOr(
                        ctx.mkGe(
                            flowA.getFlowFirstSendingTimeZ3(),
                            ctx.mkAdd(
                                flowB.getFlowFirstSendingTimeZ3(),
                                ctx.mkDiv(
                                    flowA.getPacketSizeZ3(),
                                    ctx.mkReal(String.valueOf(currentPortSpeed))
                                )
                            )
                        ),
                        ctx.mkGe(
                            flowB.getFlowFirstSendingTimeZ3(),
                            ctx.mkAdd(
                                flowA.getFlowFirstSendingTimeZ3(),
                                ctx.mkDiv(
                                    flowB.getPacketSizeZ3(),
                                    ctx.mkReal(String.valueOf(currentPortSpeed))
                                )
                            )
                        )
                    )
                );
                this.printer.printIfLoggingIsEnabled(ctx.mkOr(
                        ctx.mkGe(
                                flowA.getFlowFirstSendingTimeZ3(),
                                ctx.mkAdd(
                                        flowB.getFlowFirstSendingTimeZ3(),
                                        ctx.mkDiv(
                                                flowB.getPacketSizeZ3(),
                                                ctx.mkReal(String.valueOf(currentPortSpeed))
                                        )
                                )
                        ),
                        ctx.mkLe(
                                flowA.getFlowFirstSendingTimeZ3(),
                                ctx.mkAdd(
                                        flowB.getFlowFirstSendingTimeZ3(),
                                        ctx.mkDiv(
                                                flowB.getPacketSizeZ3(),
                                                ctx.mkReal(String.valueOf(currentPortSpeed))
                                        )
                                )
                        )
                ));

            }

        }

         */


    }
    
    public void setAllElementsToNotModified() {
    	
    	for(Switch swt : this.switches) {
    		
    		((TSNSwitch) swt).setIsModifiedOrCreated(false);
    		
    		for(Port port : ((TSNSwitch) swt).getPorts()) {
    			port.setIsModifiedOrCreated(false);
    			port.setModificationType(null);
    			
    			for(FlowFragment frag : port.getFlowFragments()) {
    				frag.setIsModifiedOrCreated(false);
    				frag.setModificationType(null);
    			}
    		}
    		
    	}
    	
    	for(Flow flw : this.flows) {
    		flw.setIsModifiedOrCreated(false);
    		flw.setModificationType(null);
    	}
    	
    }
    
    private List<FlowFragment> getFragmentsOfFirstHop(List<Flow> flowList) {
        List<FlowFragment> flowFragList = new ArrayList<FlowFragment>();

        for(Flow flow : flowList){

            for(PathNode node : flow.getPathTree().getRoot().getChildren()){
                for(FlowFragment frag : node.getFlowFragments()) {
                    flowFragList.add(frag);
                }
            }

        }

        return flowFragList;
    }

    public void assertFirstSendingTimeOfFlows(Solver solver, Context ctx) {

        for(Flow flow : this.flows) {
            flow.assertFirstSendingTime(solver, ctx);
        }

    }

    public void createNewObjects() {
    	this.netModHandler.createNewObjects();
    }
    
    public void applyChangesToSolver() {
    	this.netModHandler.applyChangesToSolver();
    }
    
    
    /*
     * GETTERS AND SETTERS
     */
    
    public Switch getSwitch(String name) {
    	Switch swt = null;
    	
    	for(Switch auxSwt : this.switches) {
    		if(auxSwt.getName().equals(name)) {
    			swt = auxSwt;
    			break;
    		}
    	}
    	
    	return swt;
    }
    
    public RealExpr getJitterUpperBoundRangeZ3() {
        return jitterUpperBoundRangeZ3;
    }

    public void setJitterUpperBoundRangeZ3(RealExpr jitterUpperBoundRange) {
        this.jitterUpperBoundRangeZ3 = jitterUpperBoundRange;
    }
    
    public void setJitterUpperBoundRangeZ3(Context ctx, double auxJitterUpperBoundRange) {
        this.jitterUpperBoundRangeZ3 = ctx.mkReal(String.valueOf(auxJitterUpperBoundRange));
    }
    
    public ArrayList<Switch> getSwitches() {
        return switches;
    }

    public void setSwitches(ArrayList<Switch> switches) {
        this.switches = switches;
    }

    public ArrayList<Flow> getFlows() {
        return flows;
    }

    public void setFlows(ArrayList<Flow> flows) {
        this.flows = flows;
    }
    
    public void addFlow (Flow flw) {
        this.flows.add(flw);
    }
    
    public void addSwitch (Switch swt) {
        this.switches.add(swt);
    }
    
	public NetworkModificationHandler getNetModHandler() {
		return netModHandler;
	}
	
	public void setNetModHandler(NetworkModificationHandler netModHandler) {
		this.netModHandler = netModHandler;
	}
	
	public void addDevice(Device dev) {
		this.devices.add(dev);
	}
	
	public ArrayList<Device> getDevices(){
		return this.devices;
	}
	
	public Device getDevice(String name) { 
		for(Device dev : this.devices) {
			if(dev.getName().equals(name)) {
				return dev;
			}
		}
		
		return null;
	}
	
	public void addElement(Flow flow, NetworkProperties propertyID) {
    	this.hasBeenModified = true;
		this.addFlow(flow);
    	this.netModHandler.addElement(flow, propertyID);
	}


	public Boolean getHasBeenModified() {
		return hasBeenModified;
	}
	

    public double getJitterUpperBoundRange() {
		return jitterUpperBoundRange;
	}


	public void setJitterUpperBoundRange(double jitterUpperBoundRange) {
		this.jitterUpperBoundRange = jitterUpperBoundRange;
	}

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}


}
