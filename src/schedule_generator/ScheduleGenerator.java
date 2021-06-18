//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    Copyright (C) 2021  Aellison Cassimiro
//    
//    TSNsched is licensed under the GNU GPL version 3 or later:
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.

package schedule_generator;

import java.io.*;

import java.time.LocalTime;
import java.util.*;

import com.microsoft.z3.*;

import nest_sched.NestSchedINIGen;
import nest_sched.NestSchedNEDGen;
import nest_sched.NestSchedXMLGen;
import network.Flow;
import network.Network;
import network.Port;
import nodes.Switch;
import nodes.TSNSwitch;
import sched2netconf.XMLExporter;

/**
 * [Class]: ScheduleGenerator
 * [Usage]: Used to generate a schedule based on the properties of
 * a given network through the method generateSchedule. Will create
 * a log file and store the timing properties on the cycles and flows.
 */
public class ScheduleGenerator {
		private Boolean exportModel = false;
		private Boolean generateXMLFiles = false;
		private Boolean generateSimulationFiles = true;
		private Boolean serializeNetwork = true;
		private Boolean loadNetwork = false;
    
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
	       System.out.println("findSchedulerModel\n");
           
           try
           {
                com.microsoft.z3.Global.ToggleWarningMessages(true);
                System.out.print("Z3 Major Version: ");
                System.out.println(Version.getMajor());
                System.out.print("Z3 Full Version: ");
                System.out.println(Version.getString());
                System.out.print("Z3 Full Version String: ");
                System.out.println(Version.getFullVersion());
                System.out.println("\n");

                
                { // These examples need model generation turned on.
                    HashMap<String, String> cfg = new HashMap<String, String>();
                    cfg.put("model", "true");
                    Context ctx = new Context(cfg);

                    return ctx;
                }
            } catch (Z3Exception ex)
            {
                System.out.println("Z3 Managed Exception: " + ex.getMessage());
                System.out.println("Stack trace: ");
                ex.printStackTrace(System.out);
            } catch (Exception ex)
            {
                System.out.println("Unknown Exception: " + ex.getMessage());
                System.out.println("Stack trace: ");
                ex.printStackTrace(System.out);
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
                    System.out.println("Log is still open!");
            } catch (Z3Exception ex)
            {
                System.out.println("Z3 Managed Exception: " + ex.getMessage());
                System.out.println("Stack trace: ");
                ex.printStackTrace(System.out);
            } catch (Exception ex)
            {
                System.out.println("Unknown Exception: " + ex.getMessage());
                System.out.println("Stack trace: ");
                ex.printStackTrace(System.out);
            } 
	   }
	   
	   
	   
	   
	   public void configureNetwork(Network net, Context ctx, Solver solver) {
		   for(Flow flw : net.getFlows()) {
		   	   flw.modifyIfUsingCustomVal();
	    	   flw.convertUnicastFlow();
	    	   flw.setUpPeriods(flw.getPathTree().getRoot());
	       }
	       
	       for(Switch swt : net.getSwitches()) {
	    	   TSNSwitch auxSwt = (TSNSwitch) swt;
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
	   
	   
	   /**
	    * [Method]: generateSchedule
	    * [Usage]: After creating a network, setting up the 
	    * flows and switches, the user now can call this 
	    * function in order calculate the schedule values
	    * using z3 
	    * 
	    * @param net   Network used as base to generate the schedule
	    */
	   public void generateSchedule(Network net) 
	   {
			long totalStartTime = System.nanoTime();
		   
		   Context ctx = this.createContext(); //Creating the z3 context
		   
		   /*
		   for(String tacticName : ctx.getTacticNames()) {
			   System.out.println(tacticName + " - " + ctx.getTacticDescription(tacticName));			   
		   }
		   */
		   
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
		   
		   
		   System.out.println("==================================================");
		   System.out.println("[CREATING FRAGMENTS AND SETTING RULES]");

		   startTime = System.nanoTime();
		   Printer printer = new Printer(); // Used to generate output
		   
	       if(this.loadNetwork) {
	    	   System.out.println("- Loading network and modifications");
	    	   this.serializeNetwork = false; 
	    	   net.loadNetwork(ctx, solver);	
	    	   
	    	   // Sets up the hard constraint for each individual flow in the network
	           net.setJitterUpperBoundRangeZ3(ctx, 25);
	           net.secureHC(solver, ctx);
	       } else {
	    	   System.out.println("- Creating network");
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
	       
	       System.out.println("Time taken to set the rules: " + ((float) totalTime)/1000000000 + " seconds\n ");
	       
	       System.out.println("\n==================================================");
		   System.out.println("[RULES SET. CHECKING SOLVER]");
	       System.out.println("Current time of the day: " + time);
	       
	       startTime = System.nanoTime();

	       Status result = solver.check();
	       if (Status.SATISFIABLE == result)
	       {
	    	   endTime = System.nanoTime();
	    	   totalTime = endTime - startTime;
	    	   System.out.println("Time taken on solving: " + ((float) totalTime)/1000000000 + " seconds ");
	    	   System.out.println("Number of assertions: " + solver.getAssertions().length);
	           model = solver.getModel();
	           
	           /*
	           for(BoolExpr exp : solver.getAssertions()) {
	    		   System.out.println(exp);	    		   
	    	   }
	           */

	           //System.out.println(model);
	           

   		       startTime = System.nanoTime();

   	    	   System.out.println("\n==================================================");
   	    	   System.out.println("[DATA LOGGING]");
   	    	   
   	   		   Expr v = model.evaluate(switch1CycDuration, false);
	           if (v != null)
	           {
	               
	        	   printer.generateLog("log.txt", net, ctx, model);   
	        	   
	        	   /*
	        	   for(Flow f : net.getFlows()) {
		        	   printer.printDataOnTree(f.getPathTree().getRoot(), model, ctx);  	        		   
	        	   }
	        	   */
	        	   
	               printer.printOnConsole(net);
	    	       if(this.exportModel) {
	    	    	   printer.exportModel(solver);
	    	       }
	    	       if(this.generateXMLFiles) {
	    	    	   new XMLExporter(net);	    	   
	    	       }
	    	       
	    	       //System.out.println("- Exporting model");
		           //printer.exportModel(solver);
	    	       
	    	       if(this.serializeNetwork) {
	    	    	   System.out.println("- Serializing network");
	    	    	   this.serializeNetwork(net, "network.ser");
	    	       }
	               
	           } else
	           {
	               System.out.println("Failed to evaluate");
	           }
	       } else
	       {
	    	   endTime = System.nanoTime();
	    	   totalTime = endTime - startTime;
	    	   System.out.println("The specified constraints MIGHT NOT be satisfiable.");
	    	   System.out.println("Time taken on solving: " + ((float) totalTime)/1000000000 + " seconds\n ");

		       startTime = System.nanoTime();

		       
		       
               System.out.println("\n==================================================");
   	    	   System.out.println("[DATA LOGGING]");
    	       if(this.exportModel) {
    	    	   printer.exportModel(solver);
    	       }	    	   	
	           /*
	           for(Flow f : net.getFlows()) {
	        	   printer.printDataOnTree(f.getPathTree().getRoot(), model, ctx);  	        		   
        	   }
        	   */
	       }
	       

    	   endTime = System.nanoTime();
    	   totalTime = endTime - startTime;
    	   System.out.println("Time taken on logging: " + ((float) totalTime)/1000000000 + " seconds");

    	   System.out.println("\n==================================================");



	       this.closeContext(ctx);
	       

		   long totalEndTime   = System.nanoTime();
		   long totalExecutionTime = totalEndTime - totalStartTime;
		
		   System.out.println("Execution time: " + ((float) totalExecutionTime)/1000000000 + " seconds\n ");
		   
		   if(generateSimulationFiles) {
			   generateSimulationFiles(net);
		   }
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
	    		System.out.println("Serialized data is saved in network.ser");
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
			   Flow.instanceCounter=value;
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
	   

		public Boolean getExportModel() {
			return exportModel;
		}



		public void setExportModel(Boolean exportModel) {
			this.exportModel = exportModel;
		}



}
