package de.haw.java.chat.client;/*
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

	private final ChatUI ui;

	private final String name;

	private ClientThread clientThread;

    public TCPClient(String hostname, int serverPort, ChatUI ui, String name) {
        this.serverPort = serverPort;
        this.hostname = hostname;
		this.ui = ui;
		this.name = name;
    }

    public void startJob() {
            initializeConnectionToServer();
            authorizeWithServer();
    }

	private void authorizeWithServer() {
		this.writeToServer("Protocol: " + VERSION);
		String answer = "";
		try {
			answer = inFromServer.readLine();
		} catch (final IOException e) {
			ui.addInfoMessage("Connection aborted by server!");
		}
		
		if (answer.equals("ACCESS GRANTED")) {
			ui.addInfoMessage("Connection to server established.");
			
			initializeClientThread();
			
			this.writeToServer(ChatCommands.LOGIN + this.name);
		} else {
			ui.addInfoMessage("Server rejected connection:\r\n" + answer);
		}
	}

	private void initializeClientThread() {
		clientThread = new ClientThread();
		clientThread.start();
	}

	private void initializeConnectionToServer() {
		try {
			/* Socket erzeugen --> Verbindungsaufbau mit dem Server */
			clientSocket = new Socket(hostname, serverPort);
	
			/* Socket-Basisstreams durch spezielle Streams filtern */
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(
			        clientSocket.getInputStream()));
		} catch (final IOException e) {
            ui.addInfoMessage("Connection to server couldn't be established!");
        }
	}

    public void writeToServer(String request) {
        /* Sende eine Zeile (mit CRLF) zum Server */
        try {
			outToServer.write((request.replace("\n", "") + '\r' + '\n').getBytes("utf-8"));
            // needed replace("\n", "") since multiple \n in one sequence causes BufferedReader#readLine
            // to start over processing the next line which will be empty and therefore would lead into
            // causing /ERR_MALFORMED_CMD
		} catch (final IOException e) {
			ui.addInfoMessage("Failed to transmit message to server.");
		}
    }


    class ClientThread extends Thread {

        private boolean serviceRequested = true;

        public ClientThread() {
        	super();
        }

        @Override
		public void run() {

            int retryCounter = 0;
            final ServerResponseCommandFactory cf = ServerResponseCommandFactory.init(ui);

            while (serviceRequested) {
                try {
                    final String reply = inFromServer.readLine();
                    cf.processReply(new MessageFromServer(reply));
                    
                    retryCounter = 0;

                } catch (final IOException e) {
                    retryCounter++;
                    ui.addInfoMessage("Lost connection to server - trying to reconnect... " + retryCounter);
                    try {
                        sleep(3000);
                    } catch (final InterruptedException e1) {
                    }
                    if (retryCounter >= 3) {
                        serviceRequested = false;
                        /* Socket-Streams schliessen --> Verbindungsabbau */
                        try {
                            clientSocket.close();
                        } catch (final IOException e1) {
                        }
                        ui.addInfoMessage("Lost connection to server. Quitting.");
                    }
                }
            }
        }
    }

	public void shutDown() {
		try {
			this.clientSocket.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		clientThread.stop();
	}
}
