package client;

import java.net.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import types.Record;

import java.security.KeyStore;
import java.security.cert.*;
import java.util.Arrays;
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
	private JFrame passwordFrame;
	private JPasswordField passwordField;
	private String filePath;
	private String host;
	private int port;
	private JTextField createDateField;
	private JTextField createDivField;
	private JTextField createMedField;
	private JTextField createNurseField;
	private JFrame journalCreator;

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
		this.port = port;
		this.host = host;
		Scanner scan = new Scanner(System.in);
		System.out.println("Input the filepath to your certificate");
		filePath = scan.nextLine();
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter a password:");
		JPasswordField pass = new JPasswordField(10);
		panel.add(label);
		panel.add(pass);
		String[] options = new String[] { "OK", "Cancel" };
		char[] password = "".toCharArray();
		int option = JOptionPane.showOptionDialog(null, panel, "Password", JOptionPane.NO_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		if (option == 0) // pressing OK button
		{
			password = pass.getPassword();
			System.out.println("Your password is: " + new String(password));
		}

		SSLSocket socket = trySocket(password);
		if (socket != null) {
			System.out.println("Success! You typed the right password.");

			run(socket);
		} else {
			JOptionPane.showMessageDialog(null, "Invalid password. Try again.", "Error Message",
					JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

	}

	private SSLSocket trySocket(char[] userPass) {
		/* set up a key manager for client authentication */
		try {
			SSLSocketFactory factory = null;
			try {

				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
				SSLContext ctx = SSLContext.getInstance("TLS");
				String completeFilePath = "certificates/" + filePath + "keystore";
				ks.load(new FileInputStream(completeFilePath), userPass); // keystore
																			// password
																			// (storepass)
				ts.load(new FileInputStream("certificates/clienttruststore"), "password".toCharArray()); // truststore
				// password
				// (storepass);
				kmf.init(ks, userPass); // user password (keypass)
				tmf.init(ts); // keystore can be used as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				factory = ctx.getSocketFactory();
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
			SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

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
			return socket;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void run(SSLSocket socket) {
		try {

			BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			String msg;
			System.out.println("\nDon't forget to close the client when your are done!");
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
				if (response instanceof String) {
					stringResponse = (String) (response);
					System.out.println("received \n'" + stringResponse + "'\nfrom server\n");
					if (((String) stringResponse).equals("send data")) {
						Record t = (Record) in.readObject();
						recieveJournal(t, true);
					}
					if (((String) stringResponse).equals("send new record")) {
						createJournalWindow();
					}
				} else if (response instanceof Record) {
					recieveJournal((Record) response, false);
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
		f = new JFrame("Journal Viewer");
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		f.setSize(600, 400);

		JLabel dateLabel = new JLabel(response.getDate());

		JLabel infoLabel = new JLabel("Doctor: " + response.getDoctor() + "     Nurse: " + response.getNurse());

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
		if (editable) {
			JButton writeButton = new JButton("Write changes to server");
			writeButton.addActionListener(new WriteButtonActionlistener());
			f.add(writeButton, BorderLayout.SOUTH);
		}
		f.add(infoPanel, BorderLayout.NORTH);
		f.add(medicalDataLabel, BorderLayout.CENTER);
		f.setVisible(true);

	}

	private void createJournalWindow() {
		journalCreator = new JFrame("Create Journal");
		journalCreator.setLayout(new BorderLayout());
		journalCreator.setSize(600, 400);
		journalCreator.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		JPanel infoPanel = new JPanel();

		infoPanel.setLayout(new BorderLayout());
		JLabel dateLabel = new JLabel("The Date: ");
		createDateField = new JTextField();
		createDateField.setPreferredSize(new Dimension(400, 50));
		JPanel datePanel = new JPanel();
		datePanel.add(dateLabel);
		datePanel.add(createDateField);
		infoPanel.add(datePanel, BorderLayout.NORTH);

		JLabel nurseLabel = new JLabel("Nurse:      ");
		createNurseField = new JTextField();
		createNurseField.setPreferredSize(new Dimension(400, 50));
		JPanel nursePanel = new JPanel();
		nursePanel.add(nurseLabel);
		nursePanel.add(createNurseField);
		infoPanel.add(nursePanel, BorderLayout.CENTER);
		journalCreator.add(infoPanel, BorderLayout.NORTH);

		JLabel medLabel = new JLabel("Medical Data: ");
		createMedField = new JTextField();
		createMedField.setPreferredSize(new Dimension(400, 200));
		JPanel medPanel = new JPanel();
		medPanel.setLayout(new BorderLayout());
		medPanel.add(medLabel, BorderLayout.NORTH);
		medPanel.add(createMedField, BorderLayout.SOUTH);
		journalCreator.add(medPanel, BorderLayout.CENTER);

		JButton createButton = new JButton("Create");
		createButton.addActionListener(new CreatenewRecordActionlistener());
		journalCreator.add(createButton, BorderLayout.SOUTH);

		journalCreator.setVisible(true);

	}

	private class WriteButtonActionlistener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (medicalDataLabel != null) {
				try {
					out.writeObject(new Record("", "", 0, "", medicalDataLabel.getText()));
					String temp = (String) in.readObject(); // use for error
															// messages if not
															// "recieved"
					medicalDataLabel = null;
				} catch (IOException | ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

			if (f != null) {
				f.setVisible(false); // you can't see me!
				f.dispose(); // Destroy the JFrame object
				f = null;
			}

		}
	}

	private class CreatenewRecordActionlistener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			try {
				out.writeObject(new Record("", createNurseField.getText(), 0, createDateField.getText(),
						createMedField.getText()));
				String temp = (String) in.readObject(); // use for error
														// messages if not
														// "recieved"
				createNurseField = null;
				createDateField = null;
				createMedField = null;
				createDivField = null;
			} catch (IOException | NumberFormatException | ClassNotFoundException e1) {
				try {
					out.writeObject("failed");
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}

			if (journalCreator != null) {
				journalCreator.setVisible(false); // you can't see me!
				journalCreator.dispose(); // Destroy the JFrame object
				journalCreator = null;
			}

		}
	}
}
