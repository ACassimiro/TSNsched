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
		int packetPeriodicity = 1000; // 2000 - normal, 1000 - high, 500 - very high
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
		int packetSize = 72; // Bytes
		
		//Switch properties
		int timeToTravel = 8; // Since the time to travel taken is in nanosseconds, leave it at 1. It is actually faster
		int transmissionTime = 13; // Taken from "Traffic Planning for Time-Sensitive Communication", Steiner et al., 2018.
		int gbSize = 1; 
		int portSpeed = 125; // Bytes per microssecond
		int maxPacketSize = 100; // Not beign used yet
		
		//Cycle properties
		int upperBoundCycleTime = 3000;
		int lowerBoundCycleTime = 400;
		int maximumSlotDuration = 50;
		
		
		int firstCycleStart = 0;
		int softConstraintTime = 0; // Not beign used yet

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
			
			out.println("import com.tsnsched.core.interface_manager.JSONParser;\n");
			out.println("import com.tsnsched.core.network.*;\n");
			out.println("import com.tsnsched.core.nodes.*;\n");
			out.println("import com.tsnsched.core.components.*;\n");
			out.println("import com.tsnsched.core.schedule_generator.*;\n");
			out.println("import com.tsnsched.core.interface_manager.*;\n\n");


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

			
			out.println("\n\n");
			out.println("\t\t/* \n\t\t* GENERATING SWITCHES\n\t\t*/");
			// GENERATING SWITCHES
			for(int i = 0; i < numOfSwitches; i++){
				out.println("\t\tTSNSwitch switch" + i + " = new TSNSwitch(" + "\"switch" + i + "\"," + maxPacketSize + ", " + timeToTravel + ", " + portSpeed + ", " + gbSize + ", " + lowerBoundCycleTime + ", " + upperBoundCycleTime + ");");
			}

			out.println("\n\n");
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
			for(int i = 0; i < numOfDevices; i++){
				out.println("\t\tnet.addDevice(dev" + i + ");");
			}

			for(int i = 0; i < numOfSwitches; i++){
				out.println("\t\tnet.addSwitch(switch" + i + ");");
			}

			for(int i = 0; i < numOfFlows; i++){
				out.println("\t\tnet.addFlow(flow" + i + ");");
			}

			out.println(
				"\n\n\t\tScheduleGenerator scheduleGenerator = new ScheduleGenerator();\n" +
				"\t\tscheduleGenerator.generateSchedule(net);\n"
			);

			out.println("\t}");
			out.println("}");
			out.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}

	}
}








