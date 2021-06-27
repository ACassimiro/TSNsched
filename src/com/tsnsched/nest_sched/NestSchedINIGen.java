package com.tsnsched.nest_sched;

import java.io.FileNotFoundException;

import java.io.PrintWriter;
import java.util.ArrayList;

import com.tsnsched.core.components.*;
import com.tsnsched.core.network.*;
import com.tsnsched.core.nodes.*;

// Class to generate the Initialization file for the Nesting Simulator given a TSNSCHED Network object
public class NestSchedINIGen {
	
	private Network net;
	private static final String CURRENT_DIR = System.getProperty("user.dir");
	
	public NestSchedINIGen(Network net) {
		this.net = net;
		writeInitializationFile();
	}
	
	
	
	public void writeInitializationFile() {
		try {
			// Creates the the file "NestSched.ini" in the directory "nestSched"
			PrintWriter out = new PrintWriter(CURRENT_DIR + "/nestSched/NestSched.ini");
			
			ArrayList<String> talkers = new ArrayList<String>();
			
			// Default parameters for the initialization file
			out.println("[General]");
			out.println("network = nestSched \n");
			out.println("record-eventlog = false");
			out.println("debug-on-errors = true");
			out.println("result-dir = results_nestSched");
			
			// Arbitrary Limit
			out.println("sim-time-limit = 5s \n");
			
			out.println("# debug");
			out.println("**.displayAddresses = true");
			out.println("**.verbose = true");
			
			
			// Writes the MAC addresses of the devices in the topology
			out.println("#MAC Addresses");
			int counter = 1;
			for(Switch currentSwitch : net.getSwitches()) {
				if(currentSwitch instanceof TSNSwitch) {
					for(String currentDev : ((TSNSwitch) currentSwitch).getConnectsTo()) {
						if(net.getSwitch(currentDev) == null) {
							if(counter < 10) {
								out.println("**." + currentDev + ".eth.address = " + "\"00-00-00-00-00-0" 
										+ counter++ + "\"");
							} else {
								out.println("**." + currentDev + ".eth.address = " + "\"00-00-00-00-00-" 
										+ counter++ + "\"");
							}
						}
					}
				}
			}
			
			// Arbitrary frequency
			out.println("\n**.frequency = 1THz\n");
			
			// Default parameters for the initialization file
			out.println("\n# Switches");
			out.println("**.switch*.processingDelay.delay = " + net.getSwitches().get(0).getTimeToTravel() + "us");
			out.println("**.filteringDatabase.database = xmldoc(\"Routing.xml\", \"/filteringDatabases/\")");
			
			// Instantiate the port schedule for each port used in the topology
			for(Switch currentSw : net.getSwitches()) {
				if(currentSw instanceof TSNSwitch) {
					for(Port currentP : ((TSNSwitch) currentSw).getPorts()) {
						if(!currentP.getFlowFragments().isEmpty()) {
							out.println("**."+ currentSw.getName() +".eth[" + currentP.getPortNum() 
								+ "].queue.gateController.initialSchedule = xmldoc(\"PortScheduling.xml\"" 
										+ ", \"/schedules/switch[@name='" + currentSw.getName() + "']/port[@id='" 
											+ currentP.getPortNum() + "']/schedule\"" + ")");
						}
					}
				}
			}
			
			// Arbitrary parameter to enable packet preemption
			out.println("**.gateController.enableHoldAndRelease = true");
			
			for(int i=0; i<8; i++) {
				out.println("**.switch*.eth[*].queuing.tsAlgorithms["+ i +"].typename = \"StrictPriority\"");
			}
			
			
			out.println("#Traffic Generators");
			for(Flow currentFlow : net.getFlows()) {	
				if(!talkers.contains(currentFlow.getStartDevice().getName())) {
					talkers.add(currentFlow.getStartDevice().getName());
				}
			}
			
			// Instantiate the traffic generation to each device
			for(String currentDev : talkers) {
				out.println("**." + currentDev +".trafGenSchedApp.initialSchedule = xmldoc(\""+ currentDev +".xml\")");
			}
			for(Switch currentSwitch : net.getSwitches()) {
				if(currentSwitch instanceof TSNSwitch) {
					for(String currentDev : ((TSNSwitch) currentSwitch).getConnectsTo()) {
						if(!talkers.contains(currentDev) && net.getSwitch(currentDev)==null){
							out.println("**." + currentDev +".trafGenSchedApp.initialSchedule = xmldoc(\"emptyFlow.xml\")");
						}
					}
				}
			}

			
			out.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
}
