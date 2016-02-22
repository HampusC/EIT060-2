package types;

public class Record {
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
}
