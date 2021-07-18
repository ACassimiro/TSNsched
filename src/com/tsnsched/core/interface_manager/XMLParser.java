package com.tsnsched.core.interface_manager;

import com.tsnsched.core.nodes.*;
import com.tsnsched.core.components.Cycle;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import com.google.gson.*;

public class XMLParser implements GenericParser {
	String inputFilePath = "";
	String outputFilePath = "";
	String fileContent = "";
	
	private Printer printer;
	
	public XMLParser() {
		;
	}
	
	public XMLParser(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}
	
	public Network parseInput() {
		Network net = null;

		List<String> contentListOfLines; 
		try {
			contentListOfLines = Files.readAllLines(Paths.get(this.inputFilePath), Charset.forName("UTF-8"));
		} catch(Exception e) {
			contentListOfLines = new ArrayList<String>();
		}
		
		for(String line : contentListOfLines) {
			this.fileContent += line;
		}
		
		net = this.parseInputContent(this.fileContent);
		
		return net;
	
	}
	

	public Network parseInputContent(String content) {

		Network net = null;
		String jsonPrettyPrintString = null;
			
		try {
	        JSONObject json = XML.toJSONObject(content); // converts xml to json
	        jsonPrettyPrintString = json.toString(4); // json pretty print
	        jsonPrettyPrintString = jsonPrettyPrintString.replace("{\"network\": ", "");
	        jsonPrettyPrintString = jsonPrettyPrintString.substring(0,jsonPrettyPrintString.length()-1);  

		} catch(JSONException je) {
			;
		}

		JSONParser jsonParser = new JSONParser();
		net = jsonParser.parseInputContent(jsonPrettyPrintString);
		
		return net;
		
	}
	
	public static void main(String []args) {
		
		//JSONParser parser = new JSONParser("");
		
		//double value = parser.convertSizeUnits("1.5 Mb");
		//double value = parser.convertTimeUnits("1.5 ms");
		//double value = parser.convertSpeedUnits("1 Gbit/sec");
		
		//System.out.println(value);
	
		//XMLParser parser = new XMLParser("src/com/tsnsched/generated_scenarios/input.xml");
		XMLParser parser = new XMLParser("/home/acassimiro/Documents/inputEMF.xml");
		
		

		parser.parseInput();
		
		
		//JSONParser parser = new JSONParser(input);
		
		//parser.parseInput();
		//parser.generateOutput();
		
	}
	

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}
}
