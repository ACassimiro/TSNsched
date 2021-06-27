package com.tsnsched.nest_sched;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.tsnsched.core.components.*;
import com.tsnsched.core.network.*;
import com.tsnsched.core.nodes.*;

public class NestSchedXMLGen {
    private static final Logger logger = Logger.getLogger(NestSchedXMLGen.class.getName());
    private Document doc;
    private Network net;

    private static final String CURRENT_DIR = System.getProperty("user.dir");
    private static final String XML = ".xml";
    private static final String SLASH = "/";
    private static final String OUTPUT = "nestSched";

    private static final String FILTERINGDATABASES = "filteringDatabases";
    private static final String FILTERINGDATABASE = "filteringDatabase";
    private static final String STATIC = "static";
    private static final String FORWARD = "forward";
    private static final String MULTICASTADDRESS = "multicastAddress";
    private static final String SCHEDULE = "schedule";
    private static final String SCHEDULES = "schedules";
    private static final String DEFAULTCYCLE = "defaultcycle";
    private static final String CYCLE = "cycle";
    private static final String HOST = "host";
    private static final String NAME = "name";
    private static final String MAX = "max";
    private static final String ENTRY = "entry";
    private static final String START = "start";
    private static final String QUEUE = "queue";
    private static final String DEST = "dest";
    private static final String SIZE = "size";
    private static final String MACADDRESS = "macAddress";
    private static final String PORTS = "ports";
    private static final String PORT = "port";
    private static final String SWITCH = "switch";
    private static final String LENGTH = "length";
    private static final String CYCLETIME = "cycleTime";
    private static final String FLOWID = "flowId";
    private static final String ID = "id";
    private static final String BITVECTOR = "bitvector";
    


    public NestSchedXMLGen(Network net) {
        this.net = net;
        writeRoutingToXML();
        writeTrafficGeneratorToXML();
        writePortSchedulingToXML();
        writeEmptyFlow();
    }

    private void writeRoutingToXML () {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {

        	
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();

            // Build document
            this.doc = builder.newDocument();

            Element root = doc.createElement(FILTERINGDATABASES);
            this.doc.appendChild(root);
            
             for(Switch currentSwitch : net.getSwitches()) {
                 if(currentSwitch instanceof TSNSwitch) {
                	 Element FDB = createTagWithAttribute(FILTERINGDATABASE, ID, currentSwitch.getName());
        		     Element ST = doc.createElement(STATIC);
        		     Element FW = doc.createElement(FORWARD);
        		     FDB.appendChild(ST);
        		     ST.appendChild(FW);
        		     ArrayList<Element> MAS = new ArrayList<Element>();
                	 for (Port currentPort : ((TSNSwitch) currentSwitch).getPorts()) {
                		 if (!currentPort.getFlowFragments().isEmpty()) {
                			 for(FlowFragment fragFlow : currentPort.getFlowFragments()) {
                				 Comment comment = doc.createComment("Forward packets addressed to " 
                						 		    + fragFlow.getParent().getName() + " to " + fragFlow.getNextHop());
                				 FW.appendChild(comment);
                				 Element MA = doc.createElement(MULTICASTADDRESS);
                				 if(fragFlow.getParent().getInstance() < 10) {
                					 MA.setAttribute(MACADDRESS, "255-0-00-00-00-0" + Integer.toString(fragFlow.getParent().getInstance()));
                				 } else {
                					 MA.setAttribute(MACADDRESS, "255-0-00-00-00-" + Integer.toString(fragFlow.getParent().getInstance()));
                				 }
                				 MA.setAttribute(PORTS, Integer.toString(currentPort.getPortNum()));
                				 MAS.add(MA);
                			 }
                		 }
                	 }
                	 for(int i=0;i<MAS.size();i++) {
						 for(int j=i+1; j<MAS.size();j++) {
							 if(MAS.get(i).getAttribute(MACADDRESS).equals(MAS.get(j).getAttribute(MACADDRESS))) {
								 MAS.get(i).setAttribute(PORTS, MAS.get(i).getAttribute(PORTS) + " " + MAS.get(j).getAttribute(PORTS));
								 MAS.remove(j);
								 j--;
							 }
						 }
					 }
                	 
        			 for(Element currentElement : MAS) { FW.appendChild(currentElement); }
        			 root.appendChild(FDB);
                 }
             }
             
             prettyPrint("Routing");
            

        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "Something went wrong: {0} ", e);
        }
        
    }
    
    
    
    
    private void writeTrafficGeneratorToXML() {
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	
    	try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            ArrayList<DevGen> devs = new ArrayList<DevGen>();
            
            
            for(int i=0; i<100;i++) {   
            	DevGen newDevGen = new DevGen("dev"+i);
            	for(Switch currentSwitch : net.getSwitches()) {
                	if(currentSwitch instanceof TSNSwitch) {
                		for(Port currentPort : ((TSNSwitch) currentSwitch).getPorts()) {
                			if(!currentPort.getFlowFragments().isEmpty()) {
                				for(FlowFragment flowFrag : currentPort.getFlowFragments()) {
                						if(flowFrag.getStartDevice().getName().equals(newDevGen.getName()) && ((TSNSwitch) currentSwitch).getConnectsTo().contains(flowFrag.getStartDevice().getName())) {
                							newDevGen.addEntry(flowFrag.getDepartureTime(0), 
                    								flowFrag.getFragmentPriority(), 
                    								 flowFrag.getStartDevice().getPacketSize(), 
                    								  flowFrag.getParent().getInstance());
                    			newDevGen.setCycle(flowFrag.getStartDevice().getPacketPeriodicity());
                    			newDevGen.setDefaultCycle(flowFrag.getStartDevice().getPacketPeriodicity());
                    			newDevGen.setMax(flowFrag.getNumOfPacketsSent());
                						}}}}}}
            	if(!newDevGen.getEntries().isEmpty()) {
            		devs.add(newDevGen);
            	}
            }
            
            for(DevGen currentDevGen : devs) {
            	
            	DocumentBuilder builder;
            	builder = factory.newDocumentBuilder();
            	// Build document
            	this.doc = builder.newDocument();

            	Element root = doc.createElement(SCHEDULES);
            	this.doc.appendChild(root);

            	root.appendChild(createTagWithValue(DEFAULTCYCLE, Double.toString(currentDevGen.getDefaultCycle()) + "us"));
            	Element host = doc.createElement(HOST);
            	host.setAttribute(NAME, currentDevGen.getName());
            	host.setAttribute(MAX, Integer.toString(currentDevGen.getMax()));
            	host.appendChild(createTagWithValue(CYCLE, Double.toString(currentDevGen.getDefaultCycle()) + "us"));

            	Collections.sort(currentDevGen.getEntries(), new SortEntries());
            	for(FlowEntry currentEntry : currentDevGen.getEntries()) {
            		Element entry = doc.createElement(ENTRY);
                	
                	entry.appendChild(createTagWithValue(START, Double.toString(currentEntry.getStart()) + "us"));
                	
                	entry.appendChild(createTagWithValue(QUEUE, Integer.toString(currentEntry.getQueue())));
                	entry.appendChild(createTagWithValue(DEST, currentEntry.getDest()));
                	entry.appendChild(createTagWithValue(SIZE, Double.toString(currentEntry.getSize()) +'B'));
                	entry.appendChild(createTagWithValue(FLOWID, Integer.toString(currentEntry.getId())));
                	host.appendChild(entry);
            	}
            	
            	root.appendChild(host);
            	
            	prettyPrint(currentDevGen.getName());
            }
            
            
            
//            for(Switch currentSwitch : net.getSwitches()) {
//            	if(currentSwitch instanceof TSNSwitch) {
//            		for(Port currentPort : ((TSNSwitch) currentSwitch).getPorts()) {
//            			if(!currentPort.getFlowFragments().isEmpty()) {
//            				for(FlowFragment fragFlow : currentPort.getFlowFragments()) {
//            					
//            					DevGen newDev = new DevGen(fragFlow.getStartDevice().getName());
//            					
//            					if(!devs.contains(newDev)) {
//            						if(((TSNSwitch) currentSwitch).getConnectsTo().contains(fragFlow.getStartDevice().getName())) {
//	            	            		devs.add(newDev);
//            						}
//            					}        	            	
//            	            	prettyPrint(fragFlow.getStartDevice().getName());
//            				}}}}}

        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "Something went wrong: {0} ", e);
        }
    }
    
    private void writePortSchedulingToXML() {
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {

            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();

            // Build document
            this.doc = builder.newDocument();

            Element root = doc.createElement(SCHEDULES);
            this.doc.appendChild(root);
            
            root.appendChild(createTagWithValue(DEFAULTCYCLE, "1000us"));
            
             for(Switch currentSwitch : net.getSwitches()) {
                 if(currentSwitch instanceof TSNSwitch) {
                	 Element SW = createTagWithAttribute(SWITCH, NAME, currentSwitch.getName());
                	 for (Port currentPort : ((TSNSwitch) currentSwitch).getPorts()) {
                		 if (!currentPort.getFlowFragments().isEmpty()) {
                			 Cycle currentCycle = currentPort.getCycle();
//                			 if(!cycleFlag) {
//                				 SW.appendChild(createTagWithValue(CYCLE, Double.toString(currentCycle.getCycleDuration()) + "us"));
//                				 cycleFlag = true;
//                			 }
                			 BitVector mirror = new BitVector(1);
                			 Element port = createTagWithAttribute(PORT, ID, currentPort.getPortNum());
                			 Element schedule = createTagWithAttribute(SCHEDULE, CYCLETIME, Double.toString(currentCycle.getCycleDuration()) + "us");
                			 port.appendChild(schedule);
                			 ArrayList<SendWindow> windows = new ArrayList<SendWindow>();
                			 for(int i=0;i<currentCycle.getNumOfPrts();i++) {
                				 for(int j=0;j<currentCycle.getNumOfSlots(i);j++) {
	                				 if(currentCycle.getSlotsUsed().contains(i)) {
	                						 if(currentCycle.getSlotDuration(i, j) > 0) {
	                							 windows.add(new SendWindow(currentCycle.getSlotStart(i,j), currentCycle.getSlotDuration(i, j), i));
			                					 mirror.setBit(i, '0');
			                    				 Collections.sort(windows, new SortWindows());
	                						 }
	                				 }
                				 }
                			 }
                		
                			 Collections.sort(windows, new SortWindows());
                			 double pre_window = 0;
                			 for(int i=0;i<windows.size();i++) {
                				 if((windows.get(i).getStart() > pre_window)) {
	                					 Element mainEntry = doc.createElement(ENTRY);
	                					 Element auxEntry = doc.createElement(ENTRY);
	                					 BitVector bv = new BitVector(0);
	                					 bv.setBit(windows.get(i).getPriority(), '1');
	                					 auxEntry.appendChild(createTagWithValue(LENGTH, Double.toString(windows.get(i).getStart() - pre_window) + "us"));
	                					 auxEntry.appendChild(createTagWithValue(BITVECTOR, mirror.getBitvector()));
	                					 mainEntry.appendChild(createTagWithValue(LENGTH, Double.toString(windows.get(i).getDuration()) + "us"));
	                					 mainEntry.appendChild(createTagWithValue(BITVECTOR, bv.getBitvector()));
	                					 schedule.appendChild(auxEntry); 
	                					 schedule.appendChild(mainEntry);
	                					 pre_window = windows.get(i).getStart() + windows.get(i).getDuration();
                					 
                				 } else {
                					 Element mainEntry = doc.createElement(ENTRY);
                					 BitVector bv = new BitVector(0);
                					 bv.setBit(windows.get(i).getPriority(), '1');
                					 mainEntry.appendChild(createTagWithValue(LENGTH, Double.toString(windows.get(i).getDuration()) + "us"));
                					 mainEntry.appendChild(createTagWithValue(BITVECTOR, bv.getBitvector()));
                					 schedule.appendChild(mainEntry);
                					 pre_window = windows.get(i).getStart() + windows.get(i).getDuration();
                				 }
                			 }
                			 
                			 if(pre_window < currentCycle.getCycleDuration()) {
            					 Element auxEntry = doc.createElement(ENTRY);
            					 auxEntry.appendChild(createTagWithValue(LENGTH, Double.toString(currentCycle.getCycleDuration() - pre_window) + "us"));
            					 auxEntry.appendChild(createTagWithValue(BITVECTOR, mirror.getBitvector()));
            					 schedule.appendChild(auxEntry);
                			 } 
                			 SW.appendChild(port);
                		 }
                	 }
                	 if(SW.hasChildNodes()) {
                		 root.appendChild(SW);
                	 }
                 }
                          
             prettyPrint("PortScheduling");
            
             }
        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "Something went wrong: {0} ", e);
        }
    }
    
    private void writeEmptyFlow() {
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	
    	try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();

            // Build document
            this.doc = builder.newDocument();

            Element root = doc.createElement(SCHEDULE);
            this.doc.appendChild(root);
            
            root.appendChild(createTagWithValue(DEFAULTCYCLE, "1000us"));
            
            ArrayList<String> talkers = new ArrayList<String>();
            for(Flow currentFlow : net.getFlows()) {
            	talkers.add(currentFlow.getStartDevice().getName());
            }
            
            for(Switch currentSwitch : net.getSwitches()) {
            	if(currentSwitch instanceof TSNSwitch) {
            		for(String currentDev : ((TSNSwitch) currentSwitch).getConnectsTo()) {
            			if(net.getSwitch(currentDev) == null && !talkers.contains(currentDev)) {
        	                Element host = doc.createElement(HOST);
        	                host.setAttribute(NAME, currentDev);
//        	                host.setAttribute(MAX, Integer.toString(0));
        	                host.setAttribute(FLOWID, Integer.toString(0));
        	                host.appendChild(createTagWithValue(CYCLE, "1000us"));
        	                
        	                root.appendChild(host);
            			}
            		}
            	}
            }
            
            prettyPrint("emptyFlow");

        } catch (ParserConfigurationException e) {
            logger.log(Level.SEVERE, "Something went wrong: {0} ", e);
        }
    }
    
    private void prettyPrint(String fileName) {
        try {
          TransformerFactory transformerFactory = TransformerFactory.newInstance();
          transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
          Transformer transformer = transformerFactory.newTransformer();

          // output settings
          transformer.setOutputProperty(OutputKeys.INDENT, "yes");
          transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
          transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

          DOMSource source = new DOMSource(this.doc);

            // write to file
          StreamResult file =
                new StreamResult(
                    new FileOutputStream(CURRENT_DIR + SLASH + OUTPUT + SLASH + fileName + XML));
          transformer.transform(source, file);
          
        } catch (TransformerException | FileNotFoundException e) {
          logger.log(Level.SEVERE, "Something went wrong: {0} ", e);
        }
      }
    
    private Node createTagWithValue(String name, Object value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(String.valueOf(value))); // convert Object to String
        return node;
      }
    
    private Element createTagWithAttribute(String nodeName, String attributeName, Object value) {
    	Element node = doc.createElement(nodeName);
    	node.setAttribute(attributeName, String.valueOf(value));
    	return node;
    }

}