package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import types.*;

//Hall�!!
//hej

public class DataBase implements Serializable {
	private HashMap<String, ArrayList<Record>> recordsMap; // ersätta alla
															// hashmaps med en
															// med User objekt
															// ist?
	private HashMap<String, User> users;


	public DataBase() {
		recordsMap = new HashMap<String, ArrayList<Record>>();
		users = new HashMap<String, User>();

		addForTest();
	}

	public ArrayList<Record> getPatientRecords(String name) {
		return recordsMap.get(name);
	}

	public Record getRecord(String name, String date) { // throw error
		for (Record temp : recordsMap.get(name)) {
			if (temp.getDate().equals(date)) {
				return temp;
			}
		}
		return null;
	}



	public User findUser(String name) {
		return users.get(name);
	}

	private void addForTest() {
		// gov= new Government("kim",3);
		users.put("kim", new Doctor("kim", 2));
		users.put("hampe", new Patient("hampe", 3));
		ArrayList<Record> recordsTemp = new ArrayList<Record>();
		recordsTemp.add(new Record("kim", "Nurse Lasse", 3, "2015-06-17", "removed \n leg."));
		recordsTemp.add(new Record("Doctor Liset", "kim", 4, "2015-06-19", "has lungs"));
		users.put("simon", new Patient("simon", 3));
		ArrayList<Record> recordsTemp2 = new ArrayList<Record>();
		recordsMap.put("hampe",recordsTemp);
		recordsMap.put("simon", recordsTemp2);
		
		users.put("KimKumz", new Doctor("Kim Kumz", 2));
		
	}

	public void putPatientRecords(String name, ArrayList<Record> records) {
		recordsMap.put(name, records);

	}
}
