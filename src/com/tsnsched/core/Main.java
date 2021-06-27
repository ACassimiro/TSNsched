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

package com.tsnsched.core;

import com.tsnsched.generated_scenarios.*;
import com.tsnsched.core.interface_manager.JSONParser;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;
import com.tsnsched.core.interface_manager.*;

public class Main {
    
    public static void main(String[] args)
    {

    	if(args.length == 0) {
    		if(ScheduleGenerator.class.getResource("ScheduleGenerator.class") != null) {
    			String resourcePath = ScheduleGenerator.class.getResource("ScheduleGenerator.class").toString();
				if(!resourcePath.startsWith("file")) {
					System.out.println("[ERROR]: TSNsched running as executable, and no input file was given.");
					return;
				}
    		}
    		
        	//GeneratedCode g = new GeneratedCode();
    	    		
	    	SmallScenario g = new SmallScenario();
	
	        g.runTestCase();
	        
			
		} else {
			
			ParserManager parser = new ParserManager(args[0]);
			Network net = parser.parseFromFile();
			
			ScheduleGenerator gen = new ScheduleGenerator();
			
			gen.generateSchedule(net);
			
		}
    
    }
}
