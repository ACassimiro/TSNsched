package generated_scenarios;

import network.Cycle;
import network.Flow;
import network.Network;
import nodes.Device;
import nodes.TSNSwitch;
import schedule_generator.ScheduleGenerator;

public class MediumScenario {
	public void runTestCase(){

		/*
		 * GENERATING DEVICES
		 */
		Device dev0 = new Device(200, 0, 1000, 750);
		Device dev1 = new Device(500, 0, 1000, 750);
		Device dev2 = new Device(400, 0, 1000, 750);
		Device dev3 = new Device(400, 0, 1000, 750);
		Device dev4 = new Device(400, 0, 1000, 750);
		Device dev5 = new Device(300, 0, 1000, 750);
		Device dev6 = new Device(700, 0, 1000, 750);


		/*
		 * GENERATING SWITCHES
		 */
		TSNSwitch switch0 = new TSNSwitch("switch0",100, 1, 125, 1, 400, 3000);
		TSNSwitch switch1 = new TSNSwitch("switch1",100, 1, 125, 1, 400, 3000);
		TSNSwitch switch2 = new TSNSwitch("switch2",100, 1, 125, 1, 400, 3000);


		/*
		 * GENERATING SWITCH CONNECTION PORTS
		 */
		Cycle cycle0 = new Cycle(50);
		switch0.createPort(switch2, cycle0);
		Cycle cycle1 = new Cycle(50);
		switch2.createPort(switch0, cycle1);
		
		Cycle cycle2 = new Cycle(50);
		switch0.createPort(switch1, cycle2);
		Cycle cycle3 = new Cycle(50);
		switch1.createPort(switch0, cycle3);		

		/*
		 * LINKING SWITCHES TO DEVICES
		 */
		Cycle cycle4 = new Cycle(50);
		switch0.createPort(dev0, cycle4);
		Cycle cycle5 = new Cycle(50);
		switch1.createPort(dev1, cycle5);
		Cycle cycle6 = new Cycle(50);
		switch1.createPort(dev2, cycle6);
		Cycle cycle7 = new Cycle(50);
		switch1.createPort(dev3, cycle7);
		Cycle cycle8 = new Cycle(50);
		switch2.createPort(dev4, cycle8);
		Cycle cycle9 = new Cycle(50);
		switch2.createPort(dev5, cycle9);
		Cycle cycle10 = new Cycle(50);
		switch2.createPort(dev6, cycle10);
		
		/*
		 * GENERATING FLOWS
		 */

		Flow flow1 = new Flow(Flow.UNICAST);
		flow1.setStartDevice(dev0);
		flow1.addToPath(switch0);
		flow1.addToPath(switch1);
		flow1.setEndDevice(dev2);
		
		Flow flow2 = new Flow(Flow.UNICAST);
		flow2.setStartDevice(dev5);
		flow2.addToPath(switch2);
		flow2.setEndDevice(dev4);
		
		Flow flow3 = new Flow(Flow.UNICAST);
		flow3.setStartDevice(dev6);
		flow3.addToPath(switch2);
		flow3.addToPath(switch0);
		flow3.addToPath(switch1);
		flow3.setEndDevice(dev3);
		
		Flow flow4 = new Flow(Flow.UNICAST);
		flow4.setStartDevice(dev1);
		flow4.addToPath(switch1);
		flow4.addToPath(switch0);
		flow4.addToPath(switch2);
		flow4.setEndDevice(dev4);


		/*
		 * GENERATING THE NETWORK
		 */
		Network net = new Network(25);
		net.addDevice(dev0);
		net.addDevice(dev1);
		net.addDevice(dev2);
		net.addDevice(dev3);
		net.addDevice(dev4);
		net.addDevice(dev5);
		net.addDevice(dev6);
		net.addSwitch(switch0);
		net.addSwitch(switch1);
		net.addSwitch(switch2);
		
		net.addFlow(flow1);
		net.addFlow(flow2);
		net.addFlow(flow3);
		net.addFlow(flow4);


		ScheduleGenerator scheduleGenerator = new ScheduleGenerator(false);
		scheduleGenerator.generateSchedule(net);

	}
}
