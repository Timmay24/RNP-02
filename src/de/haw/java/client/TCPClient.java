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
import java.util.Scanner;

public class TCPClient {
    /* Portnummer */
    private final int serverPort;

    /* Hostname */
    private final String hostname;

    private Socket clientSocket; // TCP-Standard-Socketklasse

    private DataOutputStream outToServer; // Ausgabestream zum Server
    private BufferedReader inFromServer; // Eingabestream vom Server

    private boolean serviceRequested = true; // Client beenden?

    public TCPClient(String hostname, int serverPort) {
        this.serverPort = serverPort;
        this.hostname = hostname;
    }

    public void startJob() {
        /* Client starten. Ende, wenn quit eingegeben wurde */
        Scanner inFromUser;
        String sentence; // vom User uebergebener String
        String modifiedSentence; // vom Server modifizierter String

        try {
            /* Socket erzeugen --> Verbindungsaufbau mit dem Server */
            clientSocket = new Socket(hostname, serverPort);

            /* Socket-Basisstreams durch spezielle Streams filtern */
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));

            /* Konsolenstream (Standardeingabe) initialisieren */
            inFromUser = new Scanner(System.in);

            System.out.println("Bitte wählen Sie einen Nutzernamen mit /login <nickname> aus");

            while (serviceRequested) {
                /* String vom Benutzer (Konsoleneingabe) holen */
                sentence = inFromUser.nextLine();

                /* String an den Server senden */
                writeToServer(sentence);

                /* Modifizierten String vom Server empfangen */
                modifiedSentence = readFromServer();

                /* Test, ob Client beendet werden soll */
                if (modifiedSentence.toLowerCase().startsWith("/quit")) {
                    writeToServer("/quit");
                    serviceRequested = false;
                }
            }

            /* Socket-Streams schliessen --> Verbindungsabbau */
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by server!");
        }
        System.out.println("TCP Client stopped!");
    }

    private void writeToServer(String request) throws IOException {
        /* Sende eine Zeile (mit CRLF) zum Server */
        outToServer.writeBytes(request + '\r' + '\n');
    }

    private String readFromServer() throws IOException {
        /* Lies die Antwort (reply) vom Server */
        String reply = inFromServer.readLine();
        System.out.println(reply);
        return reply;
    }

    public static void main(String[] args) {

        String host = "localhost";
        int port = 666;

        if (args.length == 2) {
            host = args[0];
            port = new Integer(args[1]);
        }

        /* Test: Erzeuge Client und starte ihn. */
        TCPClient myClient = new TCPClient(host, port);
        myClient.startJob();
    }
}
