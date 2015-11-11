package de.haw.java.server;
/*
* TCPServer.java
*
* <Bearbeitungsinfo!> TODO
*
* Version 3.1
* Autor: M. Huebner HAW Hamburg (nach Kurose/Ross)
* Zweck: TCP-Server Beispielcode:
*        Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine Anfrage bearbeitet:
*        einen String empfangen, in Grossbuchstaben konvertieren und zuruecksenden
*        Maximale Anzahl Worker-Threads begrenzt durch Semaphore
*
*/

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;


public class TCPServer {
/* TCP-Server, der Verbindungsanfragen entgegennimmt */

    /* Konstante für Umbrüche */
    public static final String CRLF  = "\r\n";

    /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads  */
    public Semaphore clientThreadsSem;

    /* Portnummer */
    public final int serverPort;

    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean serviceRequested = true;

    /* Löst Nutzernamen in ihre zugehörigen Threads auf */
    public final Map<String, ClientThread> nicknamesToClients;

    /* Liste aller aktiven Client-Threads */
    public final List<ClientThread> clients;


    public static void main(String[] args) {
        TCPServer myServer;
        int serverPort = 666;
        int maxClients = 100;

        if (args.length == 2) {
            serverPort = new Integer(args[0]);
            maxClients = new Integer(args[1]);
        }
        myServer = new TCPServer(serverPort, maxClients);
        myServer.startServer();
    }

    /**
     * Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler Worker-Threads
     */
    public TCPServer(int serverPort, int maxThreads) {
        this.serverPort = serverPort;
        this.clientThreadsSem = new Semaphore(maxThreads);
        clients = new ArrayList<>();
        nicknamesToClients = new HashMap<>();
    }

    /**
     * Startet den Chat-Server
     */
    public void startServer() {
        ServerSocket welcomeSocket;   // TCP-Server-Socketklasse
        Socket connectionSocket;      // TCP-Standard-Socketklasse

        int nextThreadNumber = 0;

        try {
            /* Server-Socket erzeugen */
            welcomeSocket = new ServerSocket(serverPort);

            // TODO Thread Routine für Prüfung, ob Clientverbindungen noch aktiv sind, einbauen (Polling über alle Sockets, Conn.State abfragen)

            while (serviceRequested) {
                clientThreadsSem.acquire();  // Blockieren, wenn max. Anzahl Worker-Threads erreicht

                System.out.println("Chat-Server wartet auf Verbindung auf Port " + serverPort);
                /*
                * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
                * Standard-Socket erzeugen und an connectionSocket zuweisen
                */
                connectionSocket = welcomeSocket.accept();

                System.out.println("Eingehende Verbindung von " + connectionSocket.getInetAddress() + " gebunden an Port " + connectionSocket.getPort());

                /* Neuen Client-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt uebergeben */
                ClientThread newClientThread = new ClientThread(++nextThreadNumber, connectionSocket, this);
                clients.add(newClientThread);
                newClientThread.start();

                /* AUTHORISIERUNG (BZW. WAHL DES NUTZERNAMENS) ERFOLGT GESONDERT NACH EINRICHTUNG VON SOCKET UND THREAD  */
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    /** OPERATIONS **/

    /**
     * Sendet die eine Servernachricht an alle Clients
     * @param message Zu versendende Nachricht
     * @throws IOException
     */
    public void notifyClients(String message) throws IOException {
        for (ClientThread recipient : clients) {
            if (recipient.isAuthorized()) {
                recipient.writeServerMessageToClient(message);
            }
        }
        log("SERVER: " + message);
    }

    /**
     * Sendet eine Client-Nachricht an alle anderen Clients auf dem Server
     * @param message Zu sendende Nachricht
     * @param sender Absender
     * @throws IOException
     */
    public void notifyClients(String message, ClientThread sender) throws IOException {
        for (ClientThread recipient : clients) {
            if (!recipient.equals(sender) && recipient.isAuthorized()) {
                recipient.writeToClient(sender.getNickname() + ": " + message);
            }
        }
        log(sender.getNickname() + ": " + message);
    }

    /**
     * Sendet eine private Nachricht eines Clients an einen bestimmten Client (Flüstern)
     * @param message Zu sendene Nachricht
     * @param sender Absender
     * @param recipientNickname Geheimer Empfänger
     * @throws IOException
     */
    public void whisperToClient(String message, ClientThread sender, String recipientNickname) throws IOException {
        if (userExists(recipientNickname)) {
            nicknamesToClients.get(recipientNickname).writeToClient(sender.getNickname() + " <Privat>: " + message);
            log(sender.getNickname() + " whispers to " + recipientNickname + ": " + message);
        }
    }

    /**
     * Registriert einen neu angemeldeten Client am Server zur Nutzung des Chats
     * @param inNickname   Gewünschter Nutzername
     * @param clientThread Threadreferenz des anfragenden Clients
     * @return true, falls Nutzer erfolgreich beigetreten ist.
     */
    public boolean registerUser(String inNickname, ClientThread clientThread) throws IOException {
        if (!userExists(inNickname)) {
            nicknamesToClients.put(inNickname, clientThread);
            clientThread.setNickname(inNickname);
            onClientLogin(clientThread);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Entfernt und deregistriert einen Nutzer aus dem Chat
     * @param client Thread des zu entfernenden Nutzers
     * @return true, falls Nutzer erfolgreich entfernt werden konnte
     */
    public boolean removeUser(ClientThread client) {
        clients.remove(client);
        nicknamesToClients.remove(client.getNickname());
        try {
            onClientLogout(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientThreadsSem.release();
        return true;
    }

    public boolean removeUser(String nickname) {
        if (userExists(nickname)) {
            return removeUser(nicknamesToClients.get(nickname));
        } else {
            return false;
        }
    }

    private void log(String message) {
        System.out.println(message);
    }

    /** EVENTS **/

    /**
     * Benachrichtigt alle Clients über den Beitritt eines Clients
     * @param client Client, der beitritt
     * @throws IOException
     */
    public void onClientLogin(ClientThread client) throws IOException {
        notifyClients(client.getNickname() + " betritt den Raum.");
    }

    /**
     * Benachrichtigt alle Clients über das Verlassen eines Clients
     * @param client Client, der den Server verlässt
     * @throws IOException
     */
    public void onClientLogout(ClientThread client) throws IOException {
        notifyClients(client.getNickname() + " verlässt den Raum.");
    }

    /**
     * Behandelt eine Umbenennungsanfrage eines Clients und führt die Umbenennung durch
     * @param desiredNickname Gewünschter Nutzername
     * @param clientThread    Threadreferenz des Nutzers, der sich umbenennen möchte
     * @return true, falls Umbenennung erfolgreich
     */
    public boolean onClientRename(String desiredNickname, ClientThread clientThread) throws IOException {
        if (!userExists(desiredNickname)) {
            nicknamesToClients.put(desiredNickname, clientThread); // neuen Namen anmelden
            String formerNickname = clientThread.getNickname();
            nicknamesToClients.remove(formerNickname); // alten Namen entfernen
            clientThread.setNickname(desiredNickname);
            notifyClients("SERVER: " + formerNickname + " hat sich zu " + desiredNickname + " umbenannt.");
            return true;
        }
        return false;
    }

    /** PREDICATES **/

    /**
     * Prüft, ob ein Nutzername bereits vergeben ist
     * @param nickname Zu prüfender Nutzername
     * @return true, falls Nutzername bereits vergeben
     */
    public boolean userExists(String nickname) {
        return nicknamesToClients.keySet().contains(nickname);
    }

    /** GETTER & SETTER **/

    /**
     * Gibt alle angemeldeten Clients mit Nutzernamen zurück
     * @return Alle angemeldeten Clients mit Nutzernamen
     */
    public String getUserlist() {
        String result = "";
        for (String client : nicknamesToClients.keySet()) {
            result += client + CRLF;
        }
        return result;
    }

    /**
     * Gibt an, ob der übergebene Nutzername zulässig ist.
     * @param inNickname Zu prüfender Nutzername
     * @return true, falls Nutzername zulässig ist
     */
    public boolean isValidNickname(String inNickname) {
        // TODO mocked
        return !inNickname.isEmpty();
    }
}

// ----------------------------------------------------------------------------

class ClientThread extends Thread {
    private static final String CRLF = "\r\n";
    /*
        * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
        * erhaelt
        */
    private int name;
    private Socket socket;
    private TCPServer server;
    private String nickname;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean clientServiceRequested = true; // Arbeitsthread beenden?

    //   private ClientState clientState;
    private boolean isAuthorized;

    private int logcount;
    /**
     * Konstruktor
     * @param num Thread-Nummer
     * @param socket Zugehöriger Socket
     * @param server Serverobjekt
     */
    public ClientThread(int num, Socket socket, TCPServer server) {
        this.name = num;
        this.socket = socket;
        this.server = server;
        isAuthorized = false;
    }

    private void log(String message) {
        System.err.println(++logcount + ": " + message);
    }

    public void run() {

        try {
        /* Socket-Basisstreams durch spezielle Streams filtern */
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());


            /* HAUPTSCHLEIFE */
            while (clientServiceRequested) {
            /* Eingehende Nachricht vom Client einlesen und verarbeiten */
                String inMessage = readFromClient();
                String commandString = "";
                String outMessage = "";

                Scanner s = new Scanner(inMessage).useDelimiter(" ");

                if (s.hasNext()) {
                    commandString += s.next();

                    /* Prüfen, ob Client bereits zur Chat-Kommunikation authorisiert ist */
                    if (isAuthorized) {

                        /* Prüfen, ob Nachricht überhaupt ein gültiger Befehl ist */
                        if (isCommand(commandString)) {
                            /* Prüfen, um welchen Befehl es sich handelt */
                            if (isCommand("w", commandString)) {
                                if (s.hasNext()) {
                                    String recipient = s.next();
                                    outMessage = inMessage.substring((commandString + recipient).length() + 1);
                                    server.whisperToClient(outMessage, this, recipient);
                                } else {
                                    writeServerMessageToClient("Fehler: Empfänger fehlt. /w <recipient nickname> <message>");
                                }
                            } else if (isCommand("poke", commandString)) {
                                // TODO poke da shit outa some1
                                todo();
                            } else if (isCommand("help", commandString)) {
                                // TODO show help list
                                todo();
                            } else if (isCommand("users", commandString) || (isCommand("list", commandString)))  {
                                writeServerMessageToClient("Angemeldete Nutzer:" + CRLF + server.getUserlist());
                            } else if (isCommand("rename", commandString)) {
                                // TODO renaming
                                todo();
                            } else if (isCommand("logout", commandString) || isCommand("quit", commandString)) {
                                clientServiceRequested = false;
                            }
                        // kein spezieller Befehl empfangen -> normale Nachricht an alle auf dem Server
                        } else {
                            outMessage += commandString;
                            while (s.hasNext()) { outMessage += " " + s.next(); }
                            server.notifyClients(outMessage, this);
                        }

                    // Client ist noch nicht authorisiert
                    } else {
                        // Anmelde-Befehl empfangen
                        if (isCommand("login", commandString)) {
                            String inNickname = "";
                            if (s.hasNext()) {
                                // Nickname Parameter einlesen
                                inNickname = s.next();
                                if (server.isValidNickname(inNickname)) {
                                    if (server.registerUser(inNickname, this)) {
                                        writeServerMessageToClient("Anmeldung erfolgreich. Hallo " + inNickname + "!");
                                        isAuthorized = true;
                                    }
                                // falls Nickname nicht zulässig
                                } else {
                                    writeServerMessageToClient("Fehler: Nickname nicht zulässig. " + inNickname);
                                    isAuthorized = false;
                                }
                            // falls kein Parameter angegeben wurde
                            } else {
                                writeServerMessageToClient("Fehler: Nickname erwartet. /login <nickname>");
                                isAuthorized = false;
                            }
                        // falls nicht, wie erforderlich, der Anmelde-Befehl empfangen wurde
                        } else {
                            log(commandString);
                            if (isCommand("quit", commandString)) {
                                clientServiceRequested = false;
                            } else {
                                writeServerMessageToClient("Fehler: Sie müssen zuerst einen Nutzernamen wählen. /login <nickname>"
                                        + CRLF + "Andernfalls müssen Sie /quit nutzen, um das Programm zu beenden.");
                            }
                            isAuthorized = false;
                        }
                    }
                }
            }

            /* Socket-Streams schliessen --> Verbindungsabbau */
            socket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by client!");
        } finally {
            System.out.println("TCP Worker Thread " + name + " stopped!");
            server.removeUser(this);
        }
        server.removeUser(this);
    }

    /**
     * Liest eingehende Zeichen aus dem Puffer
     * @return Eingelesene Zeichenkette aus dem Puffer
     * @throws IOException
     */
    private String readFromClient() throws IOException {
        String request = inFromClient.readLine();
//        System.out.println("Thread#readFromClient: " + request); // todo debug
        return request;
    }

    /**
     * Schreibt eine Nachricht in den Puffer des Clients
     * @param outMessage Zu sendende Nachricht
     * @throws IOException
     */
    public void writeToClient(String outMessage) throws IOException {
        /* Sende den String als Antwortzeile (mit CRLF) zum Client */
        outToClient.writeBytes(outMessage + '\r' + '\n');
    }

    public void writeServerMessageToClient(String outMessage) throws IOException {
        writeToClient("SERVER: " + outMessage);
    }

    public void todo() throws IOException {
        writeServerMessageToClient("TODO - not yet implemented.");
    }

    /** PREDICATES **/

    /**
     * Prüft, ob eingehende Nachricht einen Befehl darstellt
     * @param inMessage Eingehende Nachricht
     * @return true, falls Nachricht ein Befehl ist.
     */
    private boolean isCommand(String inMessage) {
        return inMessage.startsWith("/");
    }

    /**
     * Prüft, ob eingehende Nachricht einem bestimmten Befehl entspricht
     * @param inMessage Eingehende Nachricht
     * @param command   Erwarteter Befehl
     * @return true, falls erwarteter Befehl empfangen
     */
    private boolean isCommand(String command, String inMessage) {
        return inMessage.toLowerCase().startsWith("/" + command.toLowerCase());
    }

    /** GETTER & SETTER **/

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    /** OPERATIONS **/

    public boolean isAuthorized() {
        return isAuthorized;
    }
}
