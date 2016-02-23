package server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

public class AuditLog {
Calendar time = Calendar.getInstance();	
	
	
	
	public boolean log(String user, String action, String wanted){
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("auditlog.txt", true)))) {
		    out.println(time.getTime()+" "+user+" wanted to \""+action+"\" the contents of \""+ wanted+"\"");
		    //more code
		    //more code
		}catch (IOException e) {
		    //exception handling left as an exercise for the reader
		}
		return true;
	}



	public void log(String name, String msg) {
		// TODO Auto-generated method stub
		
	}
	

}
//log.log(currentUser.getName(),msgParts[0],msgParts[1]);