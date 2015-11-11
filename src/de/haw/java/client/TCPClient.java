package de.haw.java.client;/*
 * TCPClient.java
 *
 * Version 3.1
 * Autor: M. Huebner HAW Hamburg (nach Kurose/Ross)
 * Zweck: TCP-Client Beispielcode:
 *        TCP-Verbindung zum Server aufbauen, einen vom Benutzer eingegebenen
 *        String senden, den String in Grossbuchstaben empfangen und ausgeben
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPClient {
    private static final String VERSION = "Chat-0.0.1";

	/* Portnummer */
    private final int serverPort;

    /* Hostname */
    private final String hostname;

    private Socket clientSocket; // TCP-Standard-Socketklasse

    private DataOutputStream outToServer; // Ausgabestream zum Server
    private BufferedReader inFromServer; // Eingabestream vom Server

//    private Thread userInputThread;

	private ChatUI ui;

	private String name;

	private ClientThread clientThread;

    public TCPClient(String hostname, int serverPort, ChatUI ui, String name) {
        this.serverPort = serverPort;
        this.hostname = hostname;
		this.ui = ui;
		this.name = name;
    }

    public void startJob() {
        try {
            /* Socket erzeugen --> Verbindungsaufbau mit dem Server */
            clientSocket = new Socket(hostname, serverPort);

            /* Socket-Basisstreams durch spezielle Streams filtern */
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
            
            this.writeToServer("Protocol: " + VERSION);
            String awnser = inFromServer.readLine();
            
			if (awnser.equals("ACCESS GRANTED")) {
            	ui.addInfoMessage("Connection to server established.");
            	
            	clientThread = new ClientThread(this);
            	clientThread.start();
            	
            	this.writeToServer("/login " + this.name);
//            	userInputThread = new UserInput();
//            	userInputThread.start();
            	
            } else {
            	ui.addInfoMessage("Server rejected connection:\r\n" + awnser);
            }


        } catch (IOException e) {
            ui.addInfoMessage("Connection aborted by server!");
        }
    }

    public void writeToServer(String request) {
        /* Sende eine Zeile (mit CRLF) zum Server */
        try {
			outToServer.writeBytes(request + '\r' + '\n');
		} catch (IOException e) {
			ui.addInfoMessage("Failed to transmit message to server.");
		}
    }



    class ClientThread extends Thread {

        private TCPClient client;

        private boolean serviceRequested = true;

        public ClientThread(TCPClient client) {
            this.client = client;
        }

        public void run() {

            int retryCounter = 0;

            while (serviceRequested) {
                try {
                    String reply = inFromServer.readLine();
                    if (reply == null) {
                    	// server down
                    	throw new IOException();
                    } else if (reply.startsWith("/RENAMESUCCESS")) {
                    	ui.setName(reply.split(" ")[1]);
                    } else if (reply.startsWith("/LOGINSUCCESS")) {
                    	ui.setName(reply.split(" ")[1]);
                    } else {
                    	ui.addChatMessage(reply);
                    }
                    retryCounter = 0;

                } catch (IOException e) {
                    retryCounter++;
                    ui.addInfoMessage("Lost connection to server - trying to reconnect... " + retryCounter);
                    try {
                        sleep(3000);
                    } catch (InterruptedException e1) {
                    }
                    if (retryCounter > 3) {
                        serviceRequested = false;
                        /* Socket-Streams schliessen --> Verbindungsabbau */
                        try {
                            clientSocket.close();
                        } catch (IOException e1) {
                        }
                        ui.addInfoMessage("Lost connection to server. Quitting.");
                    }
                }
            }
        }
    }

//    class UserInput extends Thread {
//
//        public void run() {
//            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//            String input;
//            while(!isInterrupted()) {
//                try {
//                    // wait until we have data to complete a readLine()
//                    while (!br.ready() && !isInterrupted()) {
//                        Thread.sleep(200);
//                    }
//                    if (!isInterrupted()) {
//                        input = br.readLine();
//
//                        writeToServer(input);
//                    }
//
//                } catch (InterruptedException e) {
//                    this.interrupt();
//                } catch (IOException e) {
//                }
//            }
//        }
//    }

	public void shutDown() {
		try {
			this.clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientThread.stop();
	}
}
