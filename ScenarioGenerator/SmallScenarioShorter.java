package com.tsnsched.generated_scenarios;
import java.util.*;
import java.io.*;
import com.tsnsched.core.nodes.*;
import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;


public class SmallScenarioShorter {
	public void runTestCase(){

		/*
		 * GENERATING DEVICES
		 */
		Device dev0 = new Device(1000, 0, 1000, 1250);
		Device dev1 = new Device(200, 0, 1000, 1250);

		/*
		 * GENERATING SWITCHES
		 */
		TSNSwitch switch0 = new TSNSwitch("switch0",100, 8, 125, 1, 400, 3000); // valoes não usados 100, 400, 3000

		/*
		 * LINKING SWITCHES TO DEVICES
		 */
		Cycle cycle1 = new Cycle(50);
		switch0.createPort(dev0, cycle1);
		Cycle cycle2 = new Cycle(50);
		switch0.createPort(dev1, cycle2);
		/*
		 * GENERATING FLOWS
		 */
		LinkedList<PathNode> nodeList;

		Flow flow1 = new Flow(Flow.UNICAST);
		flow1.setStartDevice(dev1);
		flow1.addToPath(switch0);
		flow1.setEndDevice(dev0);


		/*
		 * GENERATING THE NETWORK
		 */
		Network net = new Network(25);
		net.addDevice(dev0);
		net.addDevice(dev1);
		net.addSwitch(switch0);
		
		
		net.addFlow(flow1);


		ScheduleGenerator scheduleGenerator = new ScheduleGenerator(false);
		scheduleGenerator.generateSchedule(net);

	}
}
