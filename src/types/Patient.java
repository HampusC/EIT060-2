package types;
import java.util.ArrayList;

public class Patient extends User  {
	public Patient(String name, int division) {
		super(name, division);
		// TODO Auto-generated constructor stub
	}



	ArrayList<Record> records;


	
	public ArrayList<Record> readRecord(){
		
		return records;
	}
	@Override
	public boolean checkIfInPatientsList(String name) {
		return(this.name.equals(name));
	
}
}
