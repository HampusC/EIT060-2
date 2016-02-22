package types;
import java.util.ArrayList;

import server.DataBase;

public abstract class User {
	private String serial;
	private int division;
	protected String name;
	private ArrayList<String> patients;
	
	public  User(String name,int division, ArrayList<String> patients ){
		this.name=name;
		this.division=division;
		this.patients=patients;
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
	
	public String getName() {
		return name;
	}
	

  
}
