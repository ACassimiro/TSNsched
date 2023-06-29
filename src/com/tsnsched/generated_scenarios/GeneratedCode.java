package com.tsnsched.generated_scenarios;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//  Copyright (C) 2021  Aellison Cassimiro
//  
//  TSNsched is licensed under the GNU GPL version 3 or later:
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <https://www.gnu.org/licenses/>.

import java.util.*;
import java.io.*;

import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;
import com.tsnsched.core.nodes.*;

public class GeneratedCode {
	public void runTestCase(){
		Boolean loadNetwork = false;

		ScheduleGenerator scheduleGenerator = new ScheduleGenerator(loadNetwork);
		Network net = new Network(25);

		/*
		 * GENERATING DEVICES
		 */
		Device dev0 = new Device(250, 0, 1000, 1500);
		Device dev1 = new Device(800, 0, 1000, 1500);
		Device dev2 = new Device(500, 0, 1000, 1500);
		Device dev3 = new Device(800, 0, 1000, 1500);
		Device dev4 = new Device(800, 0, 1000, 1500);
		Device dev5 = new Device(500, 0, 1000, 1500);
		Device dev6 = new Device(800, 0, 1000, 1500);
		Device dev7 = new Device(800, 0, 1000, 1500);
		Device dev8 = new Device(500, 0, 1000, 1500);
		Device dev9 = new Device(800, 0, 1000, 1500);

		/*
		 * GENERATING SWITCHES
		 */
		TSNSwitch switch0 = new TSNSwitch("switch0", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch1 = new TSNSwitch("switch1", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch2 = new TSNSwitch("switch2", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch3 = new TSNSwitch("switch3", 100, 1, 125, 1, 400, 3000);

		/*
		 * GENERATING SWITCH CONNECTION PORTS
		 */
		Cycle cycle0 = new Cycle(50);
		switch0.createPort(switch2, cycle0);
		Cycle cycle1 = new Cycle(50);
		switch2.createPort(switch0, cycle1);

		Cycle cycle2 = new Cycle(50);
		switch2.createPort(switch1, cycle2);
		Cycle cycle3 = new Cycle(50);
		switch1.createPort(switch2, cycle3);

		Cycle cycle4 = new Cycle(50);
		switch2.createPort(switch3, cycle4);
		Cycle cycle5 = new Cycle(50);
		switch3.createPort(switch2, cycle5);

		/*
		 * LINKING SWITCHES TO DEVICES
		 */
		Cycle cycle6 = new Cycle(50);
		switch0.createPort(dev0, cycle6);
		Cycle cycle7 = new Cycle(50);
		switch1.createPort(dev1, cycle7);
		Cycle cycle8 = new Cycle(50);
		switch1.createPort(dev2, cycle8);
		Cycle cycle9 = new Cycle(50);
		switch1.createPort(dev3, cycle9);
		Cycle cycle10 = new Cycle(50);
		switch2.createPort(dev4, cycle10);
		Cycle cycle11 = new Cycle(50);
		switch2.createPort(dev5, cycle11);
		Cycle cycle12 = new Cycle(50);
		switch2.createPort(dev6, cycle12);
		Cycle cycle13 = new Cycle(50);
		switch3.createPort(dev7, cycle13);
		Cycle cycle14 = new Cycle(50);
		switch3.createPort(dev8, cycle14);
		Cycle cycle15 = new Cycle(50);
		switch3.createPort(dev9, cycle15);

		/*
		 * GENERATING FLOWS
		 */
		//COORDINATION FLOW: 0, 1, 2, 7, 12,17

		LinkedList<PathNode> nodeList;

		//MAIN COORDINATION FLOWS
		Flow flow0 = new Flow(Flow.UNICAST);
		flow0.setStartDevice(dev0);
		flow0.addToPath(switch0);
		flow0.addToPath(switch2);
		flow0.addToPath(switch1);
		flow0.setEndDevice(dev2);

		Flow flow1 = new Flow(Flow.UNICAST);
		flow1.setStartDevice(dev0);
		flow1.addToPath(switch0);
		flow1.addToPath(switch2);
		flow1.setEndDevice(dev5);

		Flow flow2 = new Flow(Flow.UNICAST);
		flow2.setStartDevice(dev0);
		flow2.addToPath(switch0);
		flow2.addToPath(switch2);
		flow2.addToPath(switch3);
		flow2.setEndDevice(dev8);

		//FIRST SUBNET SET
		Flow flow3 = new Flow(Flow.UNICAST);
		flow3.setStartDevice(dev1);
		flow3.addToPath(switch1);
		flow3.setEndDevice(dev2);

		Flow flow4 = new Flow(Flow.UNICAST);
		flow4.setStartDevice(dev2);
		flow4.addToPath(switch1);
		flow4.setEndDevice(dev1);

		Flow flow5 = new Flow(Flow.UNICAST);
		flow5.setStartDevice(dev3);
		flow5.addToPath(switch1);
		flow5.setEndDevice(dev2);

		Flow flow6 = new Flow(Flow.UNICAST);
		flow6.setStartDevice(dev2);
		flow6.addToPath(switch1);
		flow6.setEndDevice(dev3);

		Flow flow7 = new Flow(Flow.UNICAST);
		flow7.setStartDevice(dev2);
		flow7.addToPath(switch1);
		flow7.addToPath(switch2);
		flow7.addToPath(switch0);
		flow7.setEndDevice(dev0);

		//SECOND SUBNET SET
		Flow flow8 = new Flow(Flow.UNICAST);
		flow8.setStartDevice(dev4);
		flow8.addToPath(switch2);
		flow8.setEndDevice(dev5);

		Flow flow9 = new Flow(Flow.UNICAST);
		flow9.setStartDevice(dev5);
		flow9.addToPath(switch2);
		flow9.setEndDevice(dev4);

		Flow flow10 = new Flow(Flow.UNICAST);
		flow10.setStartDevice(dev6);
		flow10.addToPath(switch2);
		flow10.setEndDevice(dev5);

		Flow flow11 = new Flow(Flow.UNICAST);
		flow11.setStartDevice(dev5);
		flow11.addToPath(switch2);
		flow11.setEndDevice(dev6);

		Flow flow12 = new Flow(Flow.UNICAST);
		flow12.setStartDevice(dev5);
		flow12.addToPath(switch2);
		flow12.addToPath(switch0);
		flow12.setEndDevice(dev0);

		//THIRD SUBNET SET
		Flow flow13 = new Flow(Flow.UNICAST);
		flow13.setStartDevice(dev7);
		flow13.addToPath(switch3);
		flow13.setEndDevice(dev8);

		Flow flow14 = new Flow(Flow.UNICAST);
		flow14.setStartDevice(dev8);
		flow14.addToPath(switch3);
		flow14.setEndDevice(dev7);

		Flow flow15 = new Flow(Flow.UNICAST);
		flow15.setStartDevice(dev9);
		flow15.addToPath(switch3);
		flow15.setEndDevice(dev8);

		Flow flow16 = new Flow(Flow.UNICAST);
		flow16.setStartDevice(dev8);
		flow16.addToPath(switch3);
		flow16.setEndDevice(dev9);

		Flow flow17 = new Flow(Flow.UNICAST);
		flow17.setStartDevice(dev8);
		flow17.addToPath(switch3);
		flow17.addToPath(switch2);
		flow17.addToPath(switch0);
		flow17.setEndDevice(dev0);



		/*
		 * GENERATING THE NETWORK
		 */

		net.addDevice(dev0);
		net.addDevice(dev1);
		net.addDevice(dev2);
		net.addDevice(dev3);
		net.addDevice(dev4);
		net.addDevice(dev5);
		net.addDevice(dev6);
		net.addDevice(dev7);
		net.addDevice(dev8);
		net.addDevice(dev9);

		net.addSwitch(switch0);
		net.addSwitch(switch1);
		net.addSwitch(switch2);
		net.addSwitch(switch3);

		net.addFlow(flow0);
		net.addFlow(flow1);
		net.addFlow(flow2);
		net.addFlow(flow3);
		net.addFlow(flow4);
		net.addFlow(flow5);
		/**/
		net.addFlow(flow6);
		net.addFlow(flow7);
		net.addFlow(flow8);
		net.addFlow(flow9);
		net.addFlow(flow10);
		net.addFlow(flow11);
		net.addFlow(flow12);
		net.addFlow(flow13);
		net.addFlow(flow14);
		net.addFlow(flow15);
		net.addFlow(flow16);
		net.addFlow(flow17);
		/**/
		scheduleGenerator.generateSchedule(net);

	}
}

