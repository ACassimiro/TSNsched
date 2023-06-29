//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.tsnsched.core.schedule_generator;

import java.io.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalTime;
import java.util.*;

import com.microsoft.z3.*;
import com.tsnsched.core.network.NetworkProperties;
import com.tsnsched.nest_sched.NestSchedINIGen;
import com.tsnsched.nest_sched.NestSchedNEDGen;
import com.tsnsched.nest_sched.NestSchedXMLGen;
import com.tsnsched.core.components.Flow;
import com.tsnsched.core.components.Port;
import com.tsnsched.core.interface_manager.ParserManager;
import com.tsnsched.core.interface_manager.Printer;
import com.tsnsched.core.network.Network;
import com.tsnsched.core.network.NetworkModificationHandler;
import com.tsnsched.core.nodes.Switch;
import com.tsnsched.core.nodes.TSNSwitch;
import com.tsnsched.core.sched2netconf.XMLExporter;

/**
 * [Class]: ScheduleGenerator
 * [Usage]: Used to generate a schedule based on the properties of
 * a given network through the method generateSchedule. Will create
 * a log file and store the timing properties on the cycles and flows.
 */
public class ScheduleGenerator {
		private Boolean exportModel = false;
		private Boolean generateXMLFiles = false;
		private Boolean generateSimulationFiles = false;
		private Boolean serializeNetwork = false;
		private Boolean loadNetwork = false;
		private Boolean enableConsoleOutput = false;
		private Boolean enableLoggerFile = false;
		private Boolean generateJSONOutput = true;
		private Boolean useIncrementalStrategy = false;
		private Boolean enablePacketTimeOutput = false;

		private String topologyFilePath = "network.ser";
		private ArrayList<Flow> tempFlowList;
		private int auxIncrementalFlowCounter = 1;

		private ParserManager parserManager = null;
		private Printer printer = new Printer(); // Used to generate output

	   @SuppressWarnings("serial")
	   class TestFailedException extends Exception
	   {
	       public TestFailedException()
	       {
	           super("Check FAILED");
	       }
	   };
	   
	   
	   public ScheduleGenerator() {
		   
	   }
	   
	   
	   
	   public ScheduleGenerator(Boolean loadNetwork) {
		   this.loadNetwork = loadNetwork;
	   }
	   
	   
	   /**
	    * [Method]: createContext
	    * [Usage]: Returns a z3 context used as an environment 
	    * for creation of z3 variables and operations
	    * 
	    * @return  A z3 context
	    */
	   public Context createContext() {
           
           try
           {
                com.microsoft.z3.Global.ToggleWarningMessages(true);
            	this.printer.printIfLoggingIsEnabled("Z3 Major Version: " + Version.getMajor());
            	this.printer.printIfLoggingIsEnabled("Z3 Full Version: " + Version.getString());
            	this.printer.printIfLoggingIsEnabled("Z3 Full Version String: " + Version.getFullVersion() + "\n");

                
                { // These examples need model generation turned on.
                    HashMap<String, String> cfg = new HashMap<String, String>();
                    cfg.put("model", "true");
                    Context ctx = new Context(cfg);

                    return ctx;
                }
            } catch (Z3Exception ex)
            {
            	this.printer.printIfLoggingIsEnabled("Z3 Managed Exception: " + ex.getMessage());
            } catch (Exception ex)
            {
            	this.printer.printIfLoggingIsEnabled("Unknown Exception: " + ex.getMessage());
            } 
           
            return null;
	   }
	   
	   
	   /**
	    * [Method]: closeContext
	    * [Usage]: Clears and close the context used to 
	    * generate the schedule.
	    * 
	    * @param ctx   Context to be cleared
	    */
	   public void closeContext(Context ctx) {
	       try
           {
                
                { 
                    ctx.close();
                }
                
                //Log.close();
                if (Log.isOpen())
                	this.printer.printIfLoggingIsEnabled("Log is still open!");
            } catch (Z3Exception ex)
            {
            	this.printer.printIfLoggingIsEnabled("Z3 Managed Exception: " + ex.getMessage());
            } catch (Exception ex)
            {
            	this.printer.printIfLoggingIsEnabled("Unknown Exception: " + ex.getMessage());
            } 
	   }
	   
	   
	   
	   
	   public void configureNetwork(Network net, Context ctx, Solver solver) {
		   for(Flow flw : net.getFlows()) {
			   flw.setPrinter(this.printer);
		   	   flw.modifyIfUsingCustomVal();
	    	   flw.convertUnicastFlow();
	    	   flw.setUpPeriods(flw.getPathTree().getRoot());
	       }
	       
	       for(Switch swt : net.getSwitches()) {
	    	   TSNSwitch auxSwt = (TSNSwitch) swt;
	    	   auxSwt.setPrinter(this.printer);
	    	   auxSwt.setUpCycleSize(solver, ctx);
	       }
	       
	       
	       // On all network flows: Data given by the user will be converted to z3 values 
	       for(Flow flw : net.getFlows()) {
	           flw.toZ3(ctx);
	       }
	       
	       // On all network switches: Data given by the user will be converted to z3 values
           for(Switch swt : net.getSwitches()) {
               ((TSNSwitch) swt).toZ3(ctx, solver);
           }
           
           net.preventCollisionOnFirstHop(solver, ctx);
		   net.assertFirstSendingTimeOfFlows(solver, ctx);

		   // Sets up the hard constraint for each individual flow in the network

           net.setJitterUpperBoundRangeZ3(ctx, 25);
	       net.secureHC(solver, ctx);
	   }



	   public void generateSchedule(String topologyFilePath)
	   {

		    this.parserManager = new ParserManager(topologyFilePath);
		    this.parserManager.setPrinter(this.printer);
			Network net = this.parserManager.parseFromFile();
			
			this.generateSchedule(net);
		   
	   }

	   public void generateSchedule(Network net){
		   /*
			this.setRulesAndAttemptScheduling(net);
		   /**/
		   /**/
		   if(!this.useIncrementalStrategy){
			   this.setRulesAndAttemptScheduling(net);
		   } else {

			   this.serializeNetwork=true;

			   if(this.useIncrementalStrategy){
				   this.tempFlowList = (ArrayList<Flow>) net.getFlows().clone();
				   net.setFlows(new ArrayList<Flow>());
				   net.addFlow(this.tempFlowList.get(0));
			   }

			   boolean successfullyScheduled = this.setRulesAndAttemptScheduling(net);
			   this.tempFlowList.remove(0);
			   this.loadNetwork=true;

			   for(Flow flw : this.tempFlowList) {
				    if(successfullyScheduled == false) {
						break;
					}

					net = this.deserializeNetwork("network.ser");
					Flow flow = new Flow(net, flw);
					net.addElement(flow, NetworkProperties.INCREMENTFLOW);
					successfullyScheduled = this.setRulesAndAttemptScheduling(net);
			   }


		   }
			/**/
	   }

	   /**
	    * [Usage]: After creating a network, setting up the
	    * flows and switches, the user now can call this 
	    * function in order calculate the schedule values
	    * using z3 
	    * 
	    * @param net   Network used as base to generate the schedule
	    */
	   public boolean setRulesAndAttemptScheduling(Network net)
	   {
		   boolean successfullyScheduled = false;
		   this.printer.setEnableConsoleOutput(this.enableConsoleOutput);
		   this.printer.setEnableLoggerFile(this.enableLoggerFile);

		   if(this.parserManager == null) {
			   this.parserManager = new ParserManager();
			   this.parserManager.setPrinter(this.printer);
		   }

		   net.setPrinter(printer);
		   
		   long totalStartTime = System.nanoTime();
		   
		   Context ctx = this.createContext(); //Creating the z3 context
		   
		   String name = "qfufbv_ackr";
		   //System.out.println(ctx.getTacticDescription(name));
		   //qfufbv, qfufbv_ackr, nra, qsat, psmt
		   Tactic t1 = ctx.mkTactic(name);
		   //Tactic t2 = ctx.mkTactic("nra");
		   //Tactic t = ctx.parAndThen(t1, t2);
	       Solver solver = ctx.mkSolver(t1);     //Creating the solver to generate unknown values based on the given context
	       
		   
		   long startTime;
		   long endTime;
		   long totalTime;
		   
		   
		   this.printer.printIfLoggingIsEnabled("==================================================");
		   this.printer.printIfLoggingIsEnabled("[CREATING FRAGMENTS AND SETTING RULES]");

		   startTime = System.nanoTime();
		   
		   
	       if(this.loadNetwork) {
	    	   this.printer.printIfLoggingIsEnabled("- Loading network and modifications");
	    	   //this.serializeNetwork = false; 
	    	   net.createNewObjects();
	    	   net.loadNetwork(ctx, solver);	
	    	   Flow.setInstanceCounter(net.getFlows().size() + 1);
	    	   if(net.getHasBeenModified()) {
	    		   net.setSolverAndContextForNetModHandler(solver, ctx);
	    		   net.applyChangesToSolver();	    		   
	    	   }
	    	   // Sets up the hard constraint for each individual flow in the network
	           net.preventCollisionOnFirstHop(solver, ctx);
			   net.assertFirstSendingTimeOfFlows(solver, ctx);
	           net.secureHC(solver, ctx);
	       } else {
	    	   this.printer.printIfLoggingIsEnabled("- Creating network");
	    	   this.configureNetwork(net, ctx, solver);	    	   
	       }
	       

	       // A switch is picked in order to evaluate the unknown values
           TSNSwitch switch1 = null;
           switch1 = (TSNSwitch) net.getSwitches().get(0);
           
           /*
            * The duration of the cycle is given as a question to z3, so all the 
            * constraints will have to be evaluated in order to z3 to know this cycle
            * duration 
            */            
           RealExpr switch1CycDuration = null;

           for(Switch swt : net.getSwitches()) {
        	   for(Port port : ((TSNSwitch)swt).getPorts()) {
        		   if(port.getIsModifiedOrCreated()) {
        			   switch1CycDuration = port.getCycle().getCycleDurationZ3();
        			   break;
        		   }
        	   }
           }
           
           if(switch1CycDuration == null) {
        	   switch1CycDuration = switch1.getCycle(0).getCycleDurationZ3();
           }
           
           
           endTime = System.nanoTime();
           totalTime = endTime-startTime;
           
	       /* find model for the constraints above */
	       Model model = null;
	       LocalTime time = LocalTime.now();
	       
	       this.printer.printIfLoggingIsEnabled("Time taken to set the rules: " + ((float) totalTime)/1000000000 + " seconds\n ");
	       
	       this.printer.printIfLoggingIsEnabled("\n==================================================");
	       this.printer.printIfLoggingIsEnabled("[RULES SET. CHECKING SOLVER]");
	       this.printer.printIfLoggingIsEnabled("Current time of the day: " + time);
	       
	       startTime = System.nanoTime();

	       Status result = solver.check();
	       if (Status.SATISFIABLE == result)
	       {
	    	   endTime = System.nanoTime();
	    	   totalTime = endTime - startTime;
	    	   this.printer.printIfLoggingIsEnabled("Time taken on solving: " + ((float) totalTime)/1000000000 + " seconds ");
	    	   this.printer.printIfLoggingIsEnabled("Number of assertions: " + solver.getAssertions().length);
	           model = solver.getModel();
	           
	           /*
	           for(BoolExpr exp : solver.getAssertions()) {
	    		   System.out.println(exp);	    		   
	    	   }
	           */

	           //System.out.println(model);
	           

   		       startTime = System.nanoTime();

   		       this.printer.printIfLoggingIsEnabled("\n==================================================");
   		       this.printer.printIfLoggingIsEnabled("[DATA LOGGING]");
   	    	   
   	   		   Expr v = model.evaluate(switch1CycDuration, false);
	           if (v != null)
	           {
	        	   successfullyScheduled = true;
	        	   net.setAllElementsToNotModified();
	        	   
	        	   if(this.exportModel) {
	    	    	   printer.exportModel(solver);
	    	       }
	        	   
            	   printer.generateLog("log.txt", net, ctx, model);   	            	   
	        	   
	        	   /*
	        	   for(Flow f : net.getFlows()) {
		        	   printer.printDataOnTree(f.getPathTree().getRoot(), model, ctx);  	        		   
	        	   }
	        	   */
	        	   
	               printer.printOnConsole(net);
	    	       
	    	       if(this.generateXMLFiles) {
	    	    	   new XMLExporter(net);	    	   
	    	       }
	    	       
	    	       //System.out.println("- Exporting model");
		           //printer.exportModel(solver);
	    	       
	    	       if(this.serializeNetwork) {
	    	    	   this.printer.printIfLoggingIsEnabled("- Serializing network");
	    	    	   this.serializeNetwork(net, "network.ser");
	    	       }
	       
	    	       if(this.generateSimulationFiles) {
	    	    	   this.printer.printIfLoggingIsEnabled("- Generating simulation files");
	    	    	   generateSimulationFiles(net);			   
	    		   }
	    	       
	    	       if(this.generateJSONOutput) {
					   this.parserManager.setEnablePacketTimeOutput(this.enablePacketTimeOutput);
	    	    	   this.parserManager.parseOutput(net);	    	    	   
	    	       }
	    	       
	           } else
	           {
	        	   this.printer.printIfLoggingIsEnabled("Failed to evaluate");
	           }
	       } else
	       {
	    	   endTime = System.nanoTime();
	    	   totalTime = endTime - startTime;
	    	   this.printer.printIfLoggingIsEnabled("The specified constraints MIGHT NOT be satisfiable.");
	    	   this.printer.printIfLoggingIsEnabled("Time taken on solving: " + ((float) totalTime)/1000000000 + " seconds\n ");

		       startTime = System.nanoTime();

		       this.printer.printIfLoggingIsEnabled("\n==================================================");
		       this.printer.printIfLoggingIsEnabled("[DATA LOGGING]");
    	       if(this.exportModel) {
    	    	   printer.exportModel(solver);
    	       }	    	   	
	           /*
	           for(Flow f : net.getFlows()) {
	        	   printer.printDataOnTree(f.getPathTree().getRoot(), model, ctx);  	        		   
        	   }
        	   */
	       }
	       
		   this.closeContext(ctx);
		   
    	   endTime = System.nanoTime();
    	   totalTime = endTime - startTime;
    	   this.printer.printIfLoggingIsEnabled("Time taken on logging: " + ((float) totalTime)/1000000000 + " seconds");

    	   this.printer.printIfLoggingIsEnabled("\n==================================================");

		   long totalEndTime   = System.nanoTime();
		   long totalExecutionTime = totalEndTime - totalStartTime;
		
		   this.printer.printIfLoggingIsEnabled("Execution time: " + ((float) totalExecutionTime)/1000000000 + " seconds\n ");

		   return successfullyScheduled;
	   }
	   
	   
	   /**
	    * [Method]: generateSimulationFiles
	    * [Usage]: Generate the XML, INI and NED files
	    * needed to the Nesting simulation.
	    * 
	    * @param net		Network object to be serialized
	    */
	   public void generateSimulationFiles(Network net) {
		 //Create the folder and the simulation files
	       File folder = new File(System.getProperty("user.dir") + "/nestSched");
	       if(!folder.exists()) {
	    	   if(folder.mkdir()) {
		    	   new NestSchedXMLGen(net);
				   new NestSchedINIGen(net);
				   new NestSchedNEDGen(net);
	    	   }
	       } else {
	    	   String[]entries = folder.list();
	    	   for(String s: entries){
		           File currentFile = new File(folder.getPath(),s);
		           currentFile.delete();
		       }
	    	   new NestSchedXMLGen(net);
			   new NestSchedINIGen(net);
			   new NestSchedNEDGen(net);
	       }
	   }


	   /**
	    * [Method]: serializateNetwork
	    * [Usage]: Serialize the primitive objects of the network object.
	    * The serialized file is stored in the path string folder. Can be used
	    * to store the data of a network and the values of the generated 
	    * schedule.
	    * 
	    * @param net		Network object to be serialized
	    * @param path		Path of for the serialized object file
	    */
	   public void serializeNetwork(Network net, String path) {
		   
		   for(Switch swt : net.getSwitches()) {
			   if(swt instanceof TSNSwitch) {
				   ((TSNSwitch) swt).setIsModifiedOrCreated(false);
				   for(Port port : ((TSNSwitch) swt).getPorts()) {
					   port.setIsModifiedOrCreated(false);
				   }
			   }			   
		   }
		   
	    	try {
	            FileOutputStream fileOut = new FileOutputStream(path);
	            ObjectOutputStream out = new ObjectOutputStream(fileOut);
	            out.writeObject(net);
	            out.close();
	            fileOut.close();
	            this.printer.printIfLoggingIsEnabled("Serialized data is saved in network.ser");
	         } catch (Exception i) {
	            i.printStackTrace();
	         }
	    }
	   
	   /**
	    * [Method]: deserializeNetwork
	    * [Usage]: From a serialized object file, load the primitive
	    * values of the stored object.
	    * 
	    * @param path		Path of the serialized object file
	    * @return			The network object with all its primitive values
	    */
	   public Network deserializeNetwork(String path) {
		   Network net = null;
		   
		   try {
	           FileInputStream fileIn = new FileInputStream(path);
	           ObjectInputStream in = new ObjectInputStream(fileIn);
	           net = (Network) in.readObject();
	           net.setNetModHandler(new NetworkModificationHandler());
	           in.close();
	           fileIn.close();			   
		   } catch (Exception i) {
 	           i.printStackTrace();
	           return null;
	       } 		   
		   
		   if(net.getFlows().size() > 0) {
			   net.getFlows().get(0).setInstanceCounter(
					net.getFlows().size()
			   );
		   }
		   
		   this.loadInstanceCounters(net);
		   
		   return net;
	   }
	   
	   
	   public void loadInstanceCounters(Network net) {
		   int value = -1;
		   
		   for(Flow flow : net.getFlows()) {
			   if(flow.getInstance() > value) {
				   value = flow.getInstance();
			   }
		   }
		   
		   if(value > -1) {
			   Flow.setInstanceCounter(value);
		   }
		   
		   value = -1;
		   /*
		    * Do the same for instances of the cycles and devices
		   for(Switch swt : net.getSwitches()) {
			   if(swt instanceof TSNSwitch) {
				   for(Port port : ((TSNSwitch) swt).getPorts()) {
					   if(port.getCycle)
				   }
			   }
		   }
		    */
		   
	   }
	   

		
		public void setParameters(String []args) {
			
			for(String argument : args) {
				if(!argument.contains("-")) {
					continue;
				}
				
				
				switch(argument) {
					case "-exportModel":
						this.exportModel=true;
						break;
					case "-generateXMLFiles":
						this.generateXMLFiles=true;
						break;
					case "-generateSimulationFiles":
						this.generateSimulationFiles=true;
						break;
					case "-serializeNetwork":
						this.serializeNetwork=true;
						break;
					case "-loadNetwork":
						this.loadNetwork=true;
						break;
					case "-enableConsoleOutput":
						this.enableConsoleOutput=true;
						break;
					case "-enableLoggerFile":
						this.enableLoggerFile=true;
						break;
					case "-disableJSONOutput":
						this.generateJSONOutput=false;
						break;
					case "-useIncremental":
						this.useIncrementalStrategy=true;
						break;
					case "-enablePacketTimeOutput":
						this.enablePacketTimeOutput=true;
						break;

				}
				
			}
			
		}

		public Boolean getExportModel() {
			return exportModel;
		}

		public void setExportModel(Boolean exportModel) {
			this.exportModel = exportModel;
		}

		public Boolean getGenerateXMLFiles() {
			return generateXMLFiles;
		}

		public void setGenerateXMLFiles(Boolean generateXMLFiles) {
			this.generateXMLFiles = generateXMLFiles;
		}

		public ParserManager getParserManager() {
			return parserManager;
		}

		public void setParserManager(ParserManager parserManager) {
			this.parserManager = parserManager;
		}

		public Boolean getGenerateSimulationFiles() {
			return generateSimulationFiles;
		}

		public void setGenerateSimulationFiles(Boolean generateSimulationFiles) {
			this.generateSimulationFiles = generateSimulationFiles;
		}

		public Boolean getSerializeNetwork() {
			return serializeNetwork;
		}

		public void setSerializeNetwork(Boolean serializeNetwork) {
			this.serializeNetwork = serializeNetwork;
		}

		public Boolean getEnableConsoleOutput() {
			return enableConsoleOutput;
		}

		public void setEnableConsoleOutput(Boolean enableConsoleOutput) {
			this.enableConsoleOutput = enableConsoleOutput;
		}

		public Boolean getGenerateJSONOutput() {
			return generateJSONOutput;
		}

		public void setGenerateJSONOutput(Boolean generateJSONOutput) {
			this.generateJSONOutput = generateJSONOutput;
		}

		public Boolean getEnableLoggerFile() {
			return enableLoggerFile;
		}

		public void setEnableLoggerFile(Boolean enableLoggerFile) {
			this.enableLoggerFile = enableLoggerFile;
		}

		public Boolean getEnablePacketTimeOutput() {
			return enablePacketTimeOutput;
		}

		public void setEnablePacketTimeOutput(Boolean enablePacketTimeOutput) {
			this.enablePacketTimeOutput = enablePacketTimeOutput;
		}
		
		
}
