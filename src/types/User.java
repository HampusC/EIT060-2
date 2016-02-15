package types;
import java.util.ArrayList;

import server.DataBase;

public abstract class User {
	private String serial;
	private int division;
	private String name;
	private ArrayList<String> patients;
	
	public  User(){

	}
	public boolean checkIfInPatientsList(String name) {
		for(String temp:patients){
			if(temp.equals(name)){
				return true;
			}
		}
		return false;
		
	}
	public int getDivision() {
		// TODO Auto-generated method stub
		return division;
	}
	

  
}
