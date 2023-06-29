import java.util.*;
import java.io.*;
import com.tsnsched.core.interface_manager.JSONParser;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;
import com.tsnsched.core.interface_manager.*;

public class GenerateScheduleJSONInput {
	public static void main(String []args){

		JSONParser parser = new JSONParser(args[0]);
		Network net = parser.parseInput();
		
		ScheduleGenerator gen = new ScheduleGenerator();
		
		gen.setParameters(args);
		gen.generateSchedule(net);
		parser.generateOutput(net);

	}

}