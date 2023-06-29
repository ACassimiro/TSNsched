package com.tsnsched.core.interface_manager;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.tsnsched.core.network.Network;

public class ParserManager {
	private String inputFile = "";
	private String fileContent = "";
	private Printer printer;

	private Boolean enablePacketTimeOutput = false;
	
	public ParserManager() {
		;
	}
	
	public ParserManager(String inputFile) {
		this.inputFile = inputFile;
	}
	
	public static char getFirstNonWhitespace(String string){
	    char[] characters = string.toCharArray();
	    
	    for(int i = 0; i < string.length(); i++){
	        if(!Character.isWhitespace(characters[i])){
	            return characters[i];
	        }
	    }
	    return 'n';
	}
	
	public GenericParser getParser(String content) {
		GenericParser parser = null;
		
		switch(this.getFirstNonWhitespace(content)) {
			case '<':
				this.printer.printIfLoggingIsEnabled("Input is XML");
				parser = new XMLParser(this.inputFile);
				break;
			case '{':
				this.printer.printIfLoggingIsEnabled("Input is JSON");
				parser = new JSONParser(this.inputFile);
				break;
			case '[':
				this.printer.printIfLoggingIsEnabled("Input is JSON");
				parser = new JSONParser(this.inputFile);
				break;
			default:
				this.printer.printIfLoggingIsEnabled(this.getFirstNonWhitespace(content) + "");
				this.printer.printIfLoggingIsEnabled("Input not recognized");
				break;		
		}
		
		parser.setPrinter(this.printer);
		
		return parser;
	}
	
	public Network parseFromFile() {
		Network net = null;
		
		if(this.inputFile.isEmpty()) {
			return net;
		}
		
		this.printer.printIfLoggingIsEnabled("Trying to detect type of input.");
		
		List<String> contentListOfLines; 
		try {
			contentListOfLines = Files.readAllLines(Paths.get(this.inputFile), Charset.forName("UTF-8"));
		} catch(Exception e) {
			contentListOfLines = new ArrayList<String>();
		}

		this.fileContent = "";
		for(String line : contentListOfLines) {
			this.fileContent += line;
		}
		
		GenericParser parser = this.getParser(this.fileContent);
		
		net = parser.parseInput();
		
		return net;
	}
	
	public Network parseFromContent(String content) {
		Network net = null;
		
		GenericParser parser = this.getParser(content);
		
		net = parser.parseInputContent(content);
		
		return net;
	}
	
	public void parseOutput(Network net) {
		
		JSONParser parser = new JSONParser();
		parser.setEnablePacketTimeOutput(this.enablePacketTimeOutput);
		parser.generateOutput(net);
		
	}
	
	public static void main(String []args) {
		
		ParserManager parserManager = new ParserManager("src/com/tsnsched/generated_scenarios/input.xml");
		
		parserManager.parseFromFile();
		
	}

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}

	public Boolean getEnablePacketTimeOutput() {
		return enablePacketTimeOutput;
	}

	public void setEnablePacketTimeOutput(Boolean enablePacketTimeOutput) {
		this.enablePacketTimeOutput = enablePacketTimeOutput;
	}

	
}
