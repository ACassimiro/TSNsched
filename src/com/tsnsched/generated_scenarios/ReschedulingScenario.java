package com.tsnsched.generated_scenarios;

import java.util.*;
import java.io.*;

import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;
import com.tsnsched.core.nodes.*;

public class ReschedulingScenario {
	public void runTestCase(){
		Boolean loadNetwork = true;
		
		ScheduleGenerator scheduleGenerator = new ScheduleGenerator(loadNetwork);
		Network net = new Network();
		
		if(loadNetwork) {
			

			/*
			String path = "network.ser";
			net = scheduleGenerator.deserializeNetwork(path);
			
			TSNSwitch swt = (TSNSwitch) net.getSwitch("switch0");
			Port port = swt.getPortOf("switch1");
			net.modifyElement(port, NetworkProperties.PORTSPEED, 150);
			//System.out.println("\n\n" + port.getName());
			
			long startLoadTime = System.nanoTime();
			scheduleGenerator.generateSchedule(net);
			long endLoadTime   = System.nanoTime();
			long totalLoadTime = endLoadTime - startLoadTime;
			
			
			System.out.println("\n\nExecution time: " + ((float) totalLoadTime)/1000000000 + " seconds\n ");
			/**/
			
			/*
						
			String path = "network.ser";
			net = scheduleGenerator.deserializeNetwork(path);
			
			
			Flow flow5 = new Flow(Flow.UNICAST);
			
			flow5.setStartDevice(net.getDevice("dev3"));
			flow5.addToPath((TSNSwitch) net.getSwitch("switch0"));
			flow5.addToPath((TSNSwitch) net.getSwitch("switch1"));
			flow5.setEndDevice(net.getDevice("dev7"));
			
			net.addElement(flow5, NetworkProperties.ADDFLOW);
			
			
			long startLoadTime = System.nanoTime();
			scheduleGenerator.generateSchedule(net);
			long endLoadTime   = System.nanoTime();
			long totalLoadTime = endLoadTime - startLoadTime;
			
			/**/
			
			/**/
			String path = "network.ser";
			net = scheduleGenerator.deserializeNetwork(path);
			
			
			Flow flow5 = new Flow(Flow.UNICAST);
			flow5.setStartDevice(net.getDevice("dev3"));
			flow5.addToPath((TSNSwitch) net.getSwitch("switch0"));
			flow5.addToPath((TSNSwitch) net.getSwitch("switch1"));
			flow5.setEndDevice(net.getDevice("dev7"));
 
 			net.addElement(flow5, NetworkProperties.INCREMENTFLOW);
			
			
			long startLoadTime = System.nanoTime();
			scheduleGenerator.generateSchedule(net);
			long endLoadTime   = System.nanoTime();
			long totalLoadTime = endLoadTime - startLoadTime;
			
			/**/
			
			
			return;
		}
		
		
		/* 
		* GENERATING DEVICES
		*/
		Device dev0 = new  Device(500, 0, 1000, 1625);
		Device dev1 = new  Device(500, 0, 1000, 1625);
		Device dev2 = new  Device(500, 0, 1000, 1625);
		Device dev3 = new  Device(500, 0, 1000, 1625);
		Device dev4 = new  Device(500, 0, 1000, 1625);
		Device dev5 = new  Device(500, 0, 1000, 1625);
		Device dev6 = new  Device(500, 0, 1000, 1625);
		Device dev7 = new  Device(500, 0, 1000, 1625);
		Device dev8 = new  Device(500, 0, 1000, 1625);
		Device dev9 = new  Device(500, 0, 1000, 1625);
		Device dev10 = new Device(500, 0, 1000, 1625);
		Device dev11 = new Device(500, 0, 1000, 1625);
		Device dev12 = new Device(500, 0, 1000, 1625);
		Device dev13 = new Device(500, 0, 1000, 1625);
		Device dev14 = new Device(500, 0, 1000, 1625);
		Device dev15 = new Device(500, 0, 1000, 1625);
		Device dev16 = new Device(500, 0, 1000, 1625);
		Device dev17 = new Device(500, 0, 1000, 1625);
		Device dev18 = new Device(500, 0, 1000, 1625);
		Device dev19 = new Device(500, 0, 1000, 1625);
		Device dev20 = new Device(500, 0, 1000, 1625);
		Device dev21 = new Device(500, 0, 1000, 1625);
		Device dev22 = new Device(500, 0, 1000, 1625);
		Device dev23 = new Device(500, 0, 1000, 1625);
		Device dev24 = new Device(500, 0, 1000, 1625);
		Device dev25 = new Device(500, 0, 1000, 1625);
		Device dev26 = new Device(500, 0, 1000, 1625);
		Device dev27 = new Device(500, 0, 1000, 1625);
		Device dev28 = new Device(500, 0, 1000, 1625);
		Device dev29 = new Device(500, 0, 1000, 1625);
		Device dev30 = new Device(500, 0, 1000, 1625);
		Device dev31 = new Device(500, 0, 1000, 1625);
		Device dev32 = new Device(500, 0, 1000, 1625);
		Device dev33 = new Device(500, 0, 1000, 1625);
		Device dev34 = new Device(500, 0, 1000, 1625);
		Device dev35 = new Device(500, 0, 1000, 1625);
		Device dev36 = new Device(500, 0, 1000, 1625);
		Device dev37 = new Device(500, 0, 1000, 1625);
		Device dev38 = new Device(500, 0, 1000, 1625);
		Device dev39 = new Device(500, 0, 1000, 1625);
		Device dev40 = new Device(500, 0, 1000, 1625);
		Device dev41 = new Device(500, 0, 1000, 1625);
		Device dev42 = new Device(500, 0, 1000, 1625);
		Device dev43 = new Device(500, 0, 1000, 1625);
		Device dev44 = new Device(500, 0, 1000, 1625);
		Device dev45 = new Device(500, 0, 1000, 1625);
		Device dev46 = new Device(500, 0, 1000, 1625);
		Device dev47 = new Device(500, 0, 1000, 1625);
		Device dev48 = new Device(500, 0, 1000, 1625);
		Device dev49 = new Device(500, 0, 1000, 1625);
		Device dev50 = new Device(500, 0, 1000, 1625);
		Device dev51 = new Device(500, 0, 1000, 1625);
		Device dev52 = new Device(500, 0, 1000, 1625);
		Device dev53 = new Device(500, 0, 1000, 1625);
		Device dev54 = new Device(500, 0, 1000, 1625);
		Device dev55 = new Device(500, 0, 1000, 1625);
		Device dev56 = new Device(500, 0, 1000, 1625);
		Device dev57 = new Device(500, 0, 1000, 1625);
		Device dev58 = new Device(500, 0, 1000, 1625);
		Device dev59 = new Device(500, 0, 1000, 1625);
		Device dev60 = new Device(500, 0, 1000, 1625);
		Device dev61 = new Device(500, 0, 1000, 1625);
		Device dev62 = new Device(500, 0, 1000, 1625);
		Device dev63 = new Device(500, 0, 1000, 1625);
		Device dev64 = new Device(500, 0, 1000, 1625);
		Device dev65 = new Device(500, 0, 1000, 1625);
		Device dev66 = new Device(500, 0, 1000, 1625);
		Device dev67 = new Device(500, 0, 1000, 1625);
		Device dev68 = new Device(500, 0, 1000, 1625);
		Device dev69 = new Device(500, 0, 1000, 1625);
		Device dev70 = new Device(500, 0, 1000, 1625);
		Device dev71 = new Device(500, 0, 1000, 1625);
		Device dev72 = new Device(500, 0, 1000, 1625);
		Device dev73 = new Device(500, 0, 1000, 1625);
		Device dev74 = new Device(500, 0, 1000, 1625);
		Device dev75 = new Device(500, 0, 1000, 1625);
		Device dev76 = new Device(500, 0, 1000, 1625);
		Device dev77 = new Device(500, 0, 1000, 1625);
		Device dev78 = new Device(500, 0, 1000, 1625);
		Device dev79 = new Device(500, 0, 1000, 1625);
		Device dev80 = new Device(500, 0, 1000, 1625);
		Device dev81 = new Device(500, 0, 1000, 1625);
		Device dev82 = new Device(500, 0, 1000, 1625);
		Device dev83 = new Device(500, 0, 1000, 1625);
		Device dev84 = new Device(500, 0, 1000, 1625);
		Device dev85 = new Device(500, 0, 1000, 1625);
		Device dev86 = new Device(500, 0, 1000, 1625);
		Device dev87 = new Device(500, 0, 1000, 1625);
		Device dev88 = new Device(500, 0, 1000, 1625);
		Device dev89 = new Device(500, 0, 1000, 1625);
		Device dev90 = new Device(500, 0, 1000, 1625);
		Device dev91 = new Device(500, 0, 1000, 1625);
		Device dev92 = new Device(500, 0, 1000, 1625);
		Device dev93 = new Device(500, 0, 1000, 1625);
		Device dev94 = new Device(500, 0, 1000, 1625);
		Device dev95 = new Device(500, 0, 1000, 1625);
		Device dev96 = new Device(500, 0, 1000, 1625);
		Device dev97 = new Device(500, 0, 1000, 1625);
		Device dev98 = new Device(500, 0, 1000, 1625);
		Device dev99 = new Device(500, 0, 1000, 1625);


		/* 
		* GENERATING SWITCHES
		*/
		TSNSwitch switch0 = new TSNSwitch("switch0", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch1 = new TSNSwitch("switch1", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch2 = new TSNSwitch("switch2", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch3 = new TSNSwitch("switch3", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch4 = new TSNSwitch("switch4", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch5 = new TSNSwitch("switch5", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch6 = new TSNSwitch("switch6", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch7 = new TSNSwitch("switch7", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch8 = new TSNSwitch("switch8", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch9 = new TSNSwitch("switch9", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch10 = new TSNSwitch("switch10", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch11 = new TSNSwitch("switch11", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch12 = new TSNSwitch("switch12", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch13 = new TSNSwitch("switch13", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch14 = new TSNSwitch("switch14", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch15 = new TSNSwitch("switch15", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch16 = new TSNSwitch("switch16", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch17 = new TSNSwitch("switch17", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch18 = new TSNSwitch("switch18", 100, 1, 125, 1, 400, 3000);
		TSNSwitch switch19 = new TSNSwitch("switch19", 100, 1, 125, 1, 400, 3000);

		/* 
		* GENERATING PORTS
		*/
		Cycle cycle0 = new Cycle(50); 
		switch0.createPort(switch1, cycle0);
		Cycle cycle1 = new Cycle(50); 
		switch1.createPort(switch0, cycle1);

        Cycle cycle2 = new Cycle(50); 
		switch2.createPort(switch3, cycle2);
		Cycle cycle3 = new Cycle(50); 
		switch3.createPort(switch2, cycle3);

        Cycle cycle4 = new Cycle(50); 
		switch4.createPort(switch5, cycle4);
		Cycle cycle5 = new Cycle(50); 
		switch5.createPort(switch4, cycle5);

        Cycle cycle6 = new Cycle(50); 
		switch6.createPort(switch7, cycle6);
		Cycle cycle7 = new Cycle(50); 
		switch7.createPort(switch6, cycle7);

        Cycle cycle8 = new Cycle(50); 
		switch8.createPort(switch9, cycle8);
		Cycle cycle9 = new Cycle(50); 
		switch9.createPort(switch8, cycle9);
				
        Cycle cycle10 = new Cycle(50); 
		switch10.createPort(switch11, cycle10);
		Cycle cycle11 = new Cycle(50); 
		switch11.createPort(switch10, cycle11);

        Cycle cycle12 = new Cycle(50); 
		switch12.createPort(switch13, cycle12);
		Cycle cycle13 = new Cycle(50); 
		switch13.createPort(switch12, cycle13);

        Cycle cycle14 = new Cycle(50); 
		switch14.createPort(switch15, cycle14);
		Cycle cycle15 = new Cycle(50); 
		switch15.createPort(switch14, cycle15);

        Cycle cycle16 = new Cycle(50); 
		switch16.createPort(switch17, cycle16);
		Cycle cycle17 = new Cycle(50); 
		switch17.createPort(switch16, cycle17);

        Cycle cycle18 = new Cycle(50); 
		switch18.createPort(switch19, cycle18);
		Cycle cycle19 = new Cycle(50); 
		switch19.createPort(switch18, cycle19);
		

		/* 
		* LINKING SWITCHES TO DEVICES 
		*/
		Cycle cycle380 = new Cycle(50); 
		switch0.createPort(dev0, cycle380);
		Cycle cycle381 = new Cycle(50); 
		switch0.createPort(dev1, cycle381);
		Cycle cycle382 = new Cycle(50); 
		switch0.createPort(dev2, cycle382);
		Cycle cycle383 = new Cycle(50); 
		switch0.createPort(dev3, cycle383);
		Cycle cycle384 = new Cycle(50); 
		switch0.createPort(dev4, cycle384);
		Cycle cycle385 = new Cycle(50); 
		switch1.createPort(dev5, cycle385);
		Cycle cycle386 = new Cycle(50); 
		switch1.createPort(dev6, cycle386);
		Cycle cycle387 = new Cycle(50); 
		switch1.createPort(dev7, cycle387);
		Cycle cycle388 = new Cycle(50); 
		switch1.createPort(dev8, cycle388);
		Cycle cycle389 = new Cycle(50); 
		switch1.createPort(dev9, cycle389);
		Cycle cycle390 = new Cycle(50); 
		switch2.createPort(dev10, cycle390);
		Cycle cycle391 = new Cycle(50); 
		switch2.createPort(dev11, cycle391);
		Cycle cycle392 = new Cycle(50); 
		switch2.createPort(dev12, cycle392);
		Cycle cycle393 = new Cycle(50); 
		switch2.createPort(dev13, cycle393);
		Cycle cycle394 = new Cycle(50); 
		switch2.createPort(dev14, cycle394);
		Cycle cycle395 = new Cycle(50); 
		switch3.createPort(dev15, cycle395);
		Cycle cycle396 = new Cycle(50); 
		switch3.createPort(dev16, cycle396);
		Cycle cycle397 = new Cycle(50); 
		switch3.createPort(dev17, cycle397);
		Cycle cycle398 = new Cycle(50); 
		switch3.createPort(dev18, cycle398);
		Cycle cycle399 = new Cycle(50); 
		switch3.createPort(dev19, cycle399);
		Cycle cycle400 = new Cycle(50); 
		switch4.createPort(dev20, cycle400);
		Cycle cycle401 = new Cycle(50); 
		switch4.createPort(dev21, cycle401);
		Cycle cycle402 = new Cycle(50); 
		switch4.createPort(dev22, cycle402);
		Cycle cycle403 = new Cycle(50); 
		switch4.createPort(dev23, cycle403);
		Cycle cycle404 = new Cycle(50); 
		switch4.createPort(dev24, cycle404);
		Cycle cycle405 = new Cycle(50); 
		switch5.createPort(dev25, cycle405);
		Cycle cycle406 = new Cycle(50); 
		switch5.createPort(dev26, cycle406);
		Cycle cycle407 = new Cycle(50); 
		switch5.createPort(dev27, cycle407);
		Cycle cycle408 = new Cycle(50); 
		switch5.createPort(dev28, cycle408);
		Cycle cycle409 = new Cycle(50); 
		switch5.createPort(dev29, cycle409);
		Cycle cycle410 = new Cycle(50); 
		switch6.createPort(dev30, cycle410);
		Cycle cycle411 = new Cycle(50); 
		switch6.createPort(dev31, cycle411);
		Cycle cycle412 = new Cycle(50); 
		switch6.createPort(dev32, cycle412);
		Cycle cycle413 = new Cycle(50); 
		switch6.createPort(dev33, cycle413);
		Cycle cycle414 = new Cycle(50); 
		switch6.createPort(dev34, cycle414);
		Cycle cycle415 = new Cycle(50); 
		switch7.createPort(dev35, cycle415);
		Cycle cycle416 = new Cycle(50); 
		switch7.createPort(dev36, cycle416);
		Cycle cycle417 = new Cycle(50); 
		switch7.createPort(dev37, cycle417);
		Cycle cycle418 = new Cycle(50); 
		switch7.createPort(dev38, cycle418);
		Cycle cycle419 = new Cycle(50); 
		switch7.createPort(dev39, cycle419);
		Cycle cycle420 = new Cycle(50); 
		switch8.createPort(dev40, cycle420);
		Cycle cycle421 = new Cycle(50); 
		switch8.createPort(dev41, cycle421);
		Cycle cycle422 = new Cycle(50); 
		switch8.createPort(dev42, cycle422);
		Cycle cycle423 = new Cycle(50); 
		switch8.createPort(dev43, cycle423);
		Cycle cycle424 = new Cycle(50); 
		switch8.createPort(dev44, cycle424);
		Cycle cycle425 = new Cycle(50); 
		switch9.createPort(dev45, cycle425);
		Cycle cycle426 = new Cycle(50); 
		switch9.createPort(dev46, cycle426);
		Cycle cycle427 = new Cycle(50); 
		switch9.createPort(dev47, cycle427);
		Cycle cycle428 = new Cycle(50); 
		switch9.createPort(dev48, cycle428);
		Cycle cycle429 = new Cycle(50); 
		switch9.createPort(dev49, cycle429);
		Cycle cycle430 = new Cycle(50); 
		switch10.createPort(dev50, cycle430);
		Cycle cycle431 = new Cycle(50); 
		switch10.createPort(dev51, cycle431);
		Cycle cycle432 = new Cycle(50); 
		switch10.createPort(dev52, cycle432);
		Cycle cycle433 = new Cycle(50); 
		switch10.createPort(dev53, cycle433);
		Cycle cycle434 = new Cycle(50); 
		switch10.createPort(dev54, cycle434);
		Cycle cycle435 = new Cycle(50); 
		switch11.createPort(dev55, cycle435);
		Cycle cycle436 = new Cycle(50); 
		switch11.createPort(dev56, cycle436);
		Cycle cycle437 = new Cycle(50); 
		switch11.createPort(dev57, cycle437);
		Cycle cycle438 = new Cycle(50); 
		switch11.createPort(dev58, cycle438);
		Cycle cycle439 = new Cycle(50); 
		switch11.createPort(dev59, cycle439);
		Cycle cycle440 = new Cycle(50); 
		switch12.createPort(dev60, cycle440);
		Cycle cycle441 = new Cycle(50); 
		switch12.createPort(dev61, cycle441);
		Cycle cycle442 = new Cycle(50); 
		switch12.createPort(dev62, cycle442);
		Cycle cycle443 = new Cycle(50); 
		switch12.createPort(dev63, cycle443);
		Cycle cycle444 = new Cycle(50); 
		switch12.createPort(dev64, cycle444);
		Cycle cycle445 = new Cycle(50); 
		switch13.createPort(dev65, cycle445);
		Cycle cycle446 = new Cycle(50); 
		switch13.createPort(dev66, cycle446);
		Cycle cycle447 = new Cycle(50); 
		switch13.createPort(dev67, cycle447);
		Cycle cycle448 = new Cycle(50); 
		switch13.createPort(dev68, cycle448);
		Cycle cycle449 = new Cycle(50); 
		switch13.createPort(dev69, cycle449);
		Cycle cycle450 = new Cycle(50); 
		switch14.createPort(dev70, cycle450);
		Cycle cycle451 = new Cycle(50); 
		switch14.createPort(dev71, cycle451);
		Cycle cycle452 = new Cycle(50); 
		switch14.createPort(dev72, cycle452);
		Cycle cycle453 = new Cycle(50); 
		switch14.createPort(dev73, cycle453);
		Cycle cycle454 = new Cycle(50); 
		switch14.createPort(dev74, cycle454);
		Cycle cycle455 = new Cycle(50); 
		switch15.createPort(dev75, cycle455);
		Cycle cycle456 = new Cycle(50); 
		switch15.createPort(dev76, cycle456);
		Cycle cycle457 = new Cycle(50); 
		switch15.createPort(dev77, cycle457);
		Cycle cycle458 = new Cycle(50); 
		switch15.createPort(dev78, cycle458);
		Cycle cycle459 = new Cycle(50); 
		switch15.createPort(dev79, cycle459);
		Cycle cycle460 = new Cycle(50); 
		switch16.createPort(dev80, cycle460);
		Cycle cycle461 = new Cycle(50); 
		switch16.createPort(dev81, cycle461);
		Cycle cycle462 = new Cycle(50); 
		switch16.createPort(dev82, cycle462);
		Cycle cycle463 = new Cycle(50); 
		switch16.createPort(dev83, cycle463);
		Cycle cycle464 = new Cycle(50); 
		switch16.createPort(dev84, cycle464);
		Cycle cycle465 = new Cycle(50); 
		switch17.createPort(dev85, cycle465);
		Cycle cycle466 = new Cycle(50); 
		switch17.createPort(dev86, cycle466);
		Cycle cycle467 = new Cycle(50); 
		switch17.createPort(dev87, cycle467);
		Cycle cycle468 = new Cycle(50); 
		switch17.createPort(dev88, cycle468);
		Cycle cycle469 = new Cycle(50); 
		switch17.createPort(dev89, cycle469);
		Cycle cycle470 = new Cycle(50); 
		switch18.createPort(dev90, cycle470);
		Cycle cycle471 = new Cycle(50); 
		switch18.createPort(dev91, cycle471);
		Cycle cycle472 = new Cycle(50); 
		switch18.createPort(dev92, cycle472);
		Cycle cycle473 = new Cycle(50); 
		switch18.createPort(dev93, cycle473);
		Cycle cycle474 = new Cycle(50); 
		switch18.createPort(dev94, cycle474);
		Cycle cycle475 = new Cycle(50); 
		switch19.createPort(dev95, cycle475);
		Cycle cycle476 = new Cycle(50); 
		switch19.createPort(dev96, cycle476);
		Cycle cycle477 = new Cycle(50); 
		switch19.createPort(dev97, cycle477);
		Cycle cycle478 = new Cycle(50); 
		switch19.createPort(dev98, cycle478);
		Cycle cycle479 = new Cycle(50); 
		switch19.createPort(dev99, cycle479);


		/* 
		* GENERATING FLOWS
		*/
		LinkedList<PathNode> nodeList;

        //FIRST SET
		Flow flow0 = new Flow(Flow.UNICAST);
		flow0.setStartDevice(dev0);
		flow0.addToPath(switch0);
		flow0.addToPath(switch1);
		flow0.setEndDevice(dev9);

		Flow flow1 = new Flow(Flow.UNICAST);
		flow1.setStartDevice(dev1);
		flow1.addToPath(switch0);
		flow1.addToPath(switch1);
		flow1.setEndDevice(dev9);

		Flow flow2 = new Flow(Flow.UNICAST);
		flow2.setStartDevice(dev2);
		flow2.addToPath(switch0);
		flow2.addToPath(switch1);
		flow2.setEndDevice(dev9);
		
		Flow flow15 = new Flow(Flow.UNICAST);
		flow15.setStartDevice(dev3);
		flow15.addToPath(switch0);
		flow15.addToPath(switch1);
		flow15.setEndDevice(dev9);
		
        //SECOND SET
		Flow flow3 = new Flow(Flow.UNICAST);
		flow3.setStartDevice(dev10);
		flow3.addToPath(switch2);
		flow3.addToPath(switch3);
		flow3.setEndDevice(dev19);
		
		Flow flow4 = new Flow(Flow.UNICAST);
		flow4.setStartDevice(dev11);
		flow4.addToPath(switch2);
		flow4.addToPath(switch3);
		flow4.setEndDevice(dev19);
		
		Flow flow5 = new Flow(Flow.UNICAST);
		flow5.setStartDevice(dev12);
		flow5.addToPath(switch2);
		flow5.addToPath(switch3);
		flow5.setEndDevice(dev19);

        //THIRD SET
		Flow flow6 = new Flow(Flow.UNICAST);
		flow6.setStartDevice(dev20);
		flow6.addToPath(switch4);
		flow6.addToPath(switch5);
		flow6.setEndDevice(dev29);
		
		Flow flow7 = new Flow(Flow.UNICAST);
		flow7.setStartDevice(dev21);
		flow7.addToPath(switch4);
		flow7.addToPath(switch5);
		flow7.setEndDevice(dev29);
		
		Flow flow8 = new Flow(Flow.UNICAST);
		flow8.setStartDevice(dev22);
		flow8.addToPath(switch4);
		flow8.addToPath(switch5);
		flow8.setEndDevice(dev29);

        //FOURTH SET
		Flow flow9 = new Flow(Flow.UNICAST);
		flow9.setStartDevice(dev30);
		flow9.addToPath(switch6);
		flow9.addToPath(switch7);
		flow9.setEndDevice(dev39);
		
		Flow flow10 = new Flow(Flow.UNICAST);
		flow10.setStartDevice(dev31);
		flow10.addToPath(switch6);
		flow10.addToPath(switch7);
		flow10.setEndDevice(dev39);
		
		Flow flow11 = new Flow(Flow.UNICAST);
		flow11.setStartDevice(dev32);
		flow11.addToPath(switch6);
		flow11.addToPath(switch7);
		flow11.setEndDevice(dev39);

        //FITH SET
		Flow flow12 = new Flow(Flow.UNICAST);
		flow12.setStartDevice(dev40);
		flow12.addToPath(switch8);
		flow12.addToPath(switch9);
		flow12.setEndDevice(dev49);
		
		Flow flow13 = new Flow(Flow.UNICAST);
		flow13.setStartDevice(dev41);
		flow13.addToPath(switch8);
		flow13.addToPath(switch9);
		flow13.setEndDevice(dev49);
		
		Flow flow14 = new Flow(Flow.UNICAST);
		flow14.setStartDevice(dev42);
		flow14.addToPath(switch8);
		flow14.addToPath(switch9);
		flow14.setEndDevice(dev49);




		/* 
		* GENERATING THE NETWORK
		*/
		//net = new Network();
		net.addDevice(dev0);
		net.addDevice(dev1);
		net.addDevice(dev2);
		net.addDevice(dev3);
		net.addDevice(dev4);
		net.addDevice(dev7);
		net.addDevice(dev8);
		net.addDevice(dev9);
		net.addDevice(dev10);
		net.addDevice(dev11);
		net.addDevice(dev12);
		net.addDevice(dev13);
		net.addDevice(dev15);
		net.addDevice(dev16);
		net.addDevice(dev17);
		net.addDevice(dev18);
		net.addDevice(dev19);
		net.addDevice(dev20);
		net.addDevice(dev21);
		net.addDevice(dev22);
		net.addDevice(dev23);
		net.addDevice(dev24);
		net.addDevice(dev25);
		net.addDevice(dev26);
		net.addDevice(dev27);
		net.addDevice(dev28);
		net.addDevice(dev29);
		net.addDevice(dev30);
		net.addDevice(dev31);
		net.addDevice(dev32);
		net.addDevice(dev33);
		net.addDevice(dev34);
		net.addDevice(dev35);
		net.addDevice(dev36);
		net.addDevice(dev37);
		net.addDevice(dev38);
		net.addDevice(dev39);
		net.addDevice(dev40);
		net.addDevice(dev41);
		net.addDevice(dev42);
		net.addDevice(dev43);
		net.addDevice(dev44);
		net.addDevice(dev45);
		net.addDevice(dev46);
		net.addDevice(dev47);
		net.addDevice(dev48);
		net.addDevice(dev49);
		net.addDevice(dev50);
		net.addDevice(dev51);
		net.addDevice(dev52);
		net.addDevice(dev53);
		net.addDevice(dev54);
		net.addDevice(dev55);
		net.addDevice(dev56);
		net.addDevice(dev57);
		net.addDevice(dev58);
		net.addDevice(dev59);
		net.addDevice(dev60);
		net.addDevice(dev61);
		net.addDevice(dev62);
		net.addDevice(dev63);
		net.addDevice(dev64);
		net.addDevice(dev67);
		net.addDevice(dev68);
		net.addDevice(dev69);
		net.addDevice(dev70);
		net.addDevice(dev71);
		net.addDevice(dev72);
		net.addDevice(dev73);
		net.addDevice(dev74);
		net.addDevice(dev77);
		net.addDevice(dev78);
		net.addDevice(dev79);
		net.addDevice(dev80);
		net.addDevice(dev81);
		net.addDevice(dev82);
		net.addDevice(dev83);
		net.addDevice(dev84);
		net.addDevice(dev87);
		net.addDevice(dev88);
		net.addDevice(dev89);
		net.addDevice(dev90);
		net.addDevice(dev91);
		net.addDevice(dev92);
		net.addDevice(dev93);
		net.addDevice(dev94);
		net.addDevice(dev97);
		net.addDevice(dev98);
		net.addDevice(dev99);

		net.addSwitch(switch0);
		net.addSwitch(switch1);
		net.addSwitch(switch2);
		net.addSwitch(switch3);
		net.addSwitch(switch4);
		net.addSwitch(switch5);
		net.addSwitch(switch6);
		net.addSwitch(switch7);
		net.addSwitch(switch8);
		net.addSwitch(switch9);
		net.addSwitch(switch10);
		net.addSwitch(switch11);
		net.addSwitch(switch12);
		net.addSwitch(switch13);
		net.addSwitch(switch14);
		net.addSwitch(switch15);
		net.addSwitch(switch16);
		net.addSwitch(switch17);
		net.addSwitch(switch18);
		net.addSwitch(switch19);
		
		
		//SET 1
		net.addFlow(flow0);
		net.addFlow(flow1);
		net.addFlow(flow2);
		//net.addFlow(flow15);
		
		//SET 2
		net.addFlow(flow3);
		net.addFlow(flow4);	
		net.addFlow(flow5);

		//SET 3
//		net.addFlow(flow6);
//		net.addFlow(flow7);
//		net.addFlow(flow8);
		
		//SET 4
//		net.addFlow(flow9);
//		net.addFlow(flow10);
//		net.addFlow(flow11);
		
		//SET 5
//		net.addFlow(flow12);
//		net.addFlow(flow13);
//		net.addFlow(flow14);
		

		scheduleGenerator.generateSchedule(net);
		
	}
}