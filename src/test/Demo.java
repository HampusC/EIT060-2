package test;
import server.Server;
import client.Client;
public class Demo {

			public static void main(String[] args) {
				Server1 s = new Server1();
				s.start();
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Client1 c = new Client1();
				c.start();
			
			
			}
			
			private static class Server1 extends Thread {
				public void run() {
					Server.main(new String[] {"9877"});
				}
			}
			
		
			
			private static class Client1 extends Thread {
				public void run() {
					try {
						Client.main(new String[] {"localhost" , "9877"});
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}


	}


