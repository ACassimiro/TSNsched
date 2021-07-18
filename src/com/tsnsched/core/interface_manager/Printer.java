//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    Copyright (C) 2021  Aellison Cassimiro
//    
//    TSNsched is licensed under the GNU GPL version 3 or later:
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.tsnsched.core.interface_manager;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.FlowFragment;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.components.PathTree;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.network.*;
import com.tsnsched.core.nodes.*;

public class Printer {

	private Boolean enableConsoleOutput;
	private Boolean enableLoggerFile;
	

	public Printer () {
		
	}
	
	/**
    * [Method]: stringToDouble
    * [Usage]: After evaluating the model, z3 allows the
    * user to retrieve the values of variables. This 
    * values are stored as strings, which are converted 
    * by this function in order to be stored in the 
    * classes variables. Often these z3 variables are 
    * also on fraction form, which is also handled by 
    * this function.
    * 
    * @param str   String containing value to convert to double
    * @return      Double value of the given string str
    */
   public double stringToDouble(String str) {
       BigDecimal val1;
       BigDecimal val2;
       double result = 0;
       
       if(str.contains("/")) {
           val1 = new BigDecimal(str.split("/")[0]);
           val2 = new BigDecimal(str.split("/")[1]);
           result = val1.divide(val2, MathContext.DECIMAL128).doubleValue();

       } else {
    	   try{
    		    result = Double.parseDouble(str);
    	    }catch(NumberFormatException e){
    	        result = -1;
    	    }
       }
       
       return result;
   }
	
	/**
    * [Method]: writePathTree
    * [Usage]: This is a recursive function used to 
    * navigate through the pathTree, storing information
    * about the switches and flowFramengts in the nodes
    * and printing data in the log.
    * 
    * @param pathNode  Current node of pathTree (should start with root)
    * @param model     Output model generated by z3
    * @param ctx       z3 context used to generate the model
    * @param out       PrintWriter stream to output log file
    */
   public void writePathTree(PathNode pathNode, Model model, Context ctx, PrintWriter out) {
       Switch swt;
       IntExpr indexZ3 = null;
    
       if((pathNode.getNode() instanceof Device) && (pathNode.getParent() != null)) {
           this.logIfLoggingIsEnabled(out , "    [END OF BRANCH]");
       }
       
       /*
        * Once given a node, an iteration through its children will begin. For
        * each switch children, there will be a flow fragment, and to each device
        * children, there will be an end of branch.
        * 
        * The logic for storing and printing the data on the publish subscribe
        * flows is similar but easier than the unicast flows. The pathNode object
        * stores references to both flow fragment and switch, so no search is needed.
        */
    for(PathNode child : pathNode.getChildren()) {
        if(child.getNode() instanceof Switch) {
            
            for(FlowFragment ffrag : child.getFlowFragments()) {
            	
            	/*
            	this.printIfLoggingIsEnabled("Fragment " + ffrag.getName() + " : " + 
            						((TSNSwitch) child.getNode()).getPortOf(ffrag.getNextHop()).getCycle().getName()
    			);
            	*/
            	int prt = Integer.parseInt(model.eval(ffrag.getFragmentPriorityZ3(), false).toString());
            	
                this.logIfLoggingIsEnabled(out ,"    Fragment name: " + ffrag.getName());
                this.logIfLoggingIsEnabled(out , "        Fragment node: " + ffrag.getNodeName());
                this.logIfLoggingIsEnabled(out, "        Fragment next hop: " + ffrag.getNextHop());
                this.logIfLoggingIsEnabled(out, "        Fragment priority: " + model.eval(ffrag.getFragmentPriorityZ3(), false));
                for(int index = 0; index < ((TSNSwitch) child.getNode()).getPortOf(ffrag.getNextHop()).getCycle().getNumOfSlots(prt); index++) {
             	   indexZ3 = ctx.mkInt(index);
             	   this.logIfLoggingIsEnabled(out, "        Fragment slot start " + index + ": " 
                            + this.stringToDouble(
                                    model.eval(((TSNSwitch) child.getNode())
                                           .getPortOf(ffrag.getNextHop())
                                           .getCycle()
                                           .slotStartZ3(ctx, ffrag.getFragmentPriorityZ3(), indexZ3) 
                                           , false).toString()
                                )
                           + " ; " + model.eval(((TSNSwitch) child.getNode())
                                   .getPortOf(ffrag.getNextHop())
                                   .getCycle()
                                   .slotStartZ3(ctx, ffrag.getFragmentPriorityZ3(), indexZ3) 
                                   , false).toString()
             			   );
             	   this.logIfLoggingIsEnabled(out, "        Fragment slot duration " + index + " : " 
                             + this.stringToDouble(
                                 model.eval(((TSNSwitch) child.getNode())
                                            .getPortOf(ffrag.getNextHop())
                                            .getCycle()
                                            .slotDurationZ3(ctx, ffrag.getFragmentPriorityZ3(), indexZ3) 
                                            , false).toString())
                             + " ; " + model.eval(((TSNSwitch) child.getNode())
                                     .getPortOf(ffrag.getNextHop())
                                     .getCycle()
                                     .slotDurationZ3(ctx, ffrag.getFragmentPriorityZ3(), indexZ3) 
                                     , false).toString()
             			   );
         
                }
                
                this.logIfLoggingIsEnabled(out, "        Fragment times-");
                ffrag.getParent().addToTotalNumOfPackets(ffrag.getNumOfPacketsSent());
                
                for(int i = 0; i < ffrag.getParent().getNumOfPacketsSent(); i++) {
             	   if(i < ffrag.getNumOfPacketsSent()) {
	                	   this.logIfLoggingIsEnabled(out, "          (" + Integer.toString(i) + ") Fragment departure time: " + this.stringToDouble(model.eval(((TSNSwitch) child.getNode()).departureTime(ctx, i, ffrag) , false).toString()));
	                	   this.logIfLoggingIsEnabled(out, "          (" + Integer.toString(i) + ") Fragment arrival time: " + this.stringToDouble(model.eval(((TSNSwitch) child.getNode()).arrivalTime(ctx, i, ffrag) , false).toString()));
	                       this.logIfLoggingIsEnabled(out, "          (" + Integer.toString(i) + ") Fragment scheduled time: " + this.stringToDouble(model.eval(((TSNSwitch) child.getNode()).scheduledTime(ctx, i, ffrag) , false).toString()));
	                       this.logIfLoggingIsEnabled(out, "          ----------------------------");
             	   }
                    
             	   ffrag.setFragmentPriority(
         			   Integer.parseInt(
             			   model.eval(ffrag.getFragmentPriorityZ3(), false).toString()
 					   )
     			   );
             	   
                    ffrag.addDepartureTime(
                        this.stringToDouble(
                            model.eval(((TSNSwitch) child.getNode()).departureTime(ctx, i, ffrag) , false).toString()   
                        )
                    );
                    ffrag.addArrivalTime(
                        this.stringToDouble(
                            model.eval(((TSNSwitch) child.getNode()).arrivalTime(ctx, i, ffrag) , false).toString()   
                        )
                    );
                    ffrag.addScheduledTime(
                        this.stringToDouble(
                            model.eval(((TSNSwitch) child.getNode()).scheduledTime(ctx, i, ffrag) , false).toString()   
                        )
                    );
                    
                    /*
                    this.printIfLoggingIsEnabled(
                		((TSNSwitch) child.getNode()).scheduledTime(ctx, i, ffrag) 
                		+
                		" : "
                		+
                		this.stringToDouble(
                            model.eval(((TSNSwitch) child.getNode()).scheduledTime(ctx, i, ffrag) , false).toString()   
                        )	
            		);
                    */
                }
                
                swt = (TSNSwitch) child.getNode();

                for (Port port : ((TSNSwitch) swt).getPorts()) {

                    if(!port.getFlowFragments().contains(ffrag)) {
                        continue;
                    }

                    ArrayList<Double> listOfStart = new ArrayList<Double>();
             	    ArrayList<Double> listOfDuration = new ArrayList<Double>();
             	
                    
                    for(int index = 0; index < ((TSNSwitch) child.getNode()).getPortOf(ffrag.getNextHop()).getCycle().getNumOfSlots(prt); index++) {
                 	   indexZ3 = ctx.mkInt(index);
                 	   
             		   listOfStart.add(
         				   this.stringToDouble(model.eval( 
                                ((TSNSwitch) child.getNode())
                                .getPortOf(ffrag.getNextHop())
                                .getCycle().slotStartZ3(ctx, ffrag.getFragmentPriorityZ3(), indexZ3) , false).toString())
     				   );
             		   listOfDuration.add(
         				   this.stringToDouble(model.eval( 
                                ((TSNSwitch) child.getNode())
                                .getPortOf(ffrag.getNextHop())
                                .getCycle().slotDurationZ3(ctx, ffrag.getFragmentPriorityZ3(), indexZ3) , false).toString())
     				   );
                    }
             	   
             	   port.getCycle().addSlotUsed(
                        (int) this.stringToDouble(model.eval(ffrag.getFragmentPriorityZ3(), false).toString()), 
                        listOfStart, 
                        listOfDuration
                    );
                }
                
            }
            
            this.writePathTree(child, model, ctx, out);
         } 
      }
   }
	
   
   public void generateLog(String logName, Network net, Context ctx, Model model) {
	   this.printIfLoggingIsEnabled("- Model generated successfully.");
       
       try {
           PrintWriter out = null; 
           
           if(this.enableLoggerFile) {        	   
        	   out = new PrintWriter("log.txt");
           }
           
           
           this.logIfLoggingIsEnabled(out, "SCHEDULER LOG:\n\n");
           	                   
           this.logIfLoggingIsEnabled(out, "SWITCH LIST:");
           
           // For every switch in the network, store its information in the log
           for(Switch auxSwt : net.getSwitches()) {
               this.logIfLoggingIsEnabled(out, "  Switch name: " + auxSwt.getName());
               this.logIfLoggingIsEnabled(out, "    Max packet size: " + auxSwt.getMaxPacketSize());
               this.logIfLoggingIsEnabled(out, "    Port speed: " + auxSwt.getPortSpeed());
               this.logIfLoggingIsEnabled(out, "    Time to Travel: " + auxSwt.getTimeToTravel());
               this.logIfLoggingIsEnabled(out, "    Transmission time: " + auxSwt.getTransmissionTime());
               // this.logIfLoggingIsEnabled(out, "    Cycle information -");
               // this.logIfLoggingIsEnabled(out, "        First cycle start: " + model.eval(((TSNSwitch)auxSwt).getCycleStart(), false));
               // this.logIfLoggingIsEnabled(out, "        Cycle duration: " + model.eval(((TSNSwitch)auxSwt).getCycleDuration(), false));
               this.logIfLoggingIsEnabled(out, "");
               /*
               for (Port port : ((TSNSwitch)auxSwt).getPorts()) {
                   this.logIfLoggingIsEnabled(out, "        Port name (Virtual Index): " + port.getName());
                   this.logIfLoggingIsEnabled(out, "        First cycle start: " + model.eval(port.getCycle().getFirstCycleStartZ3(), false));
                   this.logIfLoggingIsEnabled(out, "        Cycle duration: " + model.eval(port.getCycle().getCycleDurationZ3(), false));
                   this.logIfLoggingIsEnabled(out, ""); 
               }
               */
               
               
               // [EXTRACTING OUTPUT]: Obtaining the z3 output of the switch properties,
               // converting it from string to double and storing in the objects
                                        
               for (Port port : ((TSNSwitch)auxSwt).getPorts()) {
            	   if(port.getFlowFragments().isEmpty()) {
            		   continue;            		   
            	   }
            	   
                   port
                       .getCycle()
                       .setCycleStart(
                           this.stringToDouble("" + model.eval(port.getCycle().getFirstCycleStartZ3(), false))
                       );
               
                   // cycleDuration
                   port
                       .getCycle()
                       .setCycleDuration(
                           this.stringToDouble("" + model.eval(port.getCycle().getCycleDurationZ3(), false))
                       );
               }
               
           }
           
           this.logIfLoggingIsEnabled(out, "");

           this.logIfLoggingIsEnabled(out, "FLOW LIST:");
           //For every flow in the network, store its information in the log
           for(Flow f : net.getFlows()) {
        	   /*
        	   this.printIfLoggingIsEnabled(f.getName());
        	   if(f.getName().equals("flow1")) {
        		   this.printIfLoggingIsEnabled(
    				   model.eval(
						   f.getPathTree().getRoot().getChildren().get(0).getFlowFragments().get(0).getPort().scheduledTime(ctx,
								   0, f.getPathTree().getRoot().getChildren().get(0).getFlowFragments().get(0))
    						   , false)
				   );
        		   
        	   }
        	   */
        	   
               this.logIfLoggingIsEnabled(out, "  Flow name: " + f.getName());
               //this.logIfLoggingIsEnabled(out, "    Flow priority:" + model.eval(f.getFlowPriority(), false));
               //this.logIfLoggingIsEnabled(out, "    Flow latency:" + model.eval(f.getFlowPriority(), false));
               //this.logIfLoggingIsEnabled(out, "    Flow latency:" + model.eval(f.getJitterZ3(), false));
               this.logIfLoggingIsEnabled(out, "    Start first t1: " + model.eval(f.getFlowFirstSendingTimeZ3(), false));
               f.setFlowFirstSendingTime(this.stringToDouble(model.eval(f.getFlowFirstSendingTimeZ3(), false).toString()));
               this.logIfLoggingIsEnabled(out, "    Start HC: " + f.getFlowMaximumLatency());
               this.logIfLoggingIsEnabled(out, "    Start packet periodicity: " + model.eval(f.getFlowSendingPeriodicityZ3(), false));
               
               
               // IF FLOW IS UNICAST
               /*
               Observation: The flow is broken in smaller flow fragments.
               In order to know the departure, arrival, scheduled times
               and other properties of the flow the switch that the flow fragment
               belongs to must be retrieved. The flow is then used on the switch 
               to find the port to its destination. The port and the flow fragment
               can now be used to retrieve information about the flow.
               
               The way in which unicast and publish subscribe flows are 
               structured here are different. So this process is done differently
               in each case.
               */
               if(f.getType() == Flow.UNICAST) {
                   // TODO: Throw error. UNICAST data structure are not allowed at this point
            	   // Everything should had been converted into the multicast model.
               } else if(f.getType() == Flow.PUBLISH_SUBSCRIBE) { //IF FLOW IS PUB-SUB
                   
                   /*
                    * In case of a publish subscribe flow, it is easier to 
                    * traverse through the path three than iterate over the 
                    * nodes as it could be done with the unicast flow.
                    */
                   
                   PathTree pathTree;
                   PathNode pathNode;
                   
                   pathTree = f.getPathTree();
                   pathNode = pathTree.getRoot();
                   
                   this.logIfLoggingIsEnabled(out, "    Flow type: Multicast");
                   ArrayList<PathNode> auxNodes;
                   ArrayList<FlowFragment> auxFlowFragments;
                   int auxCount = 0;
                   
                   this.logInLineIfLoggingIsEnabled(out, "    List of leaves: ");
                   for(PathNode node : f.getPathTree().getLeaves()) {
                       this.logInLineIfLoggingIsEnabled(out, ((Device) node.getNode()).getName() + ", ");                                           
                   }
                   this.logIfLoggingIsEnabled(out, "");
                   for(PathNode node : f.getPathTree().getLeaves()) {
                       auxNodes = f.getNodesFromRootToNode((Device) node.getNode());
                       auxFlowFragments = f.getFlowFromRootToNode((Device) node.getNode());
                       
                       this.logInLineIfLoggingIsEnabled(out, "    Path to " + ((Device) node.getNode()).getName() + ": ");
                       auxCount = 0;
                       for(PathNode auxNode : auxNodes) {
                           if(auxNode.getNode() instanceof Device) {
                               this.logInLineIfLoggingIsEnabled(out, ((Device) auxNode.getNode()).getName() + ", ");                                           
                           } else if (auxNode.getNode() instanceof TSNSwitch) {
                               this.logInLineIfLoggingIsEnabled(out, 
                                   ((TSNSwitch) auxNode.getNode()).getName() + 
                                   "(" + 
                                   auxFlowFragments.get(auxCount).getName() +
                                   "), ");
                               auxCount++;
                           }
                           
                       }
                       this.logIfLoggingIsEnabled(out, "");
                   }
                   this.logIfLoggingIsEnabled(out, "");
                   
                   //Start the data storing and log printing process from the root
                   this.writePathTree(pathNode, model, ctx, out);                                
               }
               
               this.logIfLoggingIsEnabled(out, "");
               
           }

           if(this.enableLoggerFile) {
        	   out.close();        	   
           }
           
       } catch (FileNotFoundException e) {
           e.printStackTrace();
       }
   }
	
   
   
   public void printDataOnTree(PathNode pathNode, Model model, Context ctx) {
       Switch swt;
       IntExpr indexZ3 = null;
    
       if((pathNode.getNode() instanceof Device) && (pathNode.getParent() != null)) {
    	   this.printIfLoggingIsEnabled("    [END OF BRANCH]");
       }
       
       /*
        * Once given a node, an iteration through its children will begin. For
        * each switch children, there will be a flow fragment, and to each device
        * children, there will be an end of branch.
        * 
        * The logic for storing and printing the data on the publish subscribe
        * flows is similar but easier than the unicast flows. The pathNode object
        * stores references to both flow fragment and switch, so no search is needed.
        */
    for(PathNode child : pathNode.getChildren()) {
        if(child.getNode() instanceof Switch) {
            
            for(FlowFragment ffrag : child.getFlowFragments()) {
            	
            	this.printIfLoggingIsEnabled("Fragment " + ffrag.getName() + " : " + 
            						((TSNSwitch) child.getNode()).getPortOf(ffrag.getNextHop()).getCycle().getName()
    			);
                         
                for(int i = 0; i < ffrag.getParent().getNumOfPacketsSent(); i++) {
             	   ;
                    
                    /*
                    this.printIfLoggingIsEnabled(
                		((TSNSwitch) child.getNode()).scheduledTime(ctx, i, ffrag) 
                		+
                		" : "
                		+
                		this.stringToDouble(
                            model.eval(((TSNSwitch) child.getNode()).scheduledTime(ctx, i, ffrag) , false).toString()   
                        )	
            		);
                    */
                }
                
            }
            
            this.printDataOnTree(child, model, ctx);
         } 
      }
   }
   
   
   
   public void exportModel(Solver solver) {
	   
	   try {
		PrintWriter out = new PrintWriter("model.txt");
		for(BoolExpr exp : solver.getAssertions()) {
			out.println(exp);	    		   
		}
				
		out.close();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	}
           
	   
   }
   
   public void printOnConsole(Network net) {

		int numOfFramesScheduled = 0;
		double overallAverageJitter = 0;
		double overallAverageLatency = 0;
		this.printIfLoggingIsEnabled("");
		this.printIfLoggingIsEnabled("");
		int auxCount = 0;
		ArrayList<PathNode> auxNodes;
		ArrayList<FlowFragment> auxFlowFragments;

		Cycle auxCycle;
		TSNSwitch auxSwt;
		int flagContinueLoop = 1;


		for(Switch swt : net.getSwitches()) {
			flagContinueLoop = 1;
			auxSwt = (TSNSwitch) swt;
			for(Port port : auxSwt.getPorts()) {
				if(port.getCycle().getSlotsUsed().size() != 0) {
					flagContinueLoop = 0;
					break;
				}
			}

			if(flagContinueLoop == 1) {
				continue;
			}
			this.printIfLoggingIsEnabled("\n\n>>>> INFORMATION OF SWITCH: " + auxSwt.getName() + " <<<<");
			this.printIfLoggingIsEnabled("    Port list - ");
			for(Port port : auxSwt.getPorts()) {
				if(port.getCycle().getSlotsUsed().size() == 0) {
					continue;
				}
				this.printIfLoggingIsEnabled("        => Port name:       " + port.getName());
				this.printIfLoggingIsEnabled("        Connects to:     " + port.getConnectsTo());

				this.printIfLoggingIsEnabled("        Cycle start:    " + port.getCycle().getCycleStart());
				this.printIfLoggingIsEnabled("        Cycle duration: " + port.getCycle().getCycleDuration());
				this.printInLineIfLoggingIsEnabled("        Fragments:       ");
				for(FlowFragment ffrag : port.getFlowFragments()) {
					this.printInLineIfLoggingIsEnabled(ffrag.getName() + ", ");
				}
				this.printIfLoggingIsEnabled("");

				auxCycle = port.getCycle();
				//this.printIfLoggingIsEnabled("        Slots per prt:   " +  auxCycle.getNumOfSlots());
				for(int i = 0; i < auxCycle.getSlotsUsed().size(); i++) {
					this.printIfLoggingIsEnabled("        Priority number: " + auxCycle.getSlotsUsed().get(i));
					for(int j = 0; j < auxCycle.getNumOfSlots(auxCycle.getSlotsUsed().get(i)); j++) {						this.printIfLoggingIsEnabled("          Index " + j + " Slot start:      " + auxCycle.getSlotStart(auxCycle.getSlotsUsed().get(i), j));
						this.printIfLoggingIsEnabled("          Index " + j + " Slot duration:   " + auxCycle.getSlotDuration(auxCycle.getSlotsUsed().get(i), j));
					}					this.printIfLoggingIsEnabled("        ------------------------");
				}
			}
		}

		this.printIfLoggingIsEnabled("");

		double sumOfAvgLatencies = 0;
		double sumOfLatencies;
		int flowCounter = 0;
		for(Flow flw : net.getFlows()){


			this.printIfLoggingIsEnabled("\n\n>>>> INFORMATION OF FLOW" + flowCounter++ + " <<<<\n");

			this.printIfLoggingIsEnabled("    Total number of packets scheduled: " + flw.getTotalNumOfPackets());
			numOfFramesScheduled = numOfFramesScheduled + flw.getTotalNumOfPackets();
			this.printIfLoggingIsEnabled("    Path tree of the flow:");
			for(PathNode node : flw.getPathTree().getLeaves()) {
				auxNodes = flw.getNodesFromRootToNode((Device) node.getNode());
				auxFlowFragments = flw.getFlowFromRootToNode((Device) node.getNode());
				this.printInLineIfLoggingIsEnabled("        Path to " + ((Device) node.getNode()).getName() + ": ");
				auxCount = 0;
				for(PathNode auxNode : auxNodes) {
					if(auxNode.getNode() instanceof Device) {
						this.printInLineIfLoggingIsEnabled(((Device) auxNode.getNode()).getName() + ", ");
					} else if (auxNode.getNode() instanceof TSNSwitch) {
						this.printInLineIfLoggingIsEnabled(((TSNSwitch) auxNode.getNode()).getName() +	"(" + auxFlowFragments.get(auxCount).getName() + "), ");
						auxCount++;
					}
				}
				this.printInLineIfLoggingIsEnabled("\n");
			}


			this.printIfLoggingIsEnabled("");
			this.printIfLoggingIsEnabled("");
			for(PathNode node : flw.getPathTree().getLeaves()) {
				Device dev = (Device) node.getNode();

				sumOfLatencies = 0;
				this.printIfLoggingIsEnabled("    Packets heading to " + dev.getName() + ":");

				for(int i = 0; i < flw.getNumOfPacketsSent(); i++) {
					this.printIfLoggingIsEnabled("       Flow firstDepartureTime of packet " + i + ": " + flw.getDepartureTime(dev, 0, i));
					this.printIfLoggingIsEnabled("       Flow lastScheduledTime of packet " + i + ":  " + flw.getScheduledTime(dev, flw.getFlowFromRootToNode(dev).size() - 1, i));
					sumOfLatencies += flw.getScheduledTime(dev, flw.getFlowFromRootToNode(dev).size() - 1, i) - flw.getDepartureTime(dev, 0, i);
				}

				sumOfAvgLatencies += sumOfLatencies/flw.getNumOfPacketsSent();
				this.printIfLoggingIsEnabled("       Calculated average Latency: " + (sumOfLatencies/flw.getNumOfPacketsSent()));
				this.printIfLoggingIsEnabled("       Method average Latency: " + flw.getAverageLatencyToDevice(dev));
				this.printIfLoggingIsEnabled("       Method average Jitter: " + flw.getAverageJitterToDevice(dev));
				this.printIfLoggingIsEnabled("");

			}
			this.printIfLoggingIsEnabled("    Calculated average latency of all devices: " + sumOfAvgLatencies/flw.getPathTree().getLeaves().size());
			sumOfAvgLatencies = 0;
		}

        this.printIfLoggingIsEnabled("\n\n\n"
				+ "==================================================\n" 
				+ "[RESULTS SUMMARY]\n");

		for(Flow f : net.getFlows()){
			this.printIfLoggingIsEnabled(f.getName() + " average latency: " + f.getAverageLatency());
			this.printIfLoggingIsEnabled(f.getName() + " average jitter: " + f.getAverageJitter());
			//this.printIfLoggingIsEnabled(f.getName() + " number of packets sent: " + f.getNumOfPacketsSent());
			overallAverageLatency += f.getAverageLatency();
			overallAverageJitter += f.getAverageJitter();
		}

		overallAverageLatency = overallAverageLatency/net.getFlows().size();
		overallAverageJitter = overallAverageJitter/net.getFlows().size();
		this.printIfLoggingIsEnabled("\nTotal number of scheduled packets: " +  numOfFramesScheduled);
		this.printIfLoggingIsEnabled("Overall average latency: " + overallAverageLatency);
		this.printIfLoggingIsEnabled("Overall average jitter: " + overallAverageJitter);
		this.printIfLoggingIsEnabled("\n"
				+ "==================================================\n");
		/*
		this.printIfLoggingIsEnabled("\nNumber of nodes in the network: 1 ");
		this.printIfLoggingIsEnabled("Number of flows in the network: 5 ");
		this.printIfLoggingIsEnabled("Number of subscribers in the network: 1 ");
		this.printIfLoggingIsEnabled("Overall average latency: " + overallAverageLatency);
		this.printIfLoggingIsEnabled("Overall average jitter: " + overallAverageJitter);
		*/
   }
   
   public void printIfLoggingIsEnabled(String text) {
	   if(this.enableConsoleOutput) {
		   System.out.println(text);  
	   }
   }

   public void printInLineIfLoggingIsEnabled(String text) {
	   if(this.enableConsoleOutput) {
		   System.out.print(text);  
	   }
   }
   
   public void logIfLoggingIsEnabled(PrintWriter out, String text) {
	   if(this.enableLoggerFile) {
		   out.println(text);  
	   }
   }

   public void logInLineIfLoggingIsEnabled(PrintWriter out, String text) {
	   if(this.enableLoggerFile) {
		   out.println(text);  
	   }
   }
   
	public Boolean getEnableConsoleOutput() {
		return enableConsoleOutput;
	}

	public void setEnableConsoleOutput(Boolean enableConsoleOutput) {
		this.enableConsoleOutput = enableConsoleOutput;
	}
	
	public Boolean getEnableLoggerFile() {
		return enableLoggerFile;
	}

	public void setEnableLoggerFile(Boolean enableLoggerFile) {
		this.enableLoggerFile = enableLoggerFile;
	}
}
