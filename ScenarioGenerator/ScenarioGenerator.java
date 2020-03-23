import java.io.*;

import java.util.*;

import java.util.Random;


public class ScenarioGenerator {

	public static void main(String []args){
		Random rand = new Random();

		// Number of nodes in a network and number of subscribers in a flow. 
		// Both vary according to the configuration of the desired topology.
		int numberOfNodes = 0;
		int numOfSubscribers = 0;
		
		// MAIN CONFIGURATION VARIABLES
		int numOfFlows = 11; // Number of distinct flows in the network
		int configuration = 1; // 1 - small, 2 - medium, 3 - large
		int packetPeriodicity = 2000; // 2000 - normal, 1000 - high, 500 - very high
		int maxBranching = 2; // Maximum branching of the path tree
		
		if (args.length > 0) {
			numOfFlows = Integer.parseInt(args[0]);
			configuration = Integer.parseInt(args[1]);
			packetPeriodicity = Integer.parseInt(args[2]);
			maxBranching = Integer.parseInt(args[3]);
		}

		
		if( configuration == 1 ){
			numberOfNodes = 3;
			numOfSubscribers = 5;
		} else if ( configuration == 2 ) {
			numberOfNodes = 5;
			numOfSubscribers = 10;
		} else if ( configuration == 3 ){
			numberOfNodes = 7;
			numOfSubscribers = 15;
		}
		
		
		int numOfDevices = 50;
		int numOfSwitches = 10;
		

		// TIME UNITS ARE IN MICROSECOND
		//Device properties
		/*
		 * Values for the packet periodicity were taken from the white paper
		 * "Time Sensitive Networks for Flexible Manufacturing Testbed - 
		 * Description of Converged Traffic Types", Industrial Internet
		 * Consortium, 2018.
		 * 
		 * These values match the caracteristic of a Isochronous flow in which
		 * the hard constraint usually matches the transmission period.
		*/
		int hardConstraintTime = 1000;  // Hard constraint time is used within 
		int firstT1Time = 0; // Guessed by z3
		
		//Switch properties
		int timeToTravel = 1; // Since the time to travel taken is in nanosseconds, leave it at 1. It is actually faster
		int transmissionTime = 13; // Taken from "Traffic Planning for Time-Sensitive Communication", Steiner et al., 2018.
		int gbSize = 1; 
		
		//Cycle properties
		int upperBoundCycleTime = 3000;
		int lowerBoundCycleTime = 400;
		int maximumSlotDuration = 50;
		
		
		int firstCycleStart = 0;
		int softConstraintTime = 0; // Not beign used yet
		int packetSize = 1625; // Bytes
		int portSpeed = 125; // Bytes per microssecond
		int maxPacketSize = 100; // Not beign used yet


		String startDevice = "";
		ArrayList<String> endDevices = new ArrayList<String>();
		
		int deviceNumber;
		int switchNumber;
		int totalNumberOfSubscribers = 0;
		ArrayList<Integer> usedPublishers = new ArrayList<Integer>();
		ArrayList<Integer> usedDevices = new ArrayList<Integer>();
		ArrayList<Integer> usedSwitches = new ArrayList<Integer>();
		int auxSwitchCount = 0;
		int auxCycleCount = 0;
		int deviceSwitchRatio = numOfDevices/numOfSwitches;
		int numOfBranches = 0;



		try {
			// PrintWriter out = new PrintWriter("/home/asus/git/tsn-scheduler/src/schedule_generator/GeneratedCode.java");
			PrintWriter out = new PrintWriter("GeneratedCode.java");

			out.println("package schedule_generator;");

			out.println("import java.util.*;");
			out.println("import java.io.*;\n\n");
			out.println("public class GeneratedCode {");
			out.println("\tpublic void runTestCase(){\n");


			// GENERATING DEVICES
			out.println("\t\t/* \n\t\t* GENERATING DEVICES\n\t\t*/");
			for(int i = 0; i < numOfDevices; i++){
				// packetPeriodicity = 1000;
				// packetPeriodicity = rand.nextInt(upperBoundPacketPeriodicity - lowerBoundPacketPeriodicity) + lowerBoundPacketPeriodicity;
				out.println("\t\tDevice dev" + i + " = new Device(" + packetPeriodicity + ", " + firstT1Time + ", " + hardConstraintTime + ", " + packetSize + ");");					
			}

			
			out.println("");
			out.println("");
			out.println("\t\t/* \n\t\t* GENERATING SWITCHES\n\t\t*/");
			// GENERATING SWITCHES
			for(int i = 0; i < numOfSwitches; i++){
				out.println("\t\tTSNSwitch switch" + i + " = new TSNSwitch(" + "\"switch" + i + "\"," + maxPacketSize + ", " + timeToTravel + ", " + portSpeed + ", " + gbSize + ", " + lowerBoundCycleTime + ", " + upperBoundCycleTime + ");");
			}

			out.println("");
			out.println("");
			out.println("\t\t/* \n\t\t* GENERATING PORTS\n\t\t*/");
			// CREATING PORTS 
			auxCycleCount = 0;
			for(int i = 0; i < numOfSwitches; i++){
				for(int j = i + 1; j < numOfSwitches; j++){
					out.println("\t\tCycle cycle" + auxCycleCount + " = new Cycle(" + maximumSlotDuration + "); ");
					out.println("\t\tswitch" + i + ".createPort(switch" + j + ", cycle" + auxCycleCount++ + ");");
					out.println("\t\tCycle cycle" + auxCycleCount + " = new Cycle(" + maximumSlotDuration + "); ");
					out.println("\t\tswitch" + j + ".createPort(switch" + i + ", cycle" + auxCycleCount + ");");
					auxCycleCount++;

				}
			}

			out.println("");
			out.println("\t\t/* \n\t\t* LINKING SWITCHES TO DEVICES \n\t\t*/");
			//Devices are equaly distributed to the switches
			auxSwitchCount = 0;
			for(int i = 0; i < numOfDevices; i++){
				out.println("\t\tCycle cycle" + auxCycleCount + " = new Cycle(" + maximumSlotDuration + "); ");
				out.println("\t\tswitch" + auxSwitchCount + ".createPort(dev" + i + ", cycle" + auxCycleCount + ");");
				auxCycleCount++;

				if(((i+1)%deviceSwitchRatio == 0) && !(auxSwitchCount == numOfSwitches - 1) && (i != 0)){
					auxSwitchCount++;
				}
			}

			out.println("");
			out.println("");

			out.println("\t\t/* \n\t\t* GENERATING FLOWS\n\t\t*/");
			// GENERATING FLOWS
			String flowName;
			String pathTreeName;
			LinkedList<Integer> nodeStack;
			out.println("\t\tLinkedList<PathNode> nodeList;");

			for(int i = 0; i < numOfFlows; i++){
				out.println();
				usedDevices = new ArrayList<Integer>();
				usedSwitches = new ArrayList<Integer>();

				flowName = "flow" + String.valueOf(i);
				pathTreeName = "pathTree" + String.valueOf(i);
				
				out.println("\t\tFlow " + flowName + " = new Flow(Flow.PUBLISH_SUBSCRIBE);");
				out.println("\t\tPathTree " + pathTreeName + " = new PathTree();");
				
				while(true){
					deviceNumber = rand.nextInt(numOfDevices);
					if(usedPublishers.contains(deviceNumber)){
						continue;
					}
					
					usedPublishers.add(deviceNumber);
					break;
				}
				
				usedDevices.add(deviceNumber);
				startDevice = "dev" + deviceNumber;
				
				out.println("\t\tPathNode pathNode" + i + ";");
				out.println("\t\tpathNode" + i + " = " + pathTreeName + ".addRoot(" + startDevice + ");");
				out.println("\t\tpathNode" + i + " = pathNode" + i + ".addChild(switch" + ((Integer) deviceNumber/deviceSwitchRatio) + ");");
					
				usedSwitches.add((Integer) deviceNumber/deviceSwitchRatio);
				nodeStack = new LinkedList<Integer>();
				nodeStack.add((Integer) deviceNumber/deviceSwitchRatio);

				out.println("\t\tnodeList = new LinkedList<PathNode>();");
				out.println("\t\tnodeList.add(pathNode" + i +");");
				for(int j = 1; j < numberOfNodes; j++){
					
					if ((numberOfNodes - j) < maxBranching){
						numOfBranches = rand.nextInt(numberOfNodes - j) + 1;
					} else {
						numOfBranches = rand.nextInt(maxBranching) + 1;
					}
					
					for(int count = 0; count < numOfBranches - 1; count++){
						while(true){
							switchNumber = rand.nextInt(numOfSwitches);
							// System.out.println("Here 2 " + switchNumber);
							if(usedSwitches.contains(switchNumber)){
								continue;
							}
		
							usedSwitches.add(switchNumber);
							break;
						}

						nodeStack.getFirst();
						nodeStack.add(switchNumber);

						out.println("\t\tnodeList.add(nodeList.getFirst().addChild(switch" + switchNumber + "));");
						j++;
					}

					

					while(true){
						switchNumber = rand.nextInt(numOfSwitches);
						// System.out.println("Here 1 " + switchNumber);
						if(usedSwitches.contains(switchNumber)){
							continue;
						}
						
						usedSwitches.add(switchNumber);
						break;
					}

					nodeStack.removeFirst();
					nodeStack.add(switchNumber);

					out.println("\t\tnodeList.add(nodeList.removeFirst().addChild(switch" + switchNumber + "));");

					
				}
				
				int numOfLeaves = nodeStack.size();
				int auxNumOfSUbscribers = numOfSubscribers;

				if(numOfLeaves * deviceSwitchRatio < auxNumOfSUbscribers){
					auxNumOfSUbscribers = nodeStack.size() * deviceSwitchRatio;
				}

				// System.out.println("On Flow " + i + " with " + numOfLeaves + " leaf switches | " + ((int)auxNumOfSUbscribers/numOfLeaves));
				while(!nodeStack.isEmpty()){
					switchNumber = nodeStack.pop();
					for(int j = 0; j < ((int)auxNumOfSUbscribers/numOfLeaves); j++){
						deviceNumber = (switchNumber * deviceSwitchRatio) + j;
						out.println("\t\tnodeList.getFirst().addChild(dev" + deviceNumber + ");");
						totalNumberOfSubscribers++;
						// System.out.println("Adding device number " + deviceNumber + " to switch number " + switchNumber);
					}

					if(auxNumOfSUbscribers%numOfLeaves != 0 && nodeStack.isEmpty()){
						for(int j = 0; j < (auxNumOfSUbscribers%numOfLeaves) - 1; j++){
							deviceNumber = switchNumber * deviceSwitchRatio + j + ((int)auxNumOfSUbscribers/numOfLeaves);
							totalNumberOfSubscribers++;
							out.println("\t\tnodeList.getFirst().addChild(dev" + deviceNumber + ");");
							// System.out.println("Adding device number " + deviceNumber + " to switch number " + switchNumber);
						}
					}

					out.println("\t\tnodeList.removeFirst();");
				}

				/*
				while(!nodeStack.isEmpty()){
					while(true){
						deviceNumber = rand.nextInt(deviceSwitchRatio) + deviceSwitchRatio*nodeStack.removeFirst();

						if(!usedDevices.contains(deviceNumber)){
							break;
						}
					}

					out.println("nodeList.removeFirst().addChild(dev" + deviceNumber + ");");
					usedDevices.add(deviceNumber);	
				}
				*/

				out.println("\t\t" + flowName + ".setPathTree(" + pathTreeName + ");");
			}
			

			out.println("");
			out.println("");
			out.println("\t\t/* \n\t\t* GENERATING THE NETWORK\n\t\t*/");

			out.println("\t\tNetwork net = new Network();");
			//CREATING THE NETWORK
			for(int i = 0; i < numOfSwitches; i++){
				out.println("\t\tnet.addSwitch(switch" + i + ");");
			}

			for(int i = 0; i < numOfFlows; i++){
				out.println("\t\tnet.addFlow(flow" + i + ");");
			}

			out.println(
				"\n\n\t\tScheduleGenerator scheduleGenerator = new ScheduleGenerator();\n" +
				"\t\tlong startTime = System.nanoTime();\n" + 
				"\t\tscheduleGenerator.generateSchedule(net);\n"+
				"\t\tlong endTime   = System.nanoTime();\n" +
				"\t\tlong totalTime = endTime - startTime;\n" +
				"\t\tint numOfFramesScheduled = 0;" 
			);

			out.println("");
			out.println("");
			out.println("\t\t/* \n\t\t* OUTPUT DATA\n\t\t*/");

			out.println("\t\tfloat overallAverageJitter = 0;");
			out.println("\t\tfloat overallAverageLatency = 0;");
			out.println("\t\tSystem.out.println(\"\");");
			out.println("\t\tSystem.out.println(\"\");");
			
			out.println(
				"\t\tint auxCount = 0; \n"+
				"\t\tArrayList<PathNode> auxNodes;\n" +
				"\t\tArrayList<FlowFragment> auxFlowFragments;\n" 
			);
						
			out.println(
				"\t\tCycle auxCycle;\n" +
				"\t\tTSNSwitch auxSwt;\n" +
				"\t\tint flagContinueLoop = 1;\n\n" +
				"\t\tfor(Switch swt : net.getSwitches()) {\n" +
				"\t\t\tflagContinueLoop = 1;\n" +
				"\t\t\tauxSwt = (TSNSwitch) swt;\n" +
					
				"\t\t\tfor(Port port : auxSwt.getPorts()) {\n" +
				"\t\t\t\tif(port.getCycle().getSlotsUsed().size() != 0) {\n" +
				"\t\t\t\t\tflagContinueLoop = 0;\n" +
				"\t\t\t\t\tbreak;\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n\n" +
					
				"\t\t\tif(flagContinueLoop == 1) {\n" +
				"\t\t\t\tcontinue;\n" +
				"\t\t\t}\n" +

				"\t\t\tSystem.out.println(\"\\n\\n>>>> INFORMATION OF SWITCH: \" + auxSwt.getName() + \" <<<<\");\n" +
				// "\t\t\tSystem.out.println(\"    Cycle start:    \" + auxSwt.getPorts().get(0).getCycle().getCycleStart());\n" +
				// "\t\t\tSystem.out.println(\"    Cycle duration: \" + auxSwt.getPorts().get(0).getCycle().getCycleDuration());\n" +
				"\t\t\tSystem.out.println(\"    Port list - \");\n" +
					
				"\t\t\t\tfor(Port port : auxSwt.getPorts()) {\n" +
				"\t\t\t\t\tif(port.getCycle().getSlotsUsed().size() == 0) {\n" +
				"\t\t\t\t\t\tcontinue;\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t\tSystem.out.println(\"        => Port name:       \" + port.getName());\n" +
				"\t\t\t\t\tSystem.out.println(\"        Connects to:     \" + port.getConnectsTo());\n\n" +
				"\t\t\t\t\tSystem.out.println(\"        Cycle start:    \" + port.getCycle().getCycleStart());\n" +
				"\t\t\t\t\tSystem.out.println(\"        Cycle duration: \" + port.getCycle().getCycleDuration());\n" +
				"\t\t\t\t\tSystem.out.print(\"        Fragments:       \");\n" + 
				"\t\t\t\t\tfor(FlowFragment ffrag : port.getFlowFragments()) {\n" +
				"\t\t\t\t\t\tSystem.out.print(ffrag.getName() + \", \");\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t\tSystem.out.println();\n\n" +


				"\t\t\t\t\tauxCycle = port.getCycle();\n" +
				"\t\t\t\t\tSystem.out.println(\"        Slots per prt:   \" +  auxCycle.getNumOfSlots());\n" +

				"\t\t\t\tfor(int i = 0; i < auxCycle.getSlotsUsed().size(); i++) {\n" +
				"\t\t\t\t\tSystem.out.println(\"        Priority number: \" + auxCycle.getSlotsUsed().get(i));\n" +

				"\t\t\t\t\tfor(int j = 0; j < auxCycle.getNumOfSlots(); j++) {\n" + 

				"\t\t\t\t\t\tSystem.out.println(\"          Index \" + j + \" Slot start:      \" + auxCycle.getSlotStart(auxCycle.getSlotsUsed().get(i), j));\n" +
				"\t\t\t\t\t\tSystem.out.println(\"          Index \" + j + \" Slot duration:   \" + auxCycle.getSlotDuration(auxCycle.getSlotsUsed().get(i), j));\n" +
				
				"\t\t\t\t\t}" +

				"\t\t\t\t\tSystem.out.println(\"        ------------------------\");\n" +


				"\t\t\t\t}\n" +
						
				"\t\t\t}\n" +
				
				"\t\t}\n\n" +
				
				"\t\tSystem.out.println(\"\");\n" +


				"\n\t\tfloat sumOfAvgLatencies = 0;\n" +
				"\t\tfloat sumOfLatencies;\n" +
				"\t\tint flowCounter = 0;\n" +
				"\t\tfor(Flow flw : net.getFlows()){\n" +
				"\n\n\t\t\tSystem.out.println(\"\\n\\n>>>> INFORMATION OF FLOW\" + flowCounter++ + \" <<<<\\n\");\n\n" +
				"\t\t\tSystem.out.println(\"    Total number of packets scheduled: \" + flw.getTotalNumOfPackets());\n" +
				"\t\t\tnumOfFramesScheduled = numOfFramesScheduled + flw.getTotalNumOfPackets();\n" +

				"\t\t\tSystem.out.println(\"    Path tree of the flow:\");\n" +  
				"\t\t\tfor(PathNode node : flw.getPathTree().getLeaves()) {\n" +
				"\t\t\t\tauxNodes = flw.getNodesFromRootToNode((Device) node.getNode());\n" +
				"\t\t\t\tauxFlowFragments = flw.getFlowFromRootToNode((Device) node.getNode());\n" +
				"\t\t\t\tSystem.out.print(\"        Path to \" + ((Device) node.getNode()).getName() + \": \");\n" + 
				"\t\t\t\tauxCount = 0;\n" + 
				"\t\t\t\tfor(PathNode auxNode : auxNodes) {\n" +
				"\t\t\t\t\tif(auxNode.getNode() instanceof Device) {\n" + 
				"\t\t\t\t\t\tSystem.out.print(((Device) auxNode.getNode()).getName() + \", \");\n" +
				"\t\t\t\t\t} else if (auxNode.getNode() instanceof TSNSwitch) {\n" + 
				"\t\t\t\t\t\tSystem.out.print(((TSNSwitch) auxNode.getNode()).getName() +	\"(\" + auxFlowFragments.get(auxCount).getName() + \"), \");\n"+
				"\t\t\t\t\t\tauxCount++;\n" +
				"\t\t\t\t\t}\n" +
				"\t\t\t\t}\n" +
				"\t\t\t\tSystem.out.println(\"\");\n" +
				"\t\t\t}\n\n\n" +
				
				"\t\t\tSystem.out.println();\n" +
				"\t\t\tSystem.out.println();\n" +


				"\t\t\tfor(PathNode node : flw.getPathTree().getLeaves()) {\n" +
				"\t\t\t\tDevice dev = (Device) node.getNode();\n\n" +
						
				"\t\t\t\tsumOfLatencies = 0;\n" +
				"\t\t\t\tSystem.out.println(\"    Packets heading to \" + dev.getName() + \":\");\n\n" +
						
				"\t\t\t\tfor(int i = 0; i < flw.getNumOfPacketsSent(); i++) {\n"+
				"\t\t\t\t\tSystem.out.println(\"       Flow firstDepartureTime of packet \" + i + \": \" + flw.getDepartureTime(dev, 0, i));\n" +
				"\t\t\t\t\tSystem.out.println(\"       Flow lastScheduledTime of packet \" + i + \":  \" + flw.getScheduledTime(dev, flw.getFlowFromRootToNode(dev).size() - 1, i));\n" +
				"\t\t\t\t\tsumOfLatencies += flw.getScheduledTime(dev, flw.getFlowFromRootToNode(dev).size() - 1, i) - flw.getDepartureTime(dev, 0, i);\n" +
				"\t\t\t\t}\n\n" +
						
				"\t\t\t\tsumOfAvgLatencies += sumOfLatencies/flw.getNumOfPacketsSent();\n" +
				"\t\t\t\tSystem.out.println(\"       Calculated average Latency: \" + (sumOfLatencies/flw.getNumOfPacketsSent()));\n" +
				"\t\t\t\tSystem.out.println(\"       Method average Latency: \" + flw.getAverageLatencyToDevice(dev));\n"+
				"\t\t\t\tSystem.out.println(\"       Method average Jitter: \" + flw.getAverageJitterToDevice(dev));\n"+
				"\t\t\t\tSystem.out.println(\"\");\n\n" +
							
				"\t\t\t}\n"+

				"\t\t\tSystem.out.println(\"    Calculated average latency of all devices: \" + sumOfAvgLatencies/flw.getPathTree().getLeaves().size());\n" +
				"\t\t\tsumOfAvgLatencies = 0;\n" +
				"\t\t}\n\n"

			);

			out.println(
				"\n\n\t\tSystem.out.println(\"Execution time: \" + ((float) totalTime)/1000000000 + \" seconds\\n \");"  
			);
			for(int i = 0; i < numOfFlows; i++){
				out.println(
					"\t\tSystem.out.println(\"Flow " + i + " average latency: \" + flow" + i + ".getAverageLatency());\n" +
					"\t\tSystem.out.println(\"Flow " + i + " average jitter: \" + flow" + i + ".getAverageJitter());"
				);
				out.println(
					"\t\toverallAverageLatency += flow" + i + ".getAverageLatency(); \n" +
					"\t\toverallAverageJitter += flow" + i + ".getAverageJitter();" 					
				);
			}

			out.println("\t\toverallAverageLatency = overallAverageLatency/" + numOfFlows + ";");
			out.println("\t\toverallAverageJitter = overallAverageJitter/" + numOfFlows + ";");
			out.println();
			out.println(
				"\t\tSystem.out.println(\"\\nNumber of nodes in the network: " + numberOfNodes + " \");"
			);
			out.println(
				"\t\tSystem.out.println(\"Number of flows in the network: " + numOfFlows + " \");"
			);
			out.println(
				"\t\tSystem.out.println(\"Number of subscribers in the network: " + totalNumberOfSubscribers + " \");"
			);
			out.println(
				"\t\tSystem.out.println(\"Total number of scheduled packets: \" +  numOfFramesScheduled);"
			);
			out.println(
				"\t\tSystem.out.println(\"Overall average latency: \" + overallAverageLatency);\n" +
				"\t\tSystem.out.println(\"Overall average jitter: \" + overallAverageJitter);"
			);

			out.println("\t}");
			out.println("}");
			out.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}

	}
}








