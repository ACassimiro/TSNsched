package com.tsnsched.core.network;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.


import java.io.Serializable;
import java.util.*;
import com.microsoft.z3.*;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.FlowFragment;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.nodes.Device;
import com.tsnsched.core.nodes.TSNSwitch;

public class NetworkModificationHandler implements Serializable {
	private static final long serialVersionUID = 1L;
	private transient List<Object> modifiedObjects;
	private transient List<Object> createdObjects;
	private transient List<Object> pendingSetup;
	private transient List<Object> pendingToZ3;
	private transient List<Object> pendingToReset;


	private transient Context ctx;
	private transient Solver solver;
	
	
	public NetworkModificationHandler() {
		this.modifiedObjects = new ArrayList<Object>();
		this.createdObjects = new ArrayList<Object>();
		this.pendingToZ3 = new ArrayList<Object>();
		this.pendingSetup = new ArrayList<Object>();
	}
	
	public void modifyProperty(Port port, NetworkProperties propertyID, double value) {
    	switch(propertyID) {
    		case PORTSPEED:
    			port.setPortSpeed(value);
    			port.setIsModifiedOrCreated(true);
    			break;
    		case TIMETOTRAVEL:

    			break;

		}
    	
    	if(!this.modifiedObjects.contains(port)) {
    		modifiedObjects.add(port);
    	}	
	}
    
	public void modifyProperty(Flow flow, NetworkProperties propertyID, double value) {
    	switch(propertyID) {
    		case PACKETSIZE:
    			flow.setFlowSendingPeriodicity(value);
    			flow.setIsModifiedOrCreated(true);
    			break;
    		case SENDINGPERIODICITY:
    			
    			break;

		}
    	
    	if(!this.modifiedObjects.contains(flow)) {
    		modifiedObjects.add(flow);
    	}	
	}
    
	public void addElement(Flow flow, NetworkProperties propertyID) { 			   		
		flow.setModificationType(propertyID);
    	switch(propertyID) {
    		case ADDFLOW:
    			this.createdObjects.add(flow);
				flow.convertUnicastFlow();
    			flow.setIsModifiedOrCreated(true);   
    			break;
    		case INCREMENTFLOW:
    			this.createdObjects.add(flow);
				flow.convertUnicastFlow();
    			flow.setIsModifiedOrCreated(true);    			   			
    			break;
    		
		}
    	
	}
	
	
	public void resetPortCycleProperties(Port port) {
		Double previousPortCycleSize;
		Double currentPortCycleSize;
		
		port.setListOfPeriods(new ArrayList<Double>());
		
		for(FlowFragment frag : port.getFlowFragments()) {
			
			if(frag.getFlowSendingPeriodicity() == -1) {
				frag.setFlowSendingPeriodicity(frag.getParent().getFlowSendingPeriodicity());
			}
			
			if(!port.getListOfPeriods().contains(frag.getFlowSendingPeriodicity()))
				port.getListOfPeriods().add(frag.getFlowSendingPeriodicity());
			
		}
		
		previousPortCycleSize = port.getDefinedHyperCycleSize();
		port.setUpCycle();
		currentPortCycleSize = port.getDefinedHyperCycleSize();

		// setting the number of scheduled packets per fragment and number of packet sent on flow
		for(FlowFragment frag : port.getFlowFragments()) {
			frag.setNumOfPacketsSent((int)(port.getDefinedHyperCycleSize()/frag.getFlowSendingPeriodicity()));
			frag.getParent().setNumberOfPacketsSent(frag.getParent().getPathTree().getRoot());

		}		

		port.getCycle().setCycleDuration(previousPortCycleSize);
		port.setCycleUpperBoundRange((int) (currentPortCycleSize/previousPortCycleSize));

	}
	
	public void carryOnPortCycle(Port port) {
		
	}
	
	public void carryOnPortFragmentTimes(Port port) {
		
	}
	
	
	public void resetPort(Port port) {
		int numOfPackets = 0;
		
		this.resetPortCycleProperties(port);
		
		port.setPortSpeedZ3(ctx.mkReal(Double.toString(port.getPortSpeed())));
				
		for(FlowFragment frag : port.getFlowFragments()) {
			
			if(frag.getModificationType() != NetworkProperties.INCREMENTFLOW  && frag.getIsModifiedOrCreated() == false) {
				continue;				
			}
			
			frag.createNewDepartureTimeZ3List();
			
			frag.setFragmentPriorityZ3(
    				ctx.mkIntConst(frag.getName() + "Priority")
			);
    		
    		if(frag.getPacketPeriodicityZ3() == null) {
    			frag.setPacketPeriodicityZ3(ctx.mkReal(Double.toString(frag.getParent().getFlowSendingPeriodicity())));
    		}
    		
    		
    		if(frag.getPacketSizeZ3() == null) {
    			frag.setPacketSizeZ3(
					ctx.mkReal(
						Double.toString(
							frag.getParent().getPacketSize()
						)
					)
				);
    		}
    		    		
    		// Linking fragments to previous and next fragments
    		
    		for(int i = 0; i < frag.getNumOfPacketsSent(); i++) {
    			
    			if(i == 0 && frag.getPreviousFragment() == null) {
    				frag.addDepartureTimeZ3(
						frag.getParent().getFlowFirstSendingTimeZ3()
					);
    				
    			} else if (i > 0 && frag.getPreviousFragment() == null) {
    				
    			 	frag.addDepartureTimeZ3( (RealExpr)
						ctx.mkAdd(
							 frag.getDepartureTimeZ3(i - 1)
							,frag.getPacketPeriodicityZ3()
						)	
					);
    			 	
    			} else {
    				
    				frag.addDepartureTimeZ3(
						frag.getPreviousFragment().getPort().scheduledTime(ctx, i, frag.getPreviousFragment())
					);
    				
    			}
    			
    			
    			// KEEPING THE VALUES FOR THE FRAGMENTS ON THE PORT
    			//if (i < frag.getDepartureTimeList().size()) {
    			//	frag.addDepartureTimeZ3(ctx.mkReal(Double.toString(frag.getDepartureTime(i))));    				
    			//} 
    			
    			
    				
				/*
    			else if (frag.getDepartureTimeList().size() > 0 && i < frag.getDepartureTimeList().size()) {
    				frag.addDepartureTimeZ3(ctx.mkReal(Double.toString(frag.getDepartureTime(i))));    				
    			} else if (i >= frag.getDepartureTimeList().size() && frag.getPreviousFragment() != null) {
    				frag.addDepartureTimeZ3(
						frag.getPreviousFragment().getPort().scheduledTime(ctx, i, frag.getPreviousFragment())
					);
    			} 
    			*/
    			
    			
    			if(frag.getPreviousFragment() != null) {		
	    			solver.add(                                                          
						ctx.mkEq(
							port.departureTime(ctx, i, frag),
							frag.getPreviousFragment().getPort()
								.scheduledTime(ctx, i, frag.getPreviousFragment())
						)
					);
	    			
	    			solver.add(
							ctx.mkEq(
								port.departureTime(ctx, i, frag),
								frag.getPreviousFragment()
									.getPort()
										.scheduledTime(ctx, i, frag.getPreviousFragment())
							)
						);
	    			
    			}
    			
    			/*
    			
    			if(!frag.getNextFragments().isEmpty()) {
    				for(FlowFragment auxFrag : frag.getNextFragments()) {
    	    			System.out.println(auxFrag.getName());
    	    			System.out.println(auxFrag.getFlowFirstSendingTime());
    	    			System.out.println(auxFrag.getPort()
								.departureTime(ctx, i, auxFrag));
	    				solver.add(
							ctx.mkEq(
								port.scheduledTime(ctx, i, frag),
								auxFrag.getPort()
									.departureTime(ctx, i, auxFrag)
							)
						);
    				}
    			}
    			/**/
    		}
    		
		}
		
		//port.setupSchedulingRules(solver, ctx);
		
	}
	
	public void resetPortsByFragmentBackwards(FlowFragment frag, int depth) {
		PathNode node = frag.getReferenceToNode();
		Port port = ((TSNSwitch) node.getNode()).getPortOf(frag.getNextHop());
		this.resetPort(port);
		
		if(depth <= 0 || (frag.getPreviousFragment() == null)) {
			return;
		}
		
		this.resetPortsByFragmentBackwards(frag.getPreviousFragment(), depth - 1);
		
	}
	
	
	public void resetPortsByFragmentForward(FlowFragment frag, int depth) {
		PathNode node = frag.getReferenceToNode();
		Port port = ((TSNSwitch) node.getNode()).getPortOf(frag.getNextHop());
		this.resetPort(port);
		
		if(depth <= 0 || frag.getNextFragments().isEmpty()) {
			return;
		}
		
		for(FlowFragment auxFrag : frag.getNextFragments()) {
			this.resetPortsByFragmentForward(auxFrag, depth - 1);			
		}
		
	}
	
	
	public void resetPortsStartingFromNode(PathNode node, int depth) {
		
		// Hit a leaf
		if(node.getNode() instanceof Device && node.getChildren().isEmpty()) 
			return;
		
		// No more hops allowed
		if(depth == 0) 
			return;
		
		for(PathNode child : node.getChildren()) {
			
			if(child.getNode() instanceof Device) {
				continue;
			}
			
			for(FlowFragment frag : child.getFlowFragments()) {
				Port port = ((TSNSwitch) child.getNode()).getPortOf(frag.getNextHop());
				this.resetPort(port);
			}
			
			this.resetPortsStartingFromNode(child, depth - 1);
			
		}
		
	}
	
	public void createNewObjects() {
		
		for(Object element : this.createdObjects) {
			
			// Basic flow setup
			if(element instanceof Flow) {
				((Flow) element).modifyIfUsingCustomVal();
				// Check the method bellow. Fragments are sending 0 packets
				((Flow) element).setUpPeriods(((Flow) element).getPathTree().getRoot());
			
				this.pendingToZ3.add(element);
				
				PathNode node = ((Flow) element).getPathTree().getRoot();
				
				if(((Flow) element).getModificationType() == NetworkProperties.INCREMENTFLOW) {
					for(PathNode child : node.getChildren()) {
						this.addPortsWithNoFragmentToModifyList(child);
					}
				} else {
					for(PathNode child : node.getChildren()) {
						this.addPortsToModifyList(child);
					}
				}
			
			}		
			
		}
		
	}
	
	private void addPortsWithNoFragmentToModifyList(PathNode node) {
		
		if(node.getNode() instanceof Device || node.getChildren().isEmpty()) {
			return;
		}
		

		for(PathNode auxNode : node.getChildren()) {
			Port port = ((TSNSwitch) node.getNode()).getPortOf(
					auxNode.getNode()
					instanceof Device ?
					((Device) auxNode.getNode()).getName() :
					((TSNSwitch) auxNode.getNode()).getName()
				);
			
			if(!this.modifiedObjects.contains(port) && port.getFlowFragments().size() == 0) {
				//System.out.println("Found port with no fragment which is part of rescheduling: " + port.getName());
				//this.pendingSetup.add(port);
				this.modifiedObjects.add(port);
				
				for(FlowFragment frag : port.getFlowFragments()) {
					frag.setIsModifiedOrCreated(true);
				}
				
				port.setIsModifiedOrCreated(true);
				
			} else if (!this.modifiedObjects.contains(port) && !this.pendingSetup.contains(port)){
				this.pendingSetup.add(port);
				port.setModificationType(NetworkProperties.INCREMENTFLOW);
			}
			
			this.addPortsWithNoFragmentToModifyList(auxNode);
		}
		
		
		
	}
	
		
	
	private void addPortsToModifyList(PathNode node) {
		
		if(node.getNode() instanceof Device || node.getChildren().isEmpty()) {
			/*
			System.out.println("Leaving on");
			System.out.println(
				(node.getNode()
				instanceof Device ?
				((Device) node.getNode()).getName() :
				((TSNSwitch) node.getNode()).getName())
			);
			*/
			return;
		}
		
		for(PathNode auxNode : node.getChildren()) {
			
			if(!this.modifiedObjects.contains(
					((TSNSwitch) node.getNode()).getPortOf(
						auxNode.getNode()
						instanceof Device ?
						((Device) auxNode.getNode()).getName() :
						((TSNSwitch) auxNode.getNode()).getName()
					)
				)) 
			{	
				this.modifiedObjects.add(((TSNSwitch) node.getNode()).getPortOf(
					auxNode.getNode()
					instanceof Device ?
					((Device) auxNode.getNode()).getName() :
					((TSNSwitch) auxNode.getNode()).getName()
				));
				
				((TSNSwitch) node.getNode()).getPortOf(
					auxNode.getNode()
					instanceof Device ?
					((Device) auxNode.getNode()).getName() :
					((TSNSwitch) auxNode.getNode()).getName()
				).setIsModifiedOrCreated(true);
				
				for(FlowFragment frag : ((TSNSwitch) node.getNode()).getPortOf(
					auxNode.getNode()
					instanceof Device ?
					((Device) auxNode.getNode()).getName() :
					((TSNSwitch) auxNode.getNode()).getName()
				).getFlowFragments()) {
					frag.setIsModifiedOrCreated(true);
				}
				
				/*
				System.out.println("Adding: " +
					((TSNSwitch) node.getNode()).getPortOf(
						auxNode.getNode()
						instanceof Device ?
						((Device) auxNode.getNode()).getName() :
						((TSNSwitch) auxNode.getNode()).getName()).getName()
					+ " that leads to " +
					(auxNode.getNode()
					instanceof Device ?
					((Device) auxNode.getNode()).getName() :
					((TSNSwitch) auxNode.getNode()).getName())
				);
				/**/				
			
				this.addPortsToModifyList(auxNode);

			}

		}
		
	}
	
	
	public void resetFlow(Flow flow) {
		
		
		
	}
	
	
	private void toZ3PendingObjects() {
		
		
		
		for(Object element : this.pendingToZ3) {
			if(element instanceof Flow) {
				((Flow) element).toZ3(ctx);				
			}
		}		
		
	}
	
	public void setUpCycleInModifiedPort(Port port, ResetMethod ... methodArgs) {
		
		ResetMethod method = ResetMethod.PORTHARDRESET;
		int numOfPackets = 0;
		
		
		if(methodArgs.length > 0) {
			method = methodArgs[0]; 			
		}
		
		port.toZ3(this.ctx);
		//System.out.println(port.getName() + ": on its way to reset");
		switch(method) {
			case PORTHARDRESET:
				port.setUpCycle();
				break;
			case PORTCARRYONCYCLE:
				this.carryOnPortCycle(port);
				break;
			case PORTCARRYONFRAGMENTS:
				this.carryOnPortFragmentTimes(port);
				break;
			case PORTSOFTRESET:
				// Keeps the values that were already loaded.
				// This was already done in the loadNetwork.
				break;
			default:
				break;
		}

	}
	
	
	public void applyChangesToSolver() {

		// this.createNewObjects();

		// If there is an port object on the modified objects list, must recalculate
		// number of packets in each fragment and set up the cycle again.
		for(Object element : this.modifiedObjects) {
			if(element instanceof Port) {
				this.setUpCycleInModifiedPort((Port) element);
			}			
		}
		
		ArrayList<Double> listOfPreviousCycleSize = new ArrayList<Double>();
		for(Object element : this.pendingSetup) {
			if(element instanceof Port) {
				listOfPreviousCycleSize.add(((Port) element).getDefinedHyperCycleSize());
				//System.out.println(((Port) element).getName() + " - " + ((Port) element).getCycle().getCycleDuration() + "; " + ((Port) element).getDefinedHyperCycleSize());
				((Port) element).setUpCycle();
				//System.out.println(((Port) element).getIsModifiedOrCreated());
				//System.out.println(((Port) element).getName() + " - " + ((Port) element).getCycle().getCycleDuration());
			}			
		}
		
		this.toZ3PendingObjects();

		this.linkPossibleExtraFragments(listOfPreviousCycleSize);


		for(Object element : this.modifiedObjects) {
			if(element instanceof Port) {
				
				this.resetPort((Port) element); 
				
			} else if (element instanceof Flow) {
				this.resetFlow((Flow) element);
			}
			
		}
		
	}
	
	private  void linkPossibleExtraFragments(ArrayList<Double> listOfPreviousCycleSize) {

		for(Object element : this.pendingSetup) {
			if(element instanceof Port) {
				Double currentPortCycleSize = ((Port) element).getDefinedHyperCycleSize();

				int numOfPacketsScheduled = 0;
				for(FlowFragment frag : ((Port) element).getFlowFragments()) {
					numOfPacketsScheduled += (int) (((Port) element).getDefinedHyperCycleSize()/frag.getFlowSendingPeriodicity());
				}

				((Port) element).getCycle().setNumOfSlots(numOfPacketsScheduled);

		    	 if(listOfPreviousCycleSize.get(this.pendingSetup.indexOf(element)) != currentPortCycleSize) {
		  	    	((Port) element).setDefinedHyperCycleSize(listOfPreviousCycleSize.get(this.pendingSetup.indexOf(element)) );
		  	    	((Port) element).getCycle().setCycleDuration(listOfPreviousCycleSize.get(this.pendingSetup.indexOf(element)) );
		  	    	((Port) element).setCycleUpperBoundRange((int)(currentPortCycleSize/listOfPreviousCycleSize.get(this.pendingSetup.indexOf(element)) ));
		  	    }

		    	 
		    	for(FlowFragment frag : ((Port) element).getFlowFragments()) {
		    		solver.add(
							ctx.mkLe(
									((Port) element).arrivalTime(ctx, 0, frag),
									ctx.mkSub(
										ctx.mkAdd(
												(RealExpr) ((Port) element).getCycle().getCycleDurationZ3(),
												((Port) element).getCycle().getFirstCycleStartZ3()
										),
										ctx.mkDiv( frag.getPacketSizeZ3() , ((Port) element).getPortSpeedZ3() )
								)
					       )
					);

		    		if(currentPortCycleSize/frag.getFlowSendingPeriodicity() != frag.getNumOfPacketsSent()) {

						int previousNumOfPacketSent = frag.getNumOfPacketsSent();
		    			frag.setNumOfPacketsSent( (int) (currentPortCycleSize/frag.getFlowSendingPeriodicity()) );


		    			for(int i = 0; i < frag.getNumOfPacketsSent(); i++) {
		    				if(((int) (i/previousNumOfPacketSent)) > 0) {

	    						frag.addDepartureTimeZ3( (RealExpr)
										ctx.mkAdd(
												((Port) element).departureTime(ctx, ((i)%previousNumOfPacketSent), frag),
												ctx.mkMul(
														ctx.mkReal(Double.toString(previousNumOfPacketSent * frag.getFlowSendingPeriodicity()) ),
														ctx.mkReal(((int) ((i)/previousNumOfPacketSent)))
												)
										)
								);
	    						solver.add(
	    								ctx.mkEq(
	    										((Port) element).arrivalTime(ctx, i, frag),
	    										ctx.mkAdd(
	    												((Port) element).arrivalTime(ctx, ((i)%previousNumOfPacketSent), frag),
	    												ctx.mkMul(
																ctx.mkReal(Double.toString(previousNumOfPacketSent * frag.getFlowSendingPeriodicity()) ),
	    														ctx.mkInt(((int) ((i)/previousNumOfPacketSent)))
	    												)
	    										)	
										)
								);
	    						
	    						solver.add(
	    								ctx.mkEq(
	    										((Port) element).scheduledTime(ctx, i, frag),
	    										ctx.mkAdd(
	    												((Port) element).scheduledTime(ctx, ((i)%previousNumOfPacketSent), frag),
	    												ctx.mkMul(
																ctx.mkReal(Double.toString(previousNumOfPacketSent * frag.getFlowSendingPeriodicity()) ),
	    														ctx.mkInt(((int) ((i)/previousNumOfPacketSent)))
	    												)
	    										)	
										)
								);
		    				}
		    			}
		    			
		    		}
		    	}

			}
		}
	}
	
	public Context getCtx() {
		return ctx;
	}

	public void setCtx(Context ctx) {
		this.ctx = ctx;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setSolver(Solver solver) {
		this.solver = solver;
	}

	
}
