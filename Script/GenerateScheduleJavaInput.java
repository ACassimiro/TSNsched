import java.util.*;
import java.io.*;
import com.tsnsched.core.interface_manager.JSONParser;
import com.tsnsched.core.network.*;
import com.tsnsched.core.schedule_generator.*;
import com.tsnsched.core.interface_manager.*;

public class GenerateScheduleJavaInput {
	public static void main(String []args){

		try{

			UseCase g = new UseCase();
			
			g.runTestCase();
		
		} catch (Exception e){
			System.out.println(e.getMessage());
		}

	}

}