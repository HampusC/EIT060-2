package server;

import java.util.ArrayList;
import java.util.HashMap;

import types.*;

//Hall�!!
//hej

public class DataBase {
	private HashMap<String, ArrayList<Record>> recordsMap; //ersätta alla hashmaps med en med User objekt ist?
	private HashMap<String, Patient> patients;
	private HashMap<String, Nurse> nurses;
	private HashMap<String, Doctor> doctors;

	public DataBase() {
		recordsMap = new HashMap<String, ArrayList<Record>>();
		patients=new HashMap<String, Patient>();
		nurses=new HashMap<String, Nurse>();
		doctors=new HashMap<String, Doctor>();
	}

	public ArrayList<Record> getPatientRecords(String name){
		return recordsMap.get(name);
	}

	public Record getRecord(String name, String date) { //throw error
		for (Record temp : recordsMap.get(name)) {
			if (temp.getDate().equals(date)) {
				return temp;
			}
		}
		return null;
	}

	public boolean checkDivision(int division, String name) { // kan hända att det är samam division men itne finns ngra records än?
	if(patients.get(name).getDivision()==division){
			return true;
		}
		return false;
	}

	public User findUser(String name) {
		User temp;
		temp = patients.get(name);
		temp = nurses.get(name);
		temp = doctors.get(name);
		return temp;
	}

}
