// package helloworld;
import java.io.File;  // Import the File class
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ModuleLayer.Controller;
import java.lang.reflect.Array;
import java.util.Scanner;
import java.awt.Desktop; 

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ConverterToJson {
	public static void main(String[] args) throws JSONException, IOException {

		// get file from argument
		File file = new File(args[0]);
		File file_json = new File("./Outputs/output.json");

		Pattern pattern_device = Pattern.compile("\\s(dev\\d*)\\s", Pattern.CASE_INSENSITIVE);
		Pattern pattern_switch = Pattern.compile(".*TSNSwitch(.*)", Pattern.CASE_INSENSITIVE);
		Pattern params = Pattern.compile("new\\sDevice(.*).*", Pattern.CASE_INSENSITIVE);
		Pattern maximumDuration = Pattern.compile("Cycle\\s(cycle.*)new\\sCycle(.*)");
		Pattern pattern_cycle = Pattern.compile("Cycle\\s(cycle.*)\\s");
		Pattern port_pattern = Pattern.compile("(.*)\\.createPort\\((.*),(.*)\\)");
		Pattern flow_pattern = Pattern.compile("Flow\\s(.*)\\s=\\snew\\sFlow(.*)",Pattern.CASE_INSENSITIVE);
		Pattern hop_pattern = Pattern.compile("\\.addChild\\((.*)\\)",Pattern.CASE_INSENSITIVE);
		  
		Scanner sc = new Scanner(file);	// java file to be converted
		
		// Varibles to hold multiple data
		ArrayList<HashMap<String, Object>> device_list = new ArrayList<HashMap<String, Object>>(); // holds the final device list
		ArrayList<HashMap<String, Object>> flow_list = new ArrayList<HashMap<String, Object>>();  // holds the final flow list
		ArrayList<HashMap<String, Object>> switch_list = new ArrayList<HashMap<String, Object>>();  // holds the final switch list
		ArrayList<HashMap<String, Object>> cycle_list = new ArrayList<HashMap<String, Object>>();  // hold the cycle list
		ArrayList<HashMap<String, Object>> port_list = new ArrayList<HashMap<String, Object>>(); // hold the ports list
		ArrayList<HashMap<String, HashMap<String, Object>>> all_ports = new ArrayList<HashMap<String, HashMap<String, Object>>>(); // array of ports the source and end
		ArrayList<PathNode> hops = new ArrayList<PathNode>(); // holds all the hops
		ArrayList<String> roots = new ArrayList<String>(); // holds all the root nodes, each index represents a flow
		HashMap<String, ArrayList<PathNode>> all_flows_hops = new HashMap<String, ArrayList<PathNode>>(); // holds all the flows hops
		HashMap<String, Integer> cycle_map = new HashMap<String, Integer>(); // hold the cycle data
		PathNode pathNode = new PathNode(); // custom class to represent a node
		ArrayList<PathNode> first = new ArrayList<PathNode>(); // holds all the nodes of a given flow to be used when making the hops
		Integer instance = 0; // instance of a port, used after a "eth" to make the name of a port
		int ocurrence = 0; // number of ocurrence of a root node
		int id = 0; // id of a flow
		ArrayList<Integer> intervals = new ArrayList<Integer>(); // array to hold the number of hops for each flow
		int number_of_times = 0; // varible to hold the number of hops of a flow


		while (sc.hasNextLine()){   // get the next line of the file 

			// group of matchers of a given pattern
			String line = sc.nextLine().toString();
			Matcher device_matcher = pattern_device.matcher(line);
			Matcher match_param = params.matcher(line);
			Matcher switch_matcher = pattern_switch.matcher(line);
			Matcher cycle_matcher = pattern_cycle.matcher(line);
			Matcher maximum_matcher = maximumDuration.matcher(line);
			Matcher port_matcher = port_pattern.matcher(line);
			Matcher flow_matcher = flow_pattern.matcher(line);
			Matcher hop_matcher = hop_pattern.matcher(line);
			

			// find a device in the file and add to the list
			if(device_matcher.find() && match_param.find()){
				String name = device_matcher.group(0).toString();
				String param = match_param.group(1);
				String[] params_device = param.split(",",4);
				
				HashMap<String, Object> device_data = new HashMap<>();
				device_data.put("name", name.replace(" ", ""));
				device_data.put("defaultFirstSendingTime", Integer.parseInt(params_device[1].replace(" ", "")));
				device_data.put("defaultPacketPeriodicity", Integer.parseInt(params_device[0].replace("(", "")));
				device_data.put("defaultHardConstraintTime", Integer.parseInt(params_device[2].replace(" ", "")));
				device_data.put("defaultPacketSize", Integer.parseInt(params_device[3].replace(");", "").replace(" ", "")));
				device_list.add(device_data);
			}
			
			// find a switch and add to the list of switches and ports
			if(switch_matcher.find()){
				String sw = switch_matcher.group(1).toString();
				String[] params_switch = sw.split(",",7);

				HashMap<String, Object> switch_data = new HashMap<String, Object>();
				HashMap<String, Object> port_data = new HashMap<String, Object>();

				switch_data.put("name", params_switch[0].replace(");", "").replace("(", "").replace(" ", "").replace("\"", ""));
				switch_data.put("scheduleType", "hypercycle");
				switch_data.put("defaultTimeToTravel", 1);
				switch_data.put("defaultPortSpeed", 125);
				switch_list.add(switch_data);

				port_data.put("name", params_switch[0].replace(");", "").replace("(", "").replace(" ", ""));
				port_data.put("timeToTravel", Integer.parseInt(params_switch[2].replace(" ", "")));
				port_data.put("portSpeed", Integer.parseInt(params_switch[3].replace(" ", "")));
				port_data.put("guardBandSize", Integer.parseInt(params_switch[4].replace(" ", "")));
				port_data.put("cycleStart", 0);				
				port_list.add(port_data);
			}

			// find the maximum cycle duration
			if(maximum_matcher.find()){
				String cycle_max_duration = maximum_matcher.group(2).replace(");", "").replace("(", "").replace(" ", "");
				String cycle = maximum_matcher.group(1).toString().replace("=", "").replace(" ", "");

				cycle_map.put(cycle, Integer.parseInt(cycle_max_duration.replace("=", "").replace(" ", "")));

			}

			// find the ports
			if(port_matcher.find()){
				String port_origin = port_matcher.group(1).toString().replace("\t", "");
				String port_destination = port_matcher.group(2).toString();
				String port_cycle = port_matcher.group(3).toString();

				for(int i = 0; i < switch_list.size(); i++){
					
					if(switch_list.get(i).get("name").toString().replace("\"", "").equals(port_origin.replace("\t", ""))){

						HashMap<String, Object> port_source = new HashMap<String, Object>();
						HashMap<String, HashMap<String, Object>> temp = new HashMap<String, HashMap<String, Object>>();
						port_source.put("name", "eth" + instance);
						port_source.put("connectsTo", port_origin);
						port_source.put("timeToTravel", port_list.get(i).get("timeToTravel"));
						port_source.put("scheduleType", "microcycle");
						port_source.put("maximumSlotDuration", cycle_map.get("cycle"+i));
						port_source.put("cycleStart", 0);

						temp.put("source", port_source);

						
						instance++;
						
						HashMap<String, Object> port_end = new HashMap<String, Object>();
						port_end.put("name", "eth" + instance);
						port_end.put("connectsTo", port_destination);
						port_end.put("timeToTravel", port_list.get(i).get("timeToTravel"));
						port_end.put("portSpeed", port_list.get(i).get("portSpeed"));
						port_end.put("guardBandSize", port_list.get(i).get("guardBandSize"));
						port_end.put("maximumSlotDuration", cycle_map.get("cycle"+i));
						port_end.put("cycleStart", 0);

						temp.put("end", port_end);

						all_ports.add(temp);
					}
				}
				instance++;
			}

			// find the flows and add to a list
			if(flow_matcher.find()){
				String flow_name = flow_matcher.group(1);
				String flow_type = flow_matcher.group(2).toString().replace("(", "").replace(");", "").replace("Flow.", "");

				HashMap<String, Object> flow_data = new HashMap<String, Object>();
				flow_data.put("name", flow_name);
				flow_data.put("type", flow_type);
				flow_data.put("sourceDevice", flow_name);
				flow_data.put("endDevices", new ArrayList<String>());
				flow_list.add(flow_data);
			}

			Pattern root_pattern = Pattern.compile("(\\d*)\\.addRoot\\((.*)\\)",Pattern.CASE_INSENSITIVE);
			Matcher root_matcher = root_pattern.matcher(line);

			// find the root node which is a device of a flow and save it in an array
			if(root_matcher.find()){
				all_flows_hops.put("flow"+ocurrence, hops);
				first.clear();
				intervals.add(number_of_times);
				number_of_times = 0;
				
				String root = root_matcher.group(2);
				id = Integer.parseInt(root_matcher.group(1));
				
				PathNode root_path = new PathNode(root.toString().replace(")", ""), id);
				hops.add(root_path);
				roots.add(root);
			}
			
			Pattern addChild_pattern = Pattern.compile("pathNode(\\d*)\\.addChild\\((.*)\\)");
			Matcher child_matcher = addChild_pattern.matcher(line);
			
			// find the first node of the tree and add to an array of nodes
			if(child_matcher.find()){
				PathNode next = new PathNode(child_matcher.group(2).toString().replace(")", ""), id);
				first.add(next);
				pathNode = first.get(0);
				ocurrence++;
				next.setCurrent_node(child_matcher.group(2).toString().replace(")", ""));
				if(hops.get(0).getId() == next.getId()){
					hops.get(0).setNext_node(next);
				}
				else{
					for(int j = 0; j< hops.size(); j++){
						if(hops.get(j).getId() == next.getId()){
							hops.get(j).setNext_node(next);
						}
					}
				}
				number_of_times++;
			}
			
			Pattern addChild_pattern_2 = Pattern.compile("removeFirst\\(\\)\\.addChild\\((.*)\\)");
			Matcher child_matcher_2 = addChild_pattern_2.matcher(line);
			
			// find the pattern "removeFirst().addChild()" to remove the first node of an array of nodes and add another one
			if(child_matcher_2.find()){
				PathNode next = new PathNode(child_matcher_2.group(1).toString().replace(")", ""), id);
				
				pathNode = first.get(0);
				PathNode new_hop = new PathNode(pathNode.getCurrent_node().toString().replace(")", ""), next);
				first.remove(0);

				hops.add(new_hop);
				first.add(next);
				
				number_of_times++;	
			}
			Pattern removeFirst_pattern = Pattern.compile("nodeList\\.removeFirst\\(\\);");
			Matcher removeFirst_matcher = removeFirst_pattern.matcher(line);

			// remove the first node of the array of nodes
			if(removeFirst_matcher.find()){
				pathNode.getCurrent_node();
				first.remove(0);
			}
			
			Pattern addChild_pattern_3 = Pattern.compile("getFirst\\(\\)\\.addChild\\((.*)\\)");
			Matcher child_matcher_3 = addChild_pattern_3.matcher(line);
			
			// get the first node of the array and add another one
			if(child_matcher_3.find()){
				pathNode = first.get(0);
				PathNode next = new PathNode(child_matcher_3.group(1).replace(")", ""), id);
				PathNode new_hop = new PathNode(pathNode.getCurrent_node().toString().replace(")", ""), next);
				first.add(next);
				hops.add(new_hop);

				number_of_times++;
			}
			
			Pattern addChild_pattern_4 = Pattern.compile("nodeList\\.addChild\\((.*)\\)");
			Matcher child_matcher_4 = addChild_pattern_4.matcher(line);
			
			// add a child to the array of nodes
			if(child_matcher_4.find()){
				PathNode next = new PathNode(child_matcher_4.group(1).replace(")", ""), id);
				System.out.println(next.getCurrent_node());
				hops.get(ocurrence).setNext_node(next);
				number_of_times++;
			
			}		
		} 

		
		// get a complete port and add to the switch
		for(int i = 0; i < switch_list.size(); i++){
			int count = 0;
			ArrayList<HashMap<String, Object>> ports = new ArrayList<HashMap<String, Object>>();
			
			while(all_ports.size() > count){
				
				if(switch_list.get(i).get("name").toString().replace("\"", "").equals(all_ports.get(count).get("source").get("connectsTo"))){
					ports.add(all_ports.get(count).get("source"));
					ports.add(all_ports.get(count).get("end"));
				}
				
				count++;
			}
			switch_list.get(i).put("ports", ports);
		}
		
		int sum = 0;
		int currentflow = 0;
		int controlLoop = 0;
		int size = all_flows_hops.get("flow0").size();
		
		// add the first flow to the list of flows
		if(controlLoop == 0){
			ArrayList<HashMap<String, String>> hops_aux = new ArrayList<HashMap<String, String>>();
			ArrayList<String> end_devices = new ArrayList<String>();
			end_devices.clear();

			for(int k = intervals.get(0); k < intervals.get(1); k++){ //all_flows_hops.get("flow0").size()

				int internal_array_size = all_flows_hops.get("flow0").get(k).getNext_node().size();

				for(int j = 0; j < internal_array_size; j++){

					if(all_flows_hops.get("flow0").get(k).getNext_node().get(j).getCurrent_node().contains("dev") && all_flows_hops.get("flow0").get(k).getNext_node().get(j).getNext_node().size() == 0){
						end_devices.add(all_flows_hops.get("flow0").get(k).getNext_node().get(j).getCurrent_node());
					}

					HashMap<String, String> pair_hop = new HashMap<String, String>();

					String source = all_flows_hops.get("flow0").get(k).getCurrent_node(); 
					String end = all_flows_hops.get("flow0").get(k).getNext_node().get(j).getCurrent_node();
					
					pair_hop.put("currentNodeName", source);
					pair_hop.put("nextNodeName", end);
					hops_aux.add(pair_hop);
				}
			}
			sum = intervals.get(1);
			currentflow++;
			flow_list.get(0).put("hops", hops_aux);
			flow_list.get(0).put("endDevices", end_devices);
			flow_list.get(0).put("sourceDevice", roots.get(0));
			controlLoop++;
		}


		ArrayList<HashMap<String, String>> hops_aux = new ArrayList<HashMap<String, String>>();
		ArrayList<String> end_devices = new ArrayList<String>();
		int threshold = size;
		boolean dev = false;

		// add the remaining flows to the flow list
		for(int k = sum; k < threshold; k++){ //all_flows_hops.get("flow0").size()

			int internal_array_size = all_flows_hops.get("flow0").get(k).getNext_node().size();

			for(int j = 0; j < internal_array_size; j++){

				if(all_flows_hops.get("flow0").get(k).getNext_node().get(j).getCurrent_node().contains("dev") && all_flows_hops.get("flow0").get(k).getNext_node().get(j).getNext_node().size() == 0){
					end_devices.add(all_flows_hops.get("flow0").get(k).getNext_node().get(j).getCurrent_node());
				}

				HashMap<String, String> pair_hop = new HashMap<String, String>();

				String source = all_flows_hops.get("flow0").get(k).getCurrent_node(); 
				String end = all_flows_hops.get("flow0").get(k).getNext_node().get(j).getCurrent_node();
				
				if(source.contains("dev")){
					dev = true;
					if(dev && controlLoop == 1){
						controlLoop++;
						dev = false;
					}
					else if(dev && controlLoop != 1){
		
						flow_list.get(currentflow).put("hops", hops_aux);
						flow_list.get(currentflow).put("endDevices", end_devices);
						flow_list.get(currentflow).put("sourceDevice", roots.get(currentflow));
		
						hops_aux = new ArrayList<HashMap<String, String>>();
						end_devices = new ArrayList<String>();
		
						currentflow++;
						dev = false;
					}else if(currentflow == (flow_list.size() - 1)){
						flow_list.get(currentflow).put("hops", hops_aux);
						flow_list.get(currentflow).put("endDevices", end_devices);
						flow_list.get(currentflow).put("sourceDevice", roots.get(currentflow));
					}
				}
				pair_hop.put("currentNodeName", source);
				pair_hop.put("nextNodeName", end);
				hops_aux.add(pair_hop);
			}
			if(currentflow == (flow_list.size() - 1)){
				flow_list.get(currentflow).put("hops", hops_aux);
				flow_list.get(currentflow).put("endDevices", end_devices);
				flow_list.get(currentflow).put("sourceDevice", roots.get(currentflow));
			}
		}

		// create a HashMap to add the switches, devices and flows
		HashMap<String, Object> json_data = new HashMap<>();
		json_data.put("switches", new JSONArray());
		json_data.put("devices", new JSONArray());
		json_data.put("flows", new JSONArray());
		json_data.put("switches", switch_list);
		json_data.put("devices", device_list);
		json_data.put("flows", flow_list);

		// create a JSON Object and convert the hashMap containing the JSON data to a JSON object
		JSONObject jo = new JSONObject(json_data);
		String teste = jo.toString();

		ObjectMapper mapper = new ObjectMapper();
		JsonFactory factory = new JsonFactory();
		JsonGenerator generator = factory.createGenerator(new File("./Outputs/output.json"), JsonEncoding.UTF8);

		mapper.writeValue(generator, json_data);

		// formating the JSON

		Scanner sc2 = new Scanner(file_json);	// ouput JSON file to be formated 
		String new_file = new String(); 		// output JSON formated

		while(sc2.hasNextLine()){
			String nline =  sc2.nextLine();
			new_file += nline;
		}
		new_file = new_file.replace("{", "{\n\t").replace("[", "[\n\t").replace("}", "\n}").replace(",", ",\n\t").replace("\\", "");

		// writting it to a file
		FileWriter fw = new FileWriter("./Outputs/output.json");    
		fw.write(new_file);    
		fw.close();  

		//closing files
		sc.close();
		sc2.close();
	}

}

// classes

class PathNode extends JSONObject {
	private String current_node;
	private PathNode previous_node;
	private ArrayList<PathNode> next_node = new ArrayList<PathNode>();
	private int id = 0;

	PathNode(){

	}

	PathNode(String current_node){
		this.current_node = current_node;
	}

	PathNode(String current_node,int id){
		this.current_node = current_node;
		this.id = id;
	}

	PathNode(String current_node, PathNode next_node){
		this.current_node = current_node;
		this.next_node.add(next_node);
	}
	
	PathNode(String current_node, PathNode previous_node, PathNode next_node){
		this.current_node = current_node;
		this.previous_node = previous_node;
		this.next_node.add(next_node);
	}

	public String getCurrent_node() {
		return current_node;
	}
	public ArrayList<PathNode> getNext_node() {
		return next_node;
	}
	public PathNode getPrevious_node() {
		return previous_node;
	}
	public void setNext_node(PathNode next_node) {
		this.next_node.add(next_node);
	}
	public void setPrevious_node(PathNode previous_node) {
		this.previous_node = previous_node;
	}
	public void setCurrent_node(String current_node) {
		this.current_node = current_node;
	}
	public int getId() {
		 return id;
	}

	public int getSize(){
		return next_node.size();
	}
}