package client;

import java.net.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import types.Record;

import java.security.KeyStore;
import java.security.cert.*;
import java.util.Scanner;

/*
 * This example shows how to set up a key manager to perform client
 * authentication.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class Client {
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private JTextField medicalDataLabel;
	private JFrame f;

	public static void main(String[] args) throws Exception {
		String host = null;
		int port = -1;
		for (int i = 0; i < args.length; i++) {
			System.out.println("args[" + i + "] = " + args[i]);
		}
		if (args.length < 2) {
			System.out.println("USAGE: java client host port");
			System.exit(-1);
		}
		try { /* get input parameters */
			host = args[0];
			port = Integer.parseInt(args[1]);
			Client client = new Client(host, port);
		} catch (IllegalArgumentException e) {
			System.out.println("USAGE: java client host port");
			System.exit(-1);
		}

	}

	public Client(String host, int port) throws Exception {
		try { /* set up a key manager for client authentication */
			SSLSocketFactory factory = null;
			try {
				Scanner scan = new Scanner(System.in);
				System.out.println("Input the filepath to your certificate");
				String filePath = scan.nextLine();
				char[] password = "password".toCharArray();
				System.out.println("Input your password");
				String userPass = scan.nextLine();
				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				SSLContext ctx = SSLContext.getInstance("TLS");
				ks.load(new FileInputStream("src/client/clientkeystore"), password); // keystore
																						// password
																						// (storepass)
				ts.load(new FileInputStream("src/client/clienttruststore"), password); // truststore
																						// password
																						// (storepass);
				kmf.init(ks, userPass.toCharArray()); // user password (keypass)
				tmf.init(ts); // keystore can be used as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				factory = ctx.getSocketFactory();
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
			SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
			System.out.println("\nsocket before handshake:\n" + socket + "\n");

			/*
			 * send http request
			 *
			 * See SSLSocketClient.java for more information about why there is
			 * a forced handshake here when using PrintWriters.
			 */
			socket.startHandshake();

			SSLSession session = socket.getSession();
			X509Certificate cert = (X509Certificate) session.getPeerCertificateChain()[0];
			String subject = cert.getSubjectDN().getName();
			String issuer = cert.getIssuerDN().getName();
			String serial = cert.getSerialNumber().toString();
			System.out.println(
					"certificate name (subject DN field) on certificate received from server:\n" + subject + "\n");
			System.out.println(
					"certificate issuer (issuer DN field) on certificate received from server:\n" + issuer + "\n");
			System.out.println("certificate serial number (serial number field) on certificate received from server:\n"
					+ serial + "\n");
			System.out.println("socket after handshake:\n" + socket + "\n");
			System.out.println("secure connection established\n\n");

			BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
			out= new ObjectOutputStream(socket.getOutputStream());
			in= new ObjectInputStream(socket.getInputStream());
			String msg;
			for (;;) {
				System.out.print(">");
				msg = read.readLine();
				if (msg.equalsIgnoreCase("quit")) {
					break;
				}
				System.out.print("sending '" + msg + "' to server...");
				out.writeObject(msg);
				out.flush();
				System.out.println("done");
				Object response = in.readObject();
				String stringResponse;
				if(response instanceof String){
					stringResponse=(String) (response);
					System.out.println("received '" + stringResponse + "' from server\n");
					if(((String)stringResponse).equals("send data")){
						Record t= (Record)in.readObject();
						recieveJournal(t,true);
					}
				}
				else if (response instanceof Record) {
					recieveJournal((Record)response, false);
				} else {
					System.out.println("Didn't understand server response");
					
				}
			}
			in.close();
			out.close();
			read.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void recieveJournal(Record response, boolean editable) {
		System.out.println("in client " + response.getMedicalData());
		f = new JFrame("Journal Viewer");
		f.setSize(600, 400);

		JLabel dateLabel = new JLabel(response.getDate());

		JLabel infoLabel = new JLabel("Doctor: " + response.getDoctor()+ "     Nurse: " + response.getNurse());

		JLabel divLabel = new JLabel("Division: Div " + response.getDivision());

		medicalDataLabel = new JTextField();
		medicalDataLabel.setText(response.getMedicalData());
		medicalDataLabel.setBackground(Color.WHITE);
		medicalDataLabel.setBorder(BorderFactory.createLoweredBevelBorder());
		medicalDataLabel.setEditable(editable);

		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		infoPanel.add(dateLabel, BorderLayout.NORTH);
		infoPanel.add(divLabel, BorderLayout.CENTER);
		infoPanel.add(infoLabel, BorderLayout.SOUTH);
		infoPanel.setBackground(Color.RED);
		if(editable){
		JButton writeButton = new JButton("Write changes to server");
		writeButton.addActionListener(new WriteButtonActionlistener());
		f.add(writeButton, BorderLayout.SOUTH);
		}
		f.add(infoPanel, BorderLayout.NORTH);
		f.add(medicalDataLabel, BorderLayout.CENTER);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		

	}
	private class WriteButtonActionlistener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			if(medicalDataLabel!=null){
			try {
				out.writeObject(new Record("","",0,"", medicalDataLabel.getText()));
				String temp = (String)in.readObject(); // use for error messages if not "recieved"
				medicalDataLabel=null;
			} catch (IOException | ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
		
		if( f!=null){
			f.setVisible(false); //you can't see me!
			f.dispose(); //Destroy the JFrame object
			f=null;
		}
		
	}
	}

}
