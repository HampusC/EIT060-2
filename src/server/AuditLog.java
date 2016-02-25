package server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

public class AuditLog {
Calendar time = Calendar.getInstance();	
	



	public void log(String user, String msg, Boolean allowed) throws IOException {
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("auditlog.txt", true)))) {
			if(!allowed){
			out.println(time.getTime()+" "+user+" was denied to \""+msg+"\"");
		// TODO Auto-generated method stub
			}else{
				out.println(time.getTime()+" "+user+" was allowed to \""+msg+"\"");
			}
		}
		
	}
	

}
//log.log(currentUser.getName(),msgParts[0],msgParts[1]);