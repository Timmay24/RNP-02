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
    /* Portnummer */
    private final int serverPort;

    /* Hostname */
    private final String hostname;

    private Socket clientSocket; // TCP-Standard-Socketklasse

    private DataOutputStream outToServer; // Ausgabestream zum Server
    private BufferedReader inFromServer; // Eingabestream vom Server

    private Thread userInputThread;

    public TCPClient(String hostname, int serverPort) {
        this.serverPort = serverPort;
        this.hostname = hostname;
    }

    public void startJob() {
        try {
            /* Socket erzeugen --> Verbindungsaufbau mit dem Server */
            clientSocket = new Socket(hostname, serverPort);

            /* Socket-Basisstreams durch spezielle Streams filtern */
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));

            System.out.println("Bitte wählen Sie einen Nutzernamen mit /login <nickname> aus");

            ClientThread clientThread = new ClientThread(this);
            clientThread.start();

            userInputThread = new UserInput();
            userInputThread.start();

        } catch (IOException e) {
            System.err.println("Connection aborted by server!");
        }
    }

    private void writeToServer(String request) throws IOException {
        /* Sende eine Zeile (mit CRLF) zum Server */
        outToServer.writeBytes(request + '\r' + '\n');
    }

    public static void main(String[] args) {

        /* Standard Parameter */
        String host = "localhost";
        int port = 666;

        if (args.length == 2) {
            host = args[0];
            port = new Integer(args[1]);
        }

        /* Test: Erzeuge Client und starte ihn. */
        new TCPClient(host, port).startJob();
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
                    retryCounter = 0;
                    System.out.println(reply);

                } catch (IOException e) {
                    retryCounter++;
                    System.err.println("Lost connection to server - trying to reconnect... " + retryCounter);
                    try {
                        sleep(3000);
                    } catch (InterruptedException e1) {
                    }
                    if (retryCounter > 3) {
                        serviceRequested = false;
                        userInputThread.interrupt();
                        /* Socket-Streams schliessen --> Verbindungsabbau */
                        try {
                            clientSocket.close();
                        } catch (IOException e1) {
                        }
                        System.err.println("Lost connection to server. Quitting.");
                    }
                }
            }
        }
    }

    class UserInput extends Thread {

        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String input;
            while(!isInterrupted()) {
                try {
                    // wait until we have data to complete a readLine()
                    while (!br.ready() && !isInterrupted()) {
                        this.sleep(200);
                    }
                    if (!isInterrupted()) {
                        input = br.readLine();

                        try {
                            writeToServer(input);
                        } catch (IOException e) {
                            System.err.println("Failed to send message to server.");
                        }
                    }

                } catch (InterruptedException e) {
                    this.interrupt();
                } catch (IOException e) {
                }
            }
        }
    }
}
