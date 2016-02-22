package types;

import java.io.Serializable;

public class Record implements Serializable {
private String doctor;
private String nurse;
private String patientName;
private int division;
private String medicalData;
private String date;

public Record(String doctor, String nurse, int division, String date, String medicalData) {
	this.date=date;
	this.doctor=doctor;
	this.nurse=nurse;
	this.medicalData=medicalData;
	this.division=division;
}

public String getDate() {
	// TODO Auto-generated method stub
	return date;
}

public int getDivision() {
	// TODO Auto-generated method stub
	return division;
}

public String getMedicalData() {
	// TODO Auto-generated method stub
	return medicalData;
}

public String getDoctor() {
	// TODO Auto-generated method stub
	return doctor;
}

public String getNurse() {
	// TODO Auto-generated method stub
	return nurse;
}

public void setMedicalData(String medicalData) {
	this.medicalData=medicalData;
	System.out.println("medicaldata is " + medicalData);
	
}
}
