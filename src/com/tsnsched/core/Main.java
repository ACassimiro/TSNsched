//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
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
    		//ReschedulingScenario g = new ReschedulingScenario();
    		//IncrementalScenario g = new IncrementalScenario();
	    	SmallScenario g = new SmallScenario();
	
	        g.runTestCase();
	        
			
		} else {
			ScheduleGenerator gen = new ScheduleGenerator();
			gen.setParameters(args);
			gen.generateSchedule(args[0]);
			
		}
    
    }
}
