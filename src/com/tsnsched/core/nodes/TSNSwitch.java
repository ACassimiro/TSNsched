package com.tsnsched.core.nodes;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.

import java.io.Serializable;
import java.util.ArrayList;

import com.microsoft.z3.*;
import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.FlowFragment;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.interface_manager.Printer;
import com.tsnsched.core.network.*;
import com.tsnsched.core.nodes.*;
import com.tsnsched.core.schedule_generator.*;


/**
 * [Class]: TSNSwitch
 * [Usage]: This class contains the information needed to 
 * specify a switch capable of complying with the TSN patterns
 * to the schedule. Aside from part of the z3 data used to 
 * generate the schedule, objects created from this class
 * are able to organize a sequence of ports that connect the
 * switch to other nodes in the network.
 */
public class TSNSwitch extends Switch implements Serializable {

	private Boolean isModifiedOrCreated = true;
	private Boolean useSameCycleStart = false;
	private Double firstCycleStart = 0.0;
	
	private static final long serialVersionUID = 1L;
	// private Cycle cycle;
    private ArrayList<String> connectsTo;
    private ArrayList<Port> ports;
    private double cycleDurationUpperBound;
    private double cycleDurationLowerBound;
    
    private transient Printer printer;
    
    private double gbSize;
    private transient RealExpr gbSizeZ3; // Size of the guardBand
    private transient RealExpr cycleDuration;
    private transient RealExpr cycleStart;
    private transient RealExpr cycleDurationUpperBoundZ3;
    private transient RealExpr cycleDurationLowerBoundZ3;
    private int portNum = 0;
    
    private ScheduleType scheduleType = ScheduleType.DEFAULT;
   

	private static int indexCounter = 0; 
    
    
    /**
     * [Method]: TSNSwitch
     * [Usage]: Overloaded constructor method of this class.
     * Creates a switch, giving it a name and creating a new
     * list of ports and labels of devices that it can reach.
     * 
     * Can be adapted in the future to start the switch with 
     * default properties.
     * 
     * @param name      Name of the switch
     */
    public TSNSwitch(String name) {
        this.name = name;
        ports = new ArrayList<Port>();
        this.connectsTo = new ArrayList<String>();
    }
    
    /**
     * [Method]: TSNSwitch
     * [Usage]: Overloaded constructor method of this class.
     * Instantiates a new TSNSwitch object setting up its properties
     * that are given as parameters. There is no transmission time here,
     * as this method is used when considering that it will be calculated 
     * by the packet size divided by the port speed 
     * 
     * @param name                  Name of the switch
     * @param maxPacketSize         Maximum packet size supported by the switch
     * @param timeToTravel          Time that a packet takes to leave its port and reach the destination
     * @param portSpeed             Transmission speed of the port
     * @param gbSize                Size of the guard bands used to separate non consecutive time slots
     */
    public TSNSwitch(String name,
                     double maxPacketSize,
                     double timeToTravel,
                     double portSpeed,
                     double gbSize) {
        this.name = name;
        this.maxPacketSize = maxPacketSize;
        this.timeToTravel = timeToTravel;
        this.transmissionTime = 0;
        this.portSpeed = portSpeed;
        this.gbSize = gbSize;
        this.ports = new ArrayList<Port>();
        this.connectsTo = new ArrayList<String>();
    }
    
    /**
     * [Method]: TSNSwitch
     * [Usage]: Overloaded constructor method of this class.
     * Instantiates a new TSNSwitch object setting up its properties
     * that are given as parameters.
     * 
     * @param name                  Name of the switch
     * @param maxPacketSize         Maximum packet size supported by the switch
     * @param timeToTravel          Time that a packet takes to leave its port and reach the destination
     * @param transmissionTime      Time taken to process the packet inside the switch
     * @param portSpeed             Transmission speed of the port
     * @param gbSize                Size of the guard bands used to separate non consecutive time slots
     */
    public TSNSwitch(String name,
                     double maxPacketSize,
                     double timeToTravel,
                     double transmissionTime,
                     double portSpeed,
                     double gbSize,
                     double cycleDurationLowerBound,
                     double cycleDurationUpperBound) {
        this.name = name;
        this.maxPacketSize = maxPacketSize;
        this.timeToTravel = timeToTravel;
        this.transmissionTime = transmissionTime;
        this.portSpeed = portSpeed;
        this.gbSize = gbSize;
        this.ports = new ArrayList<Port>();
        this.connectsTo = new ArrayList<String>();
        this.cycleDurationLowerBound = cycleDurationLowerBound;
        this.cycleDurationUpperBound = cycleDurationUpperBound;
    }

    /**
     * [Method]: TSNSwitch
     * [Usage]: Overloaded constructor method of this class.
     * Instantiates a new TSNSwitch object setting up its properties
     * that are given as parameters. There is no transmission time here,
     * as this method is used when considering that it will be calculated 
     * by the packet size divided by the port speed 
     * 
     * @param name                  Name of the switch
     * @param maxPacketSize         Maximum packet size supported by the switch
     * @param timeToTravel          Time that a packet takes to leave its port and reach the destination
     * @param portSpeed             Transmission speed of the port
     * @param gbSize                Size of the guard bands used to separate non consecutive time slots
     */
    public TSNSwitch(String name,
                     double maxPacketSize,
                     double timeToTravel,
                     double portSpeed,
                     double gbSize,
                     double cycleDurationLowerBound,
                     double cycleDurationUpperBound) {
        this.name = name;
        this.maxPacketSize = maxPacketSize;
        this.timeToTravel = timeToTravel;
        this.transmissionTime = 0;
        this.portSpeed = portSpeed;
        this.gbSize = gbSize;
        this.ports = new ArrayList<Port>();
        this.connectsTo = new ArrayList<String>();
        this.cycleDurationLowerBound = cycleDurationLowerBound;
        this.cycleDurationUpperBound = cycleDurationUpperBound;
    }
    
    public TSNSwitch(String name,
            double timeToTravel,
            double portSpeed,
            double gbSize) {
		this.name = name;
		this.timeToTravel = timeToTravel;
		this.transmissionTime = 0;
		this.portSpeed = portSpeed;
		this.gbSize = gbSize;
		this.ports = new ArrayList<Port>();
		this.connectsTo = new ArrayList<String>();
	}
    

    /**
     * [Method]: toZ3
     * [Usage]: After setting all the numeric input values of the class,
     * generates the z3 equivalent of these values and creates any extra
     * variable needed.
     * 
     * @param ctx      Context variable containing the z3 environment used
     */
    public void toZ3(Context ctx, Solver solver) {
        this.cycleDurationLowerBoundZ3 = ctx.mkReal(Double.toString(cycleDurationLowerBound));
        this.cycleDurationUpperBoundZ3 = ctx.mkReal(Double.toString(cycleDurationUpperBound));
        
        // Creating the cycle duration and start for this switch
        this.cycleDuration = ctx.mkRealConst("cycleOf" + this.name + "Duration");
        this.cycleStart = ctx.mkRealConst("cycleOf" + this.name + "Start");
        

        // Creating the cycle setting up the bounds for the duration (Cycle duration constraint)
        /*
        solver.add(
            ctx.mkGe(this.cycleDuration, this.cycleDurationLowerBoundZ3)
        );
        solver.add(
            ctx.mkLe(this.cycleDuration, this.cycleDurationUpperBoundZ3)
        );
        */
        // A cycle must start on a point in time, so it must be greater than 0
        solver.add( // No negative cycle values constraint
            ctx.mkGe((RealExpr) this.cycleStart, (IntExpr) ctx.mkInt(0))
        );

                
        for (Port port : this.ports) {
            port.toZ3(ctx);
            
            for(FlowFragment frag : port.getFlowFragments()) {
                solver.add( // Maximum cycle start constraint
                    ctx.mkLe(
                        port.getCycle().getFirstCycleStartZ3(), 
                        this.arrivalTime(ctx, 0, frag)
                    )
                );
            }

            solver.add( // No negative cycle values constraint
        		ctx.mkGe(port.getCycle().getFirstCycleStartZ3(), ctx.mkInt(0))
    		);
        
            /* The cycle of every port must have the same duration
            solver.add(ctx.mkEq( // Equal cycle constraints
                this.cycleDuration, 
                port.getCycle().getCycleDurationZ3()
            ));
            /**/

            // The cycle of every port must have the same starting point
            /*
            if(this.useSameCycleStart) {
	            solver.add(ctx.mkEq( // Equal cycle constraints
	                this.cycleStart, 
	                port.getCycle().getFirstCycleStartZ3()
	            ));
            }
            /**/
            
        }

        solver.add(ctx.mkEq(
            this.cycleStart, 
            ctx.mkInt(Double.toString(this.firstCycleStart))
        ));
        
    }
    
    /**
     * [Method]: setupSchedulingRules
     * [Usage]: Iterates over the ports of the switch, calling the
     * method responsible for setting up the rules for each individual port
     * on the switch.
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     */
    public void setupSchedulingRules(Solver solver, Context ctx) {
                
        for(Port port : this.ports) {
        	if(port.getIsModifiedOrCreated() || port.checkIfHasIncrement()) {
        		port.setupSchedulingRules(solver, ctx);        		
        	}
        }
        
    }

    /**
     * [Method]: createPort
     * [Usage]: Adds a port to the switch. A cycle to that port and
     * the device object that it connects to (since TSN ports connect to 
     * individual nodes in the approach of this schedule) must be given
     * as parameters.
     * 
     * @param destination       Destination of the port as TSNSwitch or Device
     * @param cycle             Cycle used by the port
     */
    public Port createPort(Object destination, Cycle cycle) {
        String destinationName = "";
    	
        if(destination instanceof Device) {
            destinationName = ((Device)destination).getName();
        } else if (destination instanceof Switch) {
            destinationName = ((Switch)destination).getName();
        }
        else
            return null; // [TODO]: THROW ERROR
  
        Port newPort = this.createPort(destinationName, cycle);
        
        //this.portNum++;
        
        return newPort;
    }
    
    /**
     * [Method]: createPort
     * [Usage]: Adds a port to the switch. A cycle to that port and
     * the device name that it connects to (since TSN ports connect to 
     * individual nodes in the approach of this schedule) must be given
     * as parameters.
     * 
     * @param destination       Name of the destination of the port 
     * @param cycle             Cycle used by the port
     */
    public Port createPort(String destination, Cycle cycle) {
        this.connectsTo.add(destination);
        
        Port newPort = new Port(this.name + "Port" + this.portNum,
        		this.portNum,
                destination,
                this.maxPacketSize,
                this.timeToTravel,
                this.transmissionTime,
                this.portSpeed,
                this.gbSize,
                cycle
            );
        
        newPort.setPortNum(this.portNum);
        newPort.setHostSwitch(this);
        
        switch(this.scheduleType) {
        	case MICROCYCLES:
        		newPort.setUseMicroCycles(true);
        		break;
        	case HYPERCYCLES:
        		newPort.setUseHyperCycle(true);
        		break;
        	case DEFAULT:
        		newPort.setUseHyperCycle(true);
        		//newPort.setUseMicroCycles(true);
        		break;
        }
        
        this.ports.add(newPort);
        
        this.portNum++;
        
        return newPort;
    }
    
    
    /**
     * [Method]: addToFragmentList
     * [Usage]: Given a flow fragment, it finds the port that connects to
     * its destination and adds the it to the fragment list of that specific 
     * port.
     * 
     * @param flowFrag      Fragment of a flow to be added to a port
     */
    public void addToFragmentList(FlowFragment flowFrag) {
        int index = this.connectsTo.indexOf(flowFrag.getNextHop());
        
        /*
        this.printer.printIfLoggingIsEnabled("Current node: " + flowFrag.getNodeName());
        this.printer.printIfLoggingIsEnabled("Next hop: " + flowFrag.getNextHop());
        this.printer.printIfLoggingIsEnabled("Index of port: " + index);	
        System.out.print("Connects to: ");
        for(String connect : this.connectsTo) {
            System.out.print(connect + ", ");
        }
        
        this.printer.printIfLoggingIsEnabled("");
        this.printer.printIfLoggingIsEnabled("------------------");
        
        /**/
        
        this.ports.get(index).addToFragmentList(flowFrag);
    }
    
    
    /**
     * [Method]: getPortOf
     * [Usage]: Given a name of a node, returns the port that 
     * can reach this node.
     * 
     * @param name      Name of the node that the switch is connects to
     * @return          Port of the switch that connects to a given node
     */
    public Port getPortOf(String name) {
        int index = this.connectsTo.indexOf(name);
        
        //this.printer.printIfLoggingIsEnabled("On switch " + this.getName() + " looking for port to " + name);
         
        Port port = this.ports.get(index);
        
        return port;        
    }
    
    /**
     * [Method]: setUpCycleSize
     * [Usage]: Iterate over its ports. The ones using automated application
     * periods will calculate their cycle size.
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     */
    public void setUpCycleSize(Solver solver, Context ctx) {
    	for(Port port : this.ports) {
    		port.setUpCycle();
    	}
    }
        
    
    /**
     * [Method]: arrivalTime
     * [Usage]: Retrieves the arrival time of a packet from a flow fragment
     * specified by the index given as a parameter. The arrival time is the 
     * time when a packet reaches this switch's port.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex      Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    public RealExpr arrivalTime(Context ctx, int auxIndex, FlowFragment flowFrag){
        IntExpr index = ctx.mkInt(auxIndex);
        int portIndex = this.connectsTo.indexOf(flowFrag.getNextHop());

        return (RealExpr) this.ports.get(portIndex).arrivalTime(ctx, auxIndex, flowFrag);
     }
    
    
    /**
     * [Method]: arrivalTime
     * [Usage]: Retrieves the arrival time of a packet from a flow fragment
     * specified by the index given as a parameter. The arrival time is the 
     * time when a packet reaches this switch's port.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex      Index of the packet of the flow fragment as a z3 variable
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     *
    public RealExpr arrivalTime(Context ctx, IntExpr index, FlowFragment flowFrag){
        int portIndex = this.connectsTo.indexOf(flowFrag.getNextHop());
        return (RealExpr) this.ports.get(portIndex).arrivalTime(ctx, index, flowFrag);
    }
    /**/
    
    /**
     * [Method]: departureTime
     * [Usage]: Retrieves the departure time of a packet from a flow fragment
     * specified by the index given as a parameter. The departure time is the 
     * time when a packet leaves its previous node with this switch as a destination.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param index         Index of the packet of the flow fragment as a z3 variable
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    public RealExpr departureTime(Context ctx, IntExpr index, FlowFragment flowFrag){
        int portIndex = this.connectsTo.indexOf(flowFrag.getNextHop());
        return (RealExpr) this.ports.get(portIndex).departureTime(ctx, index, flowFrag);
    }
    /**/
    
    /**
     * [Method]: departureTime
     * [Usage]: Retrieves the departure time of a packet from a flow fragment
     * specified by the index given as a parameter. The departure time is the 
     * time when a packet leaves its previous node with this switch as a destination.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex         Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    public RealExpr departureTime(Context ctx, int auxIndex, FlowFragment flowFrag){
        IntExpr index = ctx.mkInt(auxIndex);
        
        int portIndex = this.connectsTo.indexOf(flowFrag.getNextHop());
        return (RealExpr) this.ports.get(portIndex).departureTime(ctx, index, flowFrag);
     }
  
    /**
     * [Method]: scheduledTime
     * [Usage]: Retrieves the scheduled time of a packet from a flow fragment
     * specified by the index given as a parameter. The scheduled time is the 
     * time when a packet leaves this switch for its next destination.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param index         Index of the packet of the flow fragment as a z3 variable
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the scheduled time of the desired packet
     *
    public RealExpr scheduledTime(Context ctx, IntExpr index, FlowFragment flowFrag){
        int portIndex = this.connectsTo.indexOf(flowFrag.getNextHop());
        return (RealExpr) this.ports.get(portIndex).scheduledTime(ctx, index, flowFrag);
    }
    /**/
    
    /**
     * [Method]: scheduledTime
     * [Usage]: Retrieves the scheduled time of a packet from a flow fragment
     * specified by the index given as a parameter. The scheduled time is the 
     * time when a packet leaves this switch for its next destination.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex      Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the scheduled time of the desired packet
     */
    public RealExpr scheduledTime(Context ctx, int auxIndex, FlowFragment flowFrag){
        // IntExpr index = ctx.mkInt(auxIndex);
        
        int portIndex = this.connectsTo.indexOf(flowFrag.getNextHop());
        
        return (RealExpr) this.ports.get(portIndex).scheduledTime(ctx, auxIndex, flowFrag);
    }
    
    
    public void loadZ3(Context ctx, Solver solver) {
    	/*
    	solver.add(
			ctx.mkEq(
				this.cycleDurationUpperBoundZ3,
				ctx.mkReal(Double.toString(this.cycleDurationUpperBound))
			)	
		);
    	
    	solver.add(
			ctx.mkEq(
				this.cycleDurationLowerBoundZ3,
				ctx.mkReal(Double.toString(this.cycleDurationLowerBound))
			)	
		);
    	*/

        this.cycleStart = ctx.mkRealConst("cycleOf" + this.name + "Start"); 
        solver.add(ctx.mkEq(
                this.cycleStart, 
                ctx.mkReal(Double.toString(this.firstCycleStart))
            ));
        	
    	if(!ports.isEmpty()) {
    		for(Port port : this.ports) {
//  			System.out.print("Should load port " + port.getName() + "(" + port.getCycle().getName() + ")? ");
//    			System.out.println(port.getIsModifiedOrCreated());
    			if(!port.getIsModifiedOrCreated() || port.getModificationType() == NetworkProperties.INCREMENTFLOW) {
//    				System.out.println("yes");
    				port.loadZ3(ctx, solver);    		
    				   
    			} else {
//    				System.out.println("no");
    				;
    			}
    		}
    	}
    	

       
    }
    
    
    /*
     *  GETTERS AND SETTERS
     */
    
    public ArrayList<Double> getSlotStartList(String nameOfDestination, int prt) {
        return this.getPortOf(nameOfDestination).getCycle().getSlotStartList(prt);
    }
       
    public ArrayList<Double>  getSlotDurationList(String nameOfDestination, int prt) {
        return this.getPortOf(nameOfDestination).getCycle().getSlotDurationList(prt);
    }   
    
    public Cycle getCycle(int index) {
        
        return this.ports.get(index).getCycle();
    }

    public void setCycle(Cycle cycle, int index) {
        this.ports.get(index).setCycle(cycle);
    }

    public double getGbSize() {
        return gbSize;
    }

    public void setGbSize(double gbSize) {
        this.gbSize = gbSize;
    }

    public RealExpr getGbSizeZ3() {
        return gbSizeZ3;
    }

    public void setGbSizeZ3(RealExpr gbSizeZ3) {
        this.gbSizeZ3 = gbSizeZ3;
    }

    public ArrayList<Port> getPorts() {
        return ports;
    }

    public void setPorts(ArrayList<Port> ports) {
        this.ports = ports;
    }

    public void addPort(Port port, String name) {
        this.ports.add(port);
        this.connectsTo.add(name);
    }
    
    public RealExpr getCycleDuration() {
        return cycleDuration;
    }

    public void setCycleDuration(RealExpr cycleDuration) {
        this.cycleDuration = cycleDuration;
    }

    public RealExpr getCycleStart() {
        return cycleStart;
    }

    public void setCycleStart(RealExpr cycleStart) {
        this.cycleStart = cycleStart;
    }

	public Boolean getIsModifiedOrCreated() {
		return isModifiedOrCreated;
	}

	public void setIsModifiedOrCreated(Boolean isModifiedOrCreated) {
		this.isModifiedOrCreated = isModifiedOrCreated;
	}
	
	public ArrayList<String> getConnectsTo(){
		return this.connectsTo;
	}
		 
    public ScheduleType getScheduleType() {
		return scheduleType;
	}

	public void setScheduleType(ScheduleType scheduleType) {
		this.scheduleType = scheduleType;
	}

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}    

	public Boolean getUseSameCycleStart() {
		return useSameCycleStart;
	}

	public void setUseSameCycleStart(Boolean useSameCycleStart) {
		this.useSameCycleStart = useSameCycleStart;
	}

}
