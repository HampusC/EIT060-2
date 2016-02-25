package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import types.*;

public class DataBase implements Serializable {
	private HashMap<String, ArrayList<Record>> recordsMap;
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

		// Docotors
		users.put("KimKumz", new Doctor("Kim Kumz", 3));
		users.put("lisaLisson", new Doctor("Lisa Lisson", 1));

		// Nures
		users.put("GretaGroot", new Nurse("Greta Groot", 3));
		users.put("JillJillson", new Nurse("Jill Jillson", 1));

		// Patients
		users.put("AnnaAnd", new Patient("Anna And", 1));
		ArrayList<Record> recordsAnna = new ArrayList<Record>();
		recordsAnna.add(new Record("Kim Kumz", "Jill Jillson", 3, "2015-06-17", "Bad headache."));
		recordsAnna.add(new Record("Kim Kumz", "Jill Jillson", 4, "2015-06-19", "No more headache."));
		recordsMap.put("Anna And", recordsAnna);

		users.put("TimBorglund", new Patient("Tim Borglund", 3));
		ArrayList<Record> recordsTim = new ArrayList<Record>();
		recordsMap.put("Tim Borglund", recordsTim);

		users.put("AndersAndersson", new Patient("Anders Andersson", 5));
		ArrayList<Record> recordsAnders = new ArrayList<Record>();
		recordsMap.put("Anders Andersson", recordsAnders);

		// Government
		users.put("Government", new Government("Government", 0));

	}

	public void putPatientRecords(String name, ArrayList<Record> records) {
		recordsMap.put(name, records);

	}
}
