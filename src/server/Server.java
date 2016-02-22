package server;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.ArrayList;

import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

import types.Patient;
import types.Record;
import types.User;

public class Server implements Runnable {
	private ServerSocket serverSocket = null;
	private static int numConnectedClients = 0;
	private DataBase db;
	private User currentUser; // remeber to set usetr accordign to certificate
	private AuditLog log;

	public Server(ServerSocket ss) throws IOException {
		serverSocket = ss;
		newListener();
		db = new DataBase();
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
			String issuer = cert.getIssuerDN().getName();
			String serial = cert.getSerialNumber().toString();
			numConnectedClients++;
			System.out.println("client connected");
			System.out.println("client name (cert subject DN field): " + subject);
			System.out.println("certificate issuer (issuer DN field) on certificate received :\n" + issuer + "\n");
			System.out.println(
					"certificate serial number (serial number field) on certificate received:\n" + serial + "\n");

			System.out.println(numConnectedClients + " concurrent connection(s)\n");
			currentUser = db.findUser("kim"); // should be subject
			// if(currentUser!=null){ //ta hand om när null

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

	private void interpretMessage(String msg, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException { // kom ihåg,
																	// vad
																	// händer om
																	// read
																	// göran
																	// knutsson
		System.out.println(msg);
		String msgParts[] = msg.split(": ");
		for (String temp : msgParts) {
			System.out.println(temp);
		}
		
		if (msgParts[0].equals("list")) {
			list(msgParts[1], out);
			log.log(currentUser.getName(),msgParts[0],msgParts[1]);
		} else if (msgParts[0].equals("read")) {
			read(msgParts[1], msgParts[2], out);
			log.log(currentUser.getName(),msgParts[0],msgParts[1]+" "+msgParts[2]);
		} else if(msgParts[0].equals("write")){
			try {
				write(msgParts[1], msgParts[2], out, in);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			out.writeObject("failed to interpret");

		}
	}

	private void write(String name, String date, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
		if (checkAccess(name)) { 
			//se till att inte ok bara för rätt division
			
			ArrayList<Record> records = db.getPatientRecords(name);
			for (Record temp : records) {
				if(temp.getDate().equals(date)){
					
					out.writeObject("send data");
					System.out.println("innan wtrite ser4ver " + temp.getMedicalData());
					out.reset();
					out.writeObject(temp);
					Object tempIn =  in.readObject();
					if(tempIn instanceof Record){
				temp.setMedicalData(((Record)tempIn).getMedicalData());
				out.writeObject("received");
					break;
				}
			}
			
			}
			System.out.println(db.getPatientRecords(name).size() + " " + db.getPatientRecords(name).get(0).getMedicalData());
		}
		
	}

	private void read(String name, String date, ObjectOutputStream out) throws IOException {
		System.out.println(db.getPatientRecords(name).size() + " " + db.getPatientRecords(name).get(0).getMedicalData());
		if (checkAccess(name)) {
			Record tempRecord =null;
			for (Record temp : db.getPatientRecords(name)) {
				if(temp.getDate().equals(date)){
					tempRecord = temp;
					break;
				}
			}
			if(tempRecord!=null){
				System.out.println("before sned: " + tempRecord.getMedicalData());
				out.reset();
		out.writeObject(tempRecord);
				return;
			}
		}
		out.writeObject("journal not found or not allowed");

	}

	private void list(String name, ObjectOutputStream out) throws IOException {
		// if ((currentUser instanceof Patient)) {
		// out.println("not a valid command!"); // also audit?
		// }
		if (checkAccess(name)) {
			for (Record temp : db.getPatientRecords(name)) {
				out.writeObject(temp.getDate());
			}
		} else {
			out.writeObject("patient not found or not allowed"); // also audit log.
		}

	}

	private boolean checkAccess(String name) {

		return (currentUser.checkIfInPatientsList(name) || db.checkDivision(currentUser.getDivision(), name));
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

				ks.load(new FileInputStream("src/server/serverkeystore"), password); // keystore
																						// password
																						// (storepass)
				ts.load(new FileInputStream("src/server/servertruststore"), password); // truststore
																						// password
																						// (storepass)
				kmf.init(ks, password); // certificate password (keypass)
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
