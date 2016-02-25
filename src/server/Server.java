package server;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.ArrayList;

import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

import types.*;


public class Server implements Runnable {
	private ServerSocket serverSocket = null;
	private static int numConnectedClients = 0;
	private DataBase db;
	private User currentUser; // remeber to set usetr accordign to certificate
	private AuditLog log;
	private final String FILEPATH = "database.ser";

	public Server(ServerSocket ss) throws IOException {
		serverSocket = ss;
		newListener();
		boolean loaded = false;
		if(!loaded){ //sätt till !loaded
			db= new DataBase();
			
		}
		log = new AuditLog();
	}

	public void run() {
		try {
			SSLSocket socket = (SSLSocket) serverSocket.accept();
			newListener();
			SSLSession session = socket.getSession();
			System.out.println(session.getCipherSuite());
			X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
			String subject = cert.getSubjectDN().getName();
			String cn = subject.substring(3, subject.indexOf(","));
			String issuer = cert.getIssuerDN().getName();
			String serial = cert.getSerialNumber().toString();
			numConnectedClients++;
			System.out.println("client connected");
			System.out.println("client name (cert subject DN field): " + subject);
			System.out.println("certificate issuer (issuer DN field) on certificate received :\n" + issuer + "\n");
			System.out.println(
					"certificate serial number (serial number field) on certificate received:\n" + serial + "\n");

			System.out.println(numConnectedClients + " concurrent connection(s)\n");
			currentUser = db.findUser(cn); // should be subject
			System.out.println(cn + " logged.");
			 if(currentUser==null){  //ta hand om när null
				System.out.println("no user found");
				return;
			 }
			
			if(currentUser instanceof Government){
				System.out.println("is gov");
			}
			if(currentUser instanceof Doctor){
				System.out.println("is doc");
			}
			if(currentUser instanceof Nurse){
				System.out.println("is nurse");
			}
			ObjectOutputStream out =null;
			ObjectInputStream in = null;
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			Object clientMsg = null;
			while ((clientMsg = in.readObject()) != null) {
				// String rev = new
				// StringBuilder(clientMsg).reverse().toString();
				// System.out.println("received '" + clientMsg + "' from
				// client");
				// System.out.print("sending '" + rev + "' to client...");
				// out.println(rev);
				// out.flush();
				// System.out.println("done\n");
				if(clientMsg instanceof String){
				interpretMessage((String)clientMsg, out, in);
				}
				}
			in.close();
			out.close();
			socket.close();
			numConnectedClients--;
			System.out.println("client disconnected");
			System.out.println(numConnectedClients + " concurrent connection(s)\n");
			// }
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Client died: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
	private boolean loadDataBase() {
		try {
		FileInputStream fin = new FileInputStream(FILEPATH);
		ObjectInputStream ois = new ObjectInputStream(fin);
			db = (DataBase) ois.readObject();
			
			ois.close();
			return true;
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
	}
	private void saveDataBase(){
		try {
		FileOutputStream fout = new FileOutputStream(FILEPATH);
		ObjectOutputStream oos;
			oos = new ObjectOutputStream(fout);
			oos.writeObject(db);
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void interpretMessage(String msg, ObjectOutputStream out, ObjectInputStream in) throws IOException { // kom ihåg,
																	// vad
																	// händer om
																	// read
				try{												// göran
																	// knutsson
		System.out.println(msg);
		String msgParts[] = msg.split(": ");
		for (String temp : msgParts) {
			System.out.println(temp);
		}
		
		if (msgParts[0].equals("list")) {
			list(msgParts[1], out);
		} else if (msgParts[0].equals("read")) {
			read(msgParts[1], msgParts[2], out);
		} else if(msgParts[0].equals("write")){
			try {
				write(msgParts[1], msgParts[2], out, in);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (msgParts[0].equals("delete")){
			
			delete(msgParts[1], msgParts[2], out);
			
		}else if(msgParts[0].equals("create")){
			create(msgParts[1], out, in);

			
		
		}else{
			out.writeObject("failed to interpret");
			log.log(currentUser.getName(),msg+" (failed to interpret)",false);

		}
				}catch(Exception e)	{
					out.writeObject("failed to interpret");
					log.log(currentUser.getName(),msg+" (failed to interpret)",false);
				}
				
	}

	private void write(String name, String date, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
			//se till att inte ok bara för rätt division
			
			ArrayList<Record> records = db.getPatientRecords(name);
			for (Record temp : records) {
				if(temp.getDate().equals(date)){
					if(checkWriteAccess(temp)){
					out.writeObject("send data");
					out.reset();
					out.writeObject(temp);
					Object tempIn =  in.readObject();
					if(tempIn instanceof Record){
				temp.setMedicalData(((Record)tempIn).getMedicalData());
				out.writeObject("received");
				saveDataBase();
				log.log(currentUser.getName(), "write "+name+" "+date, true);
					return;
					}
				}
			}
			
			}
			out.writeObject("Not allowed to write or user not found");
			log.log(currentUser.getName(), "write"+name+" "+date, false);
		}
		
	

	private void read(String name, String date, ObjectOutputStream out) throws IOException {
		
			Record tempRecord =null;
			for (Record temp : db.getPatientRecords(name)) {
				if(temp.getDate().equals(date)){
					tempRecord = temp;
					break;
					
				}
			}
			if(tempRecord!=null){
				if(checkReadAccess(tempRecord)){
				out.reset();
				out.writeObject(tempRecord);
				log.log(currentUser.getName(),"read "+name+" "+date,true);
				return;
			}
			}
		out.writeObject("journal not found or not allowed");
		log.log(currentUser.getName(), "read"+name+" "+date, false);
}
	private boolean checkReadAccess(Record tempRecord) {
		if(currentUser instanceof Patient){
			return tempRecord.equals(currentUser.getName());
		
	}
		if(currentUser instanceof Government){
			return true;
		
	}else{ 
		if(currentUser.getDivision()==tempRecord.getDivision()){
			return true;
		}if(currentUser instanceof Doctor){
			return currentUser.getName().equals(tempRecord.getDoctor());
		
		}if(currentUser instanceof Nurse){
			return currentUser.getName().equals(tempRecord.getNurse());
		}
		return false;
	}
	}
	private boolean checkWriteAccess(Record tempRecord) {
		if(currentUser instanceof Doctor){
			return currentUser.getName().equals(tempRecord.getDoctor());
		}
		if(currentUser instanceof Nurse){
			return currentUser.getName().equals(tempRecord.getNurse());
		}
		return false;
	}
	private void list(String name, ObjectOutputStream out) throws IOException {
		// if ((currentUser instanceof Patient)) {
		// out.println("not a valid command!"); // also audit?
		// }
		
			StringBuilder sb = new StringBuilder();
			for (Record temp : db.getPatientRecords(name)) {
				if(checkReadAccess(temp)){
				sb.append(temp.getDate());
				sb.append("\n");
				}
			}
			
			out.writeObject(sb.toString());
			log.log(currentUser.getName(),"list "+name,true);
	

	}
	
	private void delete(String name, String date, ObjectOutputStream out) throws IOException{
		boolean deleted = false;
		if (currentUser instanceof Government){
			ArrayList<Record> recordsTemp = db.getPatientRecords(name);
			for (Record temp : recordsTemp ) {
				if(temp.getDate().equals(date)){
					deleted =recordsTemp.remove(temp);
					out.writeObject(name + " " + date + " deleted! " + deleted);
					saveDataBase();
					log.log(currentUser.getName(), "delete"+name+" "+date, true);
				}
				}
			
		}
		out.writeObject("Could not delete!");
		log.log(currentUser.getName(), "delete"+name+" "+date, false);
		
	}
	
	private void create(String name, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException{//added method to check really acceptable record
		ArrayList<Record> recordsTemp = db.getPatientRecords(name);
		String date = null;
		if (currentUser instanceof Doctor && recordsTemp!=null){
			out.writeObject("send new record");
			Object tempIn = in.readObject();
			out.writeObject("received");
			Record received = (Record)tempIn;
			Record temp = new Record(currentUser.getName(),received.getNurse(),currentUser.getDivision(),received.getDate(),received.getMedicalData());
			recordsTemp.add(temp);
			saveDataBase();
			date=received.getDate();
			log.log(currentUser.getName(),"create "+name+" "+date,true);
			
		}else{
		out.writeObject("Not allowed to create record!");
		log.log(currentUser.getName(),"create "+name+" "+date,false);
	}
	}



	private void newListener() {
	
		(new Thread(this)).start();
	} // calls run()

	public static void main(String args[]) {
		System.out.println("\nServer Started\n");
		int port = 9877;
		if (args.length >= 1) {
			port = Integer.parseInt(args[0]);
		}
		String type = "TLS";
		try {
			ServerSocketFactory ssf = getServerSocketFactory(type);
			ServerSocket ss = ssf.createServerSocket(port);
			((SSLServerSocket) ss).setNeedClientAuth(true); // enables client
															// authentication
			new Server(ss);
		} catch (IOException e) {
			System.out.println("Unable to start Server: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static ServerSocketFactory getServerSocketFactory(String type) {
		if (type.equals("TLS")) {
			SSLServerSocketFactory ssf = null;
			try { // set up key manager to perform server authentication
				SSLContext ctx = SSLContext.getInstance("TLS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				char[] password = "password".toCharArray();

				ks.load(new FileInputStream("certificates/Serverkeystore"), "Server".toCharArray()); // keystore
																						// password
																						// (storepass)
				ts.load(new FileInputStream("certificates/servertruststore"), "password".toCharArray()); // truststore
																						// password
																						// (storepass)
				kmf.init(ks, "Server".toCharArray()); // certificate password (keypass)
				tmf.init(ts); // possible to use keystore as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				ssf = ctx.getServerSocketFactory();
				return ssf;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return ServerSocketFactory.getDefault();
		}
		return null;
	}
}
