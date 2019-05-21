package schedule_generator;
import java.util.*;
import java.io.*;


public class GeneratedCode {
	public void runTestCase(){

        /* 
        * GENERATING DEVICES
        */
                               //Prd, ft1, hc, sc, ps
        Device rdc1 = new Device(20000, 0, 100, 0, 100);
        Device rdc2 = new Device(20000, 0, 100, 0, 100);
        Device rdc3 = new Device(20000, 0, 100, 0, 100);
        Device rdc4 = new Device(20000, 0, 100, 0, 100);
        Device pl   = new Device(20000, 0, 100, 0, 100);
        Device cpu  = new Device(20000, 0, 100, 0, 100);
        Device io   = new Device(20000, 0, 100, 0, 100);
        Device mem  = new Device(20000, 0, 100, 0, 100);


        /* 
        * GENERATING SWITCHES
        */                               
        TSNSwitch switch1 = new TSNSwitch("switch1", // Name
                                                100, // Max packet size
                                                1,   // Time to travel
                                                13,  // Transmission Time
                                                100, // Port speed
                                                1,   // GB size
                                                400, // Lower bound cycle time
                                                20000); // Upper bound cycle time


        /* 
        * GENERATING PORTS
        */
        // THERE IS ONLY ONE SWITCH


        /* 
        * LINKING SWITCHES TO DEVICES 
        */
        Cycle cycle0 = new Cycle(50); 
        switch1.createPort(rdc1, cycle0);
        Cycle cycle1 = new Cycle(50); 
        switch1.createPort(rdc2, cycle1);
        Cycle cycle2 = new Cycle(50); 
        switch1.createPort(rdc3, cycle2);
        Cycle cycle3 = new Cycle(50); 
        switch1.createPort(rdc4, cycle3);
        Cycle cycle4 = new Cycle(50); 
        switch1.createPort(pl, cycle4);
        Cycle cycle5 = new Cycle(50); 
        switch1.createPort(cpu, cycle5);
        Cycle cycle6 = new Cycle(50); 
        switch1.createPort(io, cycle6);
        Cycle cycle7 = new Cycle(50); 
        switch1.createPort(mem, cycle7);

        LinkedList<PathNode> nodeList;
        
        /* 
        * GENERATING CPU-RDC FLOWS
        */

        // -> RDC

        Flow flow1 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree1 = new PathTree();
        PathNode pathNode1;
        pathNode1 = pathTree1.addRoot(rdc1);
        pathNode1 = pathNode1.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode1);
        nodeList.getFirst().addChild(cpu);
        nodeList.removeFirst();
        flow1.setPathTree(pathTree1);

        Flow flow2 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree2 = new PathTree();
        PathNode pathNode2;
        pathNode2 = pathTree2.addRoot(rdc2);
        pathNode2 = pathNode2.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode2);
        nodeList.getFirst().addChild(cpu);
        nodeList.removeFirst();
        flow2.setPathTree(pathTree2);

        Flow flow3 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree3 = new PathTree();
        PathNode pathNode3;
        pathNode3 = pathTree3.addRoot(rdc3);
        pathNode3 = pathNode3.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode3);
        nodeList.getFirst().addChild(cpu);
        nodeList.removeFirst();
        flow3.setPathTree(pathTree3);

        Flow flow4 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree4 = new PathTree();
        PathNode pathNode4;
        pathNode4 = pathTree4.addRoot(rdc4);
        pathNode4 = pathNode4.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode4);
        nodeList.getFirst().addChild(cpu);
        nodeList.removeFirst();
        flow4.setPathTree(pathTree4);

        // -> CPU

        Flow flow5 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree5 = new PathTree();
        PathNode pathNode5;
        pathNode5 = pathTree5.addRoot(cpu);
        pathNode5 = pathNode5.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode5);
        nodeList.getFirst().addChild(rdc1);
        nodeList.removeFirst();
        flow5.setPathTree(pathTree5);

        Flow flow6 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree6 = new PathTree();
        PathNode pathNode6;
        pathNode6 = pathTree6.addRoot(cpu);
        pathNode6 = pathNode6.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode6);
        nodeList.getFirst().addChild(rdc2);
        nodeList.removeFirst();
        flow6.setPathTree(pathTree6);

        Flow flow7 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree7 = new PathTree();
        PathNode pathNode7;
        pathNode7 = pathTree7.addRoot(cpu);
        pathNode7 = pathNode7.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode7);
        nodeList.getFirst().addChild(rdc3);
        nodeList.removeFirst();
        flow7.setPathTree(pathTree7);

        Flow flow8 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree8 = new PathTree();
        PathNode pathNode8;
        pathNode8 = pathTree8.addRoot(cpu);
        pathNode8 = pathNode8.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode8);
        nodeList.getFirst().addChild(rdc4);
        nodeList.removeFirst();
        flow8.setPathTree(pathTree8);
        
        /* 
        * GENERATING CPU-IO FLOWS
        */

        Flow flow9 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree9 = new PathTree();
        PathNode pathNode9;
        pathNode9 = pathTree9.addRoot(cpu);
        pathNode9 = pathNode9.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode9);
        nodeList.getFirst().addChild(io);
        nodeList.removeFirst();
        flow9.setPathTree(pathTree9);

        Flow flow10 = new Flow(Flow.PUBLISH_SUBSCRIBE);
        PathTree pathTree10 = new PathTree();
        PathNode pathNode10;
        pathNode10 = pathTree10.addRoot(io);
        pathNode10 = pathNode10.addChild(switch1);
        nodeList = new LinkedList<PathNode>();
        nodeList.add(pathNode10);
        nodeList.getFirst().addChild(cpu);
        nodeList.removeFirst();
        flow10.setPathTree(pathTree10);

        /* 
        * GENERATING PL-MEM FLOWS
        */
        // IT IS BEST EFFORT. CREATE ROOM FOR IT EXPLICITELY


		/* 
		* GENERATING THE NETWORK
		*/
		Network net = new Network();
		net.addSwitch(switch1);
		net.addFlow(flow1);
		net.addFlow(flow2);
		net.addFlow(flow3);
		net.addFlow(flow4);
		net.addFlow(flow5);
		net.addFlow(flow6);
		net.addFlow(flow7);
		net.addFlow(flow8);
		net.addFlow(flow9);
		net.addFlow(flow10);


		ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
		long startTime = System.nanoTime();
		scheduleGenerator.generateSchedule(net);
		long endTime   = System.nanoTime();
		long totalTime = endTime - startTime;


		/* 
		* OUTPUT DATA
		*/
		float overallAverageJitter = 0;
		float overallAverageLatency = 0;
		System.out.println("");
		System.out.println("");
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
			System.out.println("\n\n>>>> INFORMATION OF SWITCH: " + auxSwt.getName() + " <<<<");
			System.out.println("    Cycle start:    " + auxSwt.getPorts().get(0).getCycle().getCycleStart());
			System.out.println("    Cycle duration: " + auxSwt.getPorts().get(0).getCycle().getCycleDuration());
			System.out.println("    Priorities used - ");
				for(Port port : auxSwt.getPorts()) {
					if(port.getCycle().getSlotsUsed().size() == 0) {
						continue;
					}
					System.out.println("        Port name:       " + port.getName());
					System.out.println("        Connects to:     " + port.getConnectsTo());

					System.out.print("        Fragments:       ");
					for(FlowFragment ffrag : port.getFlowFragments()) {
						System.out.print(ffrag.getName() + ", ");
					}
					System.out.println();

					auxCycle = port.getCycle();
				for(int i = 0; i < auxCycle.getSlotsUsed().size(); i++) {
					System.out.println("        Priority number: " + auxCycle.getSlotsUsed().get(i));
					System.out.println("        Slot start:      " + auxCycle.getSlotStart(auxCycle.getSlotsUsed().get(i)));
					System.out.println("        Slot duration:   " + auxCycle.getSlotDuration(auxCycle.getSlotsUsed().get(i)));
					System.out.println("        ------------------------");
				}
			}
		}

		System.out.println("");

		float sumOfAvgLatencies = 0;
		float sumOfLatencies;
		int flowCounter = 0;
		for(Flow flw : net.getFlows()){


			System.out.println("\n\n>>>> INFORMATION OF FLOW" + flowCounter++ + " <<<<\n");

			System.out.println("    Path tree of the flow:");
			for(PathNode node : flw.getPathTree().getLeaves()) {
				auxNodes = flw.getNodesFromRootToNode((Device) node.getNode());
				auxFlowFragments = flw.getFlowFromRootToNode((Device) node.getNode());
				System.out.print("        Path to " + ((Device) node.getNode()).getName() + ": ");
				auxCount = 0;
				for(PathNode auxNode : auxNodes) {
					if(auxNode.getNode() instanceof Device) {
						System.out.print(((Device) auxNode.getNode()).getName() + ", ");
					} else if (auxNode.getNode() instanceof TSNSwitch) {
						System.out.print(((TSNSwitch) auxNode.getNode()).getName() +	"(" + auxFlowFragments.get(auxCount).getName() + "), ");
						auxCount++;
					}
				}
				System.out.println("");
			}


			System.out.println();			System.out.println();			for(PathNode node : flw.getPathTree().getLeaves()) {
				Device dev = (Device) node.getNode();

				sumOfLatencies = 0;
				System.out.println("    Packets heading to " + dev.getName() + ":");

				for(int i = 0; i < Network.PACKETUPPERBOUNDRANGE; i++) {
					System.out.println("       Flow firstDepartureTime of packet " + i + ": " + flw.getDepartureTime(dev, 0, i));
					System.out.println("       Flow lastScheduledTime of packet " + i + ":  " + flw.getScheduledTime(dev, flw.getFlowFromRootToNode(dev).size() - 1, i));
					sumOfLatencies += flw.getScheduledTime(dev, flw.getFlowFromRootToNode(dev).size() - 1, i) - flw.getDepartureTime(dev, 0, i);
				}

				sumOfAvgLatencies += sumOfLatencies/Network.PACKETUPPERBOUNDRANGE;
				System.out.println("       Calculated average Latency: " + (sumOfLatencies/Network.PACKETUPPERBOUNDRANGE));
				System.out.println("       Method average Latency: " + flw.getAverageLatencyToDevice(dev));
				System.out.println("       Method average Jitter: " + flw.getAverageJitterToDevice(dev));
				System.out.println("");

			}
			System.out.println("    Calculated average latency of all devices: " + sumOfAvgLatencies/flw.getPathTree().getLeaves().size());
			sumOfAvgLatencies = 0;
		}




		System.out.println("Execution time: " + ((float) totalTime)/1000000000 + " seconds\n ");

		System.out.println("Flow 1 average latency: " + flow1.getAverageLatency());
		System.out.println("Flow 1 average jitter: " + flow1.getAverageJitter());
		overallAverageLatency += flow1.getAverageLatency(); 
		overallAverageJitter += flow1.getAverageJitter();
		System.out.println("Flow 2 average latency: " + flow2.getAverageLatency());
		System.out.println("Flow 2 average jitter: " + flow2.getAverageJitter());
		overallAverageLatency += flow2.getAverageLatency(); 
		overallAverageJitter += flow2.getAverageJitter();
		System.out.println("Flow 3 average latency: " + flow3.getAverageLatency());
		System.out.println("Flow 3 average jitter: " + flow3.getAverageJitter());
		overallAverageLatency += flow3.getAverageLatency(); 
		overallAverageJitter += flow3.getAverageJitter();
		System.out.println("Flow 4 average latency: " + flow4.getAverageLatency());
		System.out.println("Flow 4 average jitter: " + flow4.getAverageJitter());
		overallAverageLatency += flow4.getAverageLatency(); 
		overallAverageJitter += flow4.getAverageJitter();
		System.out.println("Flow 5 average latency: " + flow5.getAverageLatency());
		System.out.println("Flow 5 average jitter: " + flow5.getAverageJitter());
		overallAverageLatency += flow5.getAverageLatency(); 
		overallAverageJitter += flow5.getAverageJitter();
		System.out.println("Flow 6 average latency: " + flow6.getAverageLatency());
		System.out.println("Flow 6 average jitter: " + flow6.getAverageJitter());
		overallAverageLatency += flow6.getAverageLatency(); 
		overallAverageJitter += flow6.getAverageJitter();
		System.out.println("Flow 7 average latency: " + flow7.getAverageLatency());
		System.out.println("Flow 7 average jitter: " + flow7.getAverageJitter());
		overallAverageLatency += flow7.getAverageLatency(); 
		overallAverageJitter += flow7.getAverageJitter();
		System.out.println("Flow 8 average latency: " + flow8.getAverageLatency());
		System.out.println("Flow 8 average jitter: " + flow8.getAverageJitter());
		overallAverageLatency += flow8.getAverageLatency(); 
		overallAverageJitter += flow8.getAverageJitter();
		System.out.println("Flow 9 average latency: " + flow9.getAverageLatency());
		System.out.println("Flow 9 average jitter: " + flow9.getAverageJitter());
		overallAverageLatency += flow9.getAverageLatency(); 
		overallAverageJitter += flow9.getAverageJitter();
		System.out.println("Flow 10 average latency: " + flow10.getAverageLatency());
		System.out.println("Flow 10 average jitter: " + flow10.getAverageJitter());
		overallAverageLatency += flow10.getAverageLatency(); 
		overallAverageJitter += flow10.getAverageJitter();
		overallAverageLatency = overallAverageLatency/10;
		overallAverageJitter = overallAverageJitter/10;

		System.out.println("\nNumber of nodes in the network: 3 ");
		System.out.println("Number of flows in the network: 11 ");
		System.out.println("Number of subscribers in the network: 47 ");
		System.out.println("Overall average latency: " + overallAverageLatency);
		System.out.println("Overall average jitter: " + overallAverageJitter);
	}
}
