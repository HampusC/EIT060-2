package types;
import java.util.ArrayList;

public class Patient extends User  {
	ArrayList<Record> records;


	
	public ArrayList<Record> readRecord(){
		
		return records;
	}
	
}
