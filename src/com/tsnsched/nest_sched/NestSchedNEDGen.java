package com.tsnsched.nest_sched;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.tsnsched.core.components.*;
import com.tsnsched.core.network.*;
import com.tsnsched.core.nodes.*;

// Class to generate the Network Description and Device files the for the Nesting Simulator 
// given a TSNSCHED Network object
public class NestSchedNEDGen {
	
	private Network net;
	private static final String CURRENT_DIR = System.getProperty("user.dir");
	
	public NestSchedNEDGen(Network net) {
		this.net = net;
		writeNetworkFile();
		writeDevFile();
	}
	
	
	public void writeNetworkFile() {
		try {
			// Creates the the file "NestSched.ned" in the directory "nestSched"
			PrintWriter out = new PrintWriter(CURRENT_DIR + "/nestSched/NestSched.ned");

			
			// Default parameters for the network description file
			out.println("package nesting.simulations.examples.nestSched;\n");
			
			out.println("import ned.DatarateChannel;");
			out.println("import nesting.node.ethernet.VlanEtherSwitchPreemptable;\n");
			
			out.println("network nestSched");
			out.println("{");
			out.println("\ttypes:");
			out.println("\t\tchannel C extends DatarateChannel");
			out.println("\t\t{");
			// Arbitrary propagation delay
			out.println("\t\t\tdelay = 0ns;");
			// Arbitrary data rate
			out.println("\t\t\tdatarate = 1Gbps;");
			out.println("\t\t}\n");

			out.println("\tsubmodules:");
			for(Switch currentSwitch : net.getSwitches()) {
				if(currentSwitch instanceof TSNSwitch) {
					// Instantiate the devices
					for(String currentDev : ((TSNSwitch) currentSwitch).getConnectsTo()) {
						if(net.getSwitch(currentDev)==null) {
							out.println("\t\t" +currentDev + ": NestSchedDev;");
						}
					}
					
					// Instantiate the switches
					out.println("\n\n");
					out.println("\t\t" + currentSwitch.getName() + ": VlanEtherSwitchPreemptable {");
					out.println("\t\t\tparameters:");
					out.println("\t\t\tgates:");
					out.println("\t\t\t\tethg[" + ((TSNSwitch) currentSwitch).getPorts().size() + "];");
					out.println("\t\t}");
					out.println("\n\n");
				}
			}
			
			
			// Writes the connections between the devices
			out.println("\tconnections:");
			ArrayList<String> aux = new ArrayList<>();
			for(Switch currentSwitch : net.getSwitches()) {				
				if(currentSwitch instanceof TSNSwitch) {
					for(String currentDev : ((TSNSwitch) currentSwitch).getConnectsTo()) {
						int i = ((TSNSwitch) currentSwitch).getPortOf(currentDev).getPortNum();
						if(net.getSwitch(currentDev)!=null) {
							int j = ((TSNSwitch) net.getSwitch(currentDev)).getPortOf(currentSwitch.getName()).getPortNum();
							if(!aux.contains(currentDev+j+currentSwitch.getName()+i) && !aux.contains(currentSwitch.getName()+i+currentDev+j)) {	
								aux.add(currentSwitch.getName()+i+currentDev+j);
								aux.add(currentDev+j+currentSwitch.getName()+i);
								out.println("\t\t" + currentSwitch.getName() + ".ethg[" + i + "] <--> C <--> " 
										+ currentDev + ".ethg[" + j + "];");
							}
						} else {
							out.println("\t\t" + currentSwitch.getName() + ".ethg[" + i + "] <--> C <--> "
										+ currentDev + ".ethg;");
						}
					}					
				}				
				out.println("\n");
			}
			
			out.println("}");
			out.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
	
	
	
	public void writeDevFile() {
		try {
			// Creates the the file "NestSched.ned" in the directory "nestSched"
			PrintWriter out = new PrintWriter(CURRENT_DIR + "/nestSched/NestSchedDev.ned");

			// Default parameters for the device file
			out.println("package nesting.simulations.examples.nestSched;\n");
			
			out.println("import nesting.node.ethernet.VlanEtherHostSched;\n");
			
			out.println("module NestSchedDev extends VlanEtherHostSched {");
			
			// Flow Signals to track the traffic latency
			for(Flow currentFlow : net.getFlows()) {
				out.println("\t@signal[criticalFlowSig" + currentFlow.getInstance() +"](type=inet::Packet);");
				out.println("\t@statistic[criticalFlowSig" + currentFlow.getInstance() + "Received](title=\"criticalFlowSig" +
						currentFlow.getInstance() + "Received\"; source=\"dataAge(criticalFlowSig" +
							currentFlow.getInstance() + ")\"; unit=s; record=histogram,vector; "
									+ "interpolationmode=linear);\n");
				
			}
			
			out.println("}");
			out.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}
}

