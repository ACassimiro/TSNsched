package com.tsnsched.core.interface_manager;

import com.tsnsched.core.nodes.*;
import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.FlowFragment;
import com.tsnsched.core.components.PathNode;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.*;
import org.json.*;


public class JSONParser implements GenericParser {
	String inputFilePath = "";
	String outputFilePath = "";
	String fileContent = "";
	

	public JSONParser() {
		;
	}
	
	public JSONParser(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}
	
	private Device[] getListOfDevices(JsonObject jsonObject) {
		List<Device> listOfDevices = new ArrayList<Device>();
		
		if (jsonObject.has("devices")) {
			JsonArray devicesDataObject = null;
			
			devicesDataObject = this.getArrayFromElement(jsonObject, "devices");
			
		    if (devicesDataObject  != null) {

	    		for(JsonElement jsonElement : devicesDataObject ) {
	    			
	    			JsonObject deviceObject = jsonElement.getAsJsonObject();
	    			
	    			Device dev = new Device(deviceObject.get("name").getAsString());
	    			
	    			if(deviceObject.has("defaultFirstSendingTime")) {
	    				dev.setFirstT1Time(this.convertTimeUnits(deviceObject.get("defaultFirstSendingTime").getAsString()));
	    			}
	    			if(deviceObject.has("defaultPacketPeriodicity")) {
	    				dev.setPacketPeriodicity(this.convertTimeUnits(deviceObject.get("defaultPacketPeriodicity").getAsString()));
	    			}
	    			if(deviceObject.has("defaultHardConstraintTime")) {
	    				dev.setHardConstraintTime(this.convertTimeUnits(deviceObject.get("defaultHardConstraintTime").getAsString()));
	    			}
	    			if(deviceObject.has("defaultPacketSize")) {
	    				dev.setPacketSize(this.convertSizeUnits(deviceObject.get("defaultPacketSize").getAsString()));
	    			}

	    			listOfDevices.add(dev);
	    			
	    		}
	    		
		    }
		    
		}
		
		Device[] arrayOfDevices = new Device[listOfDevices.size()];
		listOfDevices.toArray(arrayOfDevices);
		return arrayOfDevices;
		
	}
	
	private void setPortsOfSwitch(JsonObject jsonObject, Network net) {
		List<Switch> listOfSwitches = new ArrayList<Switch>();
		
		if (jsonObject.has("switches")) {

		    JsonArray switchesDataObject = this.getArrayFromElement(jsonObject, "switches");
			
		    if (switchesDataObject  != null) {
		    	
	    		for(JsonElement jsonElement : switchesDataObject ) {
	    			
	    			JsonObject switchObject = jsonElement.getAsJsonObject();
	    			TSNSwitch swt = (TSNSwitch) net.getSwitch(switchObject.get("name").getAsString());
	    			
	    			if(switchObject.has("ports")) {

	    			    JsonArray portsDataObject = this.getArrayFromElement(switchObject, "ports");

	    			    if (portsDataObject  != null) {
	    			    	
	    		    		for(JsonElement portJsonElement : portsDataObject ) {

	    		    			Cycle cycle;
	    		    			Port port; 
	    		    			
	    		    			JsonObject portObject = portJsonElement.getAsJsonObject();

	    		    			cycle = new Cycle(this.convertTimeUnits(portObject.get("maximumSlotDuration").getAsString()));
	    		    			
	    		    			port = swt.createPort(portObject.get("connectsTo").getAsString(), cycle);
	    		    			
	    		    			if(portObject.has("portSpeed")) {
	    		    				port.setPortSpeed(this.convertSpeedUnits(portObject.get("portSpeed").getAsString()));
	    		    			}
	    		    			
	    		    			if(portObject.has("timeToTravel")) {
	    		    				port.setTimeToTravel(this.convertTimeUnits(portObject.get("timeToTravel").getAsString()));
	    		    			}
	    		    			
	    		    			if(portObject.has("guardBandSize")) {
	    		    				port.setGbSize(this.convertTimeUnits(portObject.get("guardBandSize").getAsString()));
	    		    			}
	    		    			
	    		    			if(portObject.has("cycleStart")) {
	    		    				port.setCycleStart(portObject.get("cycleStart").getAsDouble());
	    		    			}
	    		    			
	    		    			if(portObject.has("scheduleType")) {
	    		    				String scheduleType = portObject.get("scheduleType").getAsString().toLowerCase();
	    		    				
	    		    				switch(scheduleType) {
	    		    					case "hypercycle":
	    		    						port.clearScheduleType();
	    		    		        		port.setUseHyperCycle(true);
	    		    						break;
	    		    					case "microcycle":
	    		    						port.clearScheduleType();
	    		    		        		port.setUseMicroCycles(true);
	    		    						break;
	    		    					default:
	    		    						System.out.println("[ALERT] Schedule type for port " + portObject.get("name").getAsString() + " not recognized");
	    		    				}
	    		    			}
	    		    				    		    			
	    		    		}
	    		    		
	    			    }
	    			    
	    			}
	    				    			
	    		}
		        
		    } 
		    
		} 
		
	}
	
	public JsonArray getArrayFromElement(JsonObject object, String objectName) {
		JsonArray jsonArray = null;
		JsonObject auxObject = null;
		
		
		if(object.get(objectName) instanceof JsonObject) {
			//jsonArray = (JsonArray) ((JsonObject) object.get(objectName)).get("element");
			
			if(! (((JsonObject) object.get(objectName)).get("element") instanceof JsonArray)) {
				jsonArray = new JsonArray();
				jsonArray.add(((JsonObject) object.get(objectName)).get("element"));
			} else {
				jsonArray = (JsonArray) ((JsonObject) object.get(objectName)).get("element") ;
			}
			
		} else {
			jsonArray = (JsonArray) object.get(objectName);
		}
		
		return jsonArray;
	}
	
		
	private Switch[] getListOfSwitches(JsonObject jsonObject) {
		List<Switch> listOfSwitches = new ArrayList<Switch>();
		
		if (jsonObject.has("switches")) {
			JsonArray switchesDataObject = null;
			
			switchesDataObject = this.getArrayFromElement(jsonObject, "switches");
			
			
		    if (switchesDataObject  != null) {
		    	
	    		for(JsonElement jsonElement : switchesDataObject ) {
	    			
	    			JsonObject switchObject = jsonElement.getAsJsonObject();
	    			
	    			TSNSwitch swt = new TSNSwitch(switchObject.get("name").getAsString());
	    			
	    			if(switchObject.has("defaultTimeToTravel")) {
	    				swt.setTimeToTravel(this.convertTimeUnits(switchObject.get("defaultTimeToTravel").getAsString()));
	    			}
	    			
	    			if(switchObject.has("defaultGuardBandSize")) {
	    				swt.setGbSize(this.convertTimeUnits(switchObject.get("defaultGuardBandSize").getAsString()));
	    			}
	    				    			
	    			if(switchObject.has("defaultPortSpeed")) {
	    				swt.setPortSpeed(this.convertSpeedUnits(switchObject.get("defaultPortSpeed").getAsString()));
	    			}
	    			if(switchObject.has("defaultScheduleType")) {
	    				String scheduleType = switchObject.get("defaultScheduleType").getAsString().toLowerCase();
	    				
	    				switch(scheduleType) {
	    					case "hypercycle":
	    						swt.setScheduleType(ScheduleType.HYPERCYCLES);
	    						break;
	    					case "microcycle":
	    						swt.setScheduleType(ScheduleType.MICROCYCLES);
	    						break;
	    					default:
	    						System.out.println("[ALERT] Schedule type for switch " + switchObject.get("name").getAsString() + " not recognized");
	    				}
	    			}

	    			listOfSwitches.add(swt);
	    			
	    		}
		        
		    } 
		    
		} 
		
		Switch[] arrayOfSwitches = new Switch[listOfSwitches.size()];
		listOfSwitches.toArray(arrayOfSwitches);
		return arrayOfSwitches;
		
	}
	
	private Flow[] getListOfFlows(JsonObject jsonObject, Network net) {
		List<Flow> listOfFlows = new ArrayList<Flow>();
		
		if (jsonObject.has("flows")) {

		    JsonArray flowsDataObject = this.getArrayFromElement(jsonObject, "flows");
			
		    if (flowsDataObject  != null) {
		    	
	    		for(JsonElement jsonElement : flowsDataObject ) {

	    			JsonObject flowObject = jsonElement.getAsJsonObject();
	    			Flow flow;
	    			
	    			flow = new Flow(flowObject.get("name").getAsString(), Flow.PUBLISH_SUBSCRIBE);
	    			
	    			if(flowObject.has("packetPeriodicity")) {
	    				flow.setFlowSendingPeriodicity(this.convertTimeUnits(flowObject.get("packetPeriodicity").getAsString()));
	    			}
	    			
	    			if(flowObject.has("firstSendingTime")) {
	    				flow.setFlowFirstSendingTime(this.convertTimeUnits(flowObject.get("firstSendingTime").getAsString()));
	    			}
	    			
	    			if(flowObject.has("fixedPriority")) {
	    				flow.setFixedPriority(flowObject.get("fixedPriority").getAsBoolean());
	    			}
	    			
	    			if(flowObject.has("priority")) {
	    				flow.setPriorityValue(flowObject.get("priority").getAsInt());
	    			}
	    			
	    			if(flowObject.has("sourceDevice")) {
	    				flow.setStartDevice(net.getDevice(flowObject.get("sourceDevice").getAsString()));
	    			}
	    			

	    			if(flowObject.has("maximumJitter")) {
	    				flow.setFlowMaximumJitter(this.convertTimeUnits(flowObject.get("maximumJitter").getAsString()));
	    			}
	    			
	    			if(flowObject.has("hardConstraintTime")) {
	    				flow.setFlowMaximumLatency(this.convertTimeUnits(flowObject.get("hardConstraintTime").getAsString()));
	    			}
	    			
	    			if(flowObject.has("endDevices")) {

	    			    JsonArray endDevicesDataObject = this.getArrayFromElement(flowObject, "endDevices");

	    			    if (endDevicesDataObject  != null) {
	    			    	
	    			    	for(JsonElement endDevicesJsonElement : endDevicesDataObject ) {
	    			    		
	    			    		flow.setEndDevice(net.getDevice(endDevicesJsonElement.getAsString()));
	    		    			
	    			    	}
	    			
	    			    }
	    			
	    			}
	    			
	    			if(flowObject.has("hops")) {

	    			    JsonArray hopsDataObject = this.getArrayFromElement(flowObject, "hops");

	    			    if (hopsDataObject  != null) {
	    			    	for(JsonElement hopJsonElement : hopsDataObject ) {
	    		    			
	    		    			JsonObject hopObject = hopJsonElement.getAsJsonObject();
	    		    			
	    		    			Object source = net.getDevice(hopObject.get("currentNodeName").getAsString()) == null ?
	    		    					net.getSwitch(hopObject.get("currentNodeName").getAsString()) :
	    		    					net.getDevice(hopObject.get("currentNodeName").getAsString());
	    		    			Object destination = net.getDevice(hopObject.get("nextNodeName").getAsString()) == null ?
	    		    					net.getSwitch(hopObject.get("nextNodeName").getAsString()) :
	    		    					net.getDevice(hopObject.get("nextNodeName").getAsString());
	    		    			
	    		    			flow.addToPath(source, destination);
	    		    			
	    			    	}
	    			    	
	    			    }
	    			
	    			}
	    			
	    			listOfFlows.add(flow);
	    			
	    		}
	    		
		    }
		    
		}
		
		Flow[] arrayOfFlows = new Flow[listOfFlows.size()];
		listOfFlows.toArray(arrayOfFlows);
		return arrayOfFlows;
		
	}

	public Network parseInput() {

		System.out.println("Trying to deserialize");
		List<String> contentListOfLines; 
		try {
			contentListOfLines = Files.readAllLines(Paths.get(this.inputFilePath), Charset.forName("UTF-8"));
		} catch(Exception e) {
			contentListOfLines = new ArrayList<String>();
		}
		
		for(String line : contentListOfLines) {
			this.fileContent += line;
		}
		
		return this.parseInputContent(fileContent);
	}
	
	public Network parseInputContent(String content) {
		
		JsonObject networkJson = new Gson().fromJson(content, JsonObject.class);
		
		Network net = new Network();
		
		for(Switch swt : this.getListOfSwitches(networkJson)) {
			net.addSwitch(swt);
		}
		
		for(Device dev : this.getListOfDevices(networkJson)) {
			net.addDevice(dev);
		}
		
		this.setPortsOfSwitch(networkJson, net);
		
		for(Flow flw : this.getListOfFlows(networkJson, net)) {
			net.addFlow(flw);
		}
		
		System.out.println("Deserialization succedeed");
		
		return net;
		
	}
	
	
	public double convertSpeedUnits(String value) {
		double valueDouble = -1;
		
		String valueString = value.replaceAll("[123456789., ]", "");
		
		value = value.replaceAll("[^ .0-9]", "");
		
		valueDouble = Double.parseDouble(value);
		
		if(valueString.contains("B") || valueString.contains("Byte") || valueString.contains("byte")) {
			;
		} else if(valueString.contains("b") || valueString.contains("bit") || valueString.contains("Bit")) {
			valueDouble = valueDouble/8;
		} 

		valueString = valueString.toLowerCase();

		if(valueString.startsWith("kilo") || valueString.startsWith("k")) {
			valueDouble *= 1000;
		} else if(valueString.startsWith("mega") || valueString.startsWith("m")) {
			valueDouble *= 1000*1000;
		} else if(valueString.startsWith("giga") || valueString.startsWith("g")) {
			valueDouble *= 1000*1000*1000;
		}
		
		if(valueString.endsWith("hour") || valueString.endsWith("h")) {
			valueDouble /= 1000*1000*60*60;
		} else if(valueString.endsWith("minute") || valueString.endsWith("m") || valueString.endsWith("min")) {
			valueDouble /= 1000*1000*60;
		} else if(valueString.endsWith("millisecond") || valueString.endsWith("ms")) {
			valueDouble /= 1000;
		} else if(valueString.endsWith("microsecond") || valueString.endsWith("us")) {
			;
		} else if(valueString.endsWith("nanosecond") || valueString.endsWith("ns")) {
			valueDouble *= 1000;
		} else if(valueString.endsWith("second") || valueString.endsWith("s") || valueString.endsWith("sec")) {
			valueDouble /= 1000*1000;
		}
		
		
		return valueDouble;
	}
	
	
	public double convertSizeUnits(String value) {
		
		double valueDouble = -1;
		
		String valueString = value.replaceAll("[123456789., ]", "");
		
		value = value.replaceAll("[^ .0-9]", "");

		valueDouble = Double.parseDouble(value);
		
		if(valueString.contains("B")) { //Byte
			valueString = valueString.toLowerCase();

			if(valueString.contains("k")) {
				valueDouble*=1000;
			} else if (valueString.contains("m")) {
				valueDouble*=(1000*1000);				
			} 
			
		} else if (valueString.contains("b")) { // bit
			valueString = valueString.toLowerCase();
			
			valueDouble = valueDouble/8;
			
			if(valueString.contains("k")) {
				valueDouble*=1000;
			} else if (valueString.contains("m")) {
				valueDouble*=(1000*1000);				
			} 
			
		}
		
		
		return valueDouble;
		
	}
	
	public double convertTimeUnits(String value) {
		
		double valueDouble = -1;
		
		String valueString = value.replaceAll("[123456789., ]", "");
		valueString = valueString.toLowerCase();
				
		value = value.replaceAll("[^ .0-9]", "");
		
		valueDouble = Double.parseDouble(value);
		
		if(valueString.equals("s") || valueString.equals("second") || valueString.equals("seconds")) {
			valueDouble *= 1000000;
		} else if (valueString.equals("ms") || valueString.contains("millisecond")) {
			valueDouble *= 1000;
		} else if (valueString.equals("us") || valueString.contains("microsecond")) {
			;
		}
					
		return valueDouble;
	}
	
	
	public static void main(String []args) {
		
		//JSONParser parser = new JSONParser("");
		
		//double value = parser.convertSizeUnits("1.5 Mb");
		//double value = parser.convertTimeUnits("1.5 ms");
		//double value = parser.convertSpeedUnits("1 Gbit/sec");
		
		//System.out.println(value);
		
		String input = "src/com/tsnsched/generated_scenarios/input.json";

		JSONParser parser = new JSONParser(input);
		
		parser.parseInput();
		//parser.generateOutput();
		
	}
	
	
	
	public ArrayList<Map<String, Object>> extractSwitchInfoList(Network net) {

		ArrayList<Map<String, Object>> switchInfoList = new ArrayList<Map<String, Object>>();
		
		for(Switch swt : net.getSwitches()) {
			
			TSNSwitch tsnswt = (TSNSwitch) swt;
			
			Map<String, Object> switchInfo = new HashMap<>();

			ArrayList<Map<String, Object>> portInfoList = new ArrayList<Map<String, Object>>();
			
			switchInfo.put("name", swt.getName());
			
			for(Port port : tsnswt.getPorts()) {
				
				Cycle cyc = port.getCycle();
				
				Map<String, Object> portInfo = new HashMap<>();
				
				portInfo.put("name", port.getName());

				ArrayList<Map<String, Object>> prtInfoList = new ArrayList<Map<String, Object>>();
				
				for(int prt : cyc.getSlotsUsed()) {

					Map<String, Object> prtInfo = new HashMap<>();
					
					ArrayList<Map<String, Object>> slotInfoList = new ArrayList<Map<String, Object>>();
					
					for(int i = 0; i<cyc.getSlotStartList(prt).size(); i++) {

						Map<String, Object> slotInfo = new HashMap<>();
						
						if(cyc.getSlotDuration(prt, i) > 0) {
							slotInfo.put("slotStart", cyc.getSlotStart(prt, i));
							slotInfo.put("slotDuration", cyc.getSlotDuration(prt, i));
							slotInfoList.add(slotInfo);
						}
						
					}			
					
					prtInfo.put("slotsData", slotInfoList);
					prtInfo.put("priority", prt);
					prtInfoList.add(prtInfo);
					
				}	

				portInfo.put("firstCycleStart", port.getCycle().getFirstCycleStart());
				portInfo.put("cycleDuration", port.getCycle().getCycleDuration());
				portInfo.put("prioritySlotsData", prtInfoList);
				
				portInfoList.add(portInfo);
				
			}
			
			switchInfo.put("ports", portInfoList);

			switchInfoList.add(switchInfo);
			
		}

		return switchInfoList;
	}
	
	private void recursiveHopInfoGathering(PathNode node, ArrayList<Map<String, Object>> flowHopInfoList) {
		
		if(node.getFlowFragments() == null)
			return;
		
		for(FlowFragment frag : node.getFlowFragments()) {
			Map<String, Object> flowHopInfo = new HashMap<>();
			flowHopInfo.put("currentNodeName", frag.getNodeName());
			flowHopInfo.put("nextNodeName", frag.getNextHop());
			flowHopInfo.put("priority", frag.getFragmentPriority());
			flowHopInfoList.add(flowHopInfo);
		}
		
		for(PathNode childNode : node.getChildren()) {
			this.recursiveHopInfoGathering(childNode, flowHopInfoList);
		}
		
	}
	
	public ArrayList<Map<String, Object>> extractFlowInfoList(Network net) {

		ArrayList<Map<String, Object>> flowInfoList = new ArrayList<Map<String, Object>>();
		
		for(Flow flow : net.getFlows()) {

			Map<String, Object> flowInfo = new HashMap<>();
			
			flowInfo.put("name", flow.getName());
			flowInfo.put("firstSendingTime", flow.getFlowFirstSendingTime());
			flowInfo.put("averageLatency", flow.getAverageLatency());			
			flowInfo.put("jitter", flow.getAverageJitter());
			
			if(flow.isFixedPriority()) {
				flowInfo.put("flowPriority", flow.getPriorityValue());
			} else {
				PathNode node = flow.getPathTree().getRoot();
				node = node.getChildren().get(0); // First 
				ArrayList<Map<String, Object>> flowHopInfoList = new ArrayList<Map<String, Object>>();
				this.recursiveHopInfoGathering(node, flowHopInfoList);
				flowInfo.put("hops", flowHopInfoList);	
			}
			
			flowInfoList.add(flowInfo);
		}
		
		return flowInfoList;

	}
	
	public void generateOutput(Network net) {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();;
		String outputPath = "./output.json";
		
		JsonObject networkObject = new JsonObject();
		JsonArray switchList = new JsonArray();
		

		Map<String, Object> networkInfo = new HashMap<>();
				
		networkInfo.put("switches", this.extractSwitchInfoList(net));
		networkInfo.put("flows", this.extractFlowInfoList(net));

	    
		try {
		    Writer writer = new FileWriter(outputPath);
			gson.toJson(networkInfo, writer);
			writer.close();
		} catch (JsonIOException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
		
		
}
