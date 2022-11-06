package dev.akuniutka.skillfactory.netchat;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;


public class ChatServer implements Runnable {

    private final ServerSocket serverSocket;
    private final List<Connection> connections = new LinkedList<>();

    private class ConnectionStarter implements Runnable {
        private static final String WELCOME_MESSAGE = "Welcome to our chat!\nWhat is your name?";
        private static final String GREETING = "Hi, %s!\nSend 'bye' when you want to leave.\nJoining...\n";
        private static final String ANONYMOUS = "(Anonymous)";
        private final Socket socket;

        public ConnectionStarter(Socket clientSocket) throws IOException {
            this.socket = clientSocket;
        }

        @Override
        public void run() {
            try {
                Scanner in = new Scanner(socket.getInputStream());
                PrintStream out = new PrintStream(socket.getOutputStream());
                out.println(WELCOME_MESSAGE);
                String userName = in.nextLine();
                if (userName.equals("")) {
                    userName = ANONYMOUS;
                }
                out.printf(GREETING, userName);
                Connection connection = new Connection(socket, in, out, userName);
                connections.add(connection);
                new Thread(connection).start();
                sendToAll(userName + " joined the chat");
                System.out.println("A new connection [" + userName + "] started.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Connection implements Runnable {
        private final Socket socket;
        private final Scanner in;
        private final PrintStream out;
        private final String userName;

        public Connection(Socket clientSocket, Scanner in, PrintStream out, String userName) {
            this.socket = clientSocket;
            this.in = in;
            this.out = out;
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }

        public void send(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            String message;
            do {
                message = in.nextLine();
                sendFrom(this, message);
            } while (!message.equals("bye"));
            dropConnection(this);
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            System.out.println("Waiting for a new connection...");
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Starting a new connection...");
                new Thread(new ConnectionStarter(clientSocket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToAll(String message) {
        sendFrom(null, message);
    }

    public void sendFrom(Connection from, String message) {
        for (Connection connection : connections) {
            if (from == null) {
                connection.send("[" + message + "]");
            } else if (!connection.equals(from)) {
                connection.send(from.getUserName() + ": " + message);
            }
        }
    }

    public void dropConnection(Connection connection) {
        sendToAll(connection.getUserName() + " left the chat");
        connection.close();
        connections.remove(connection);
        System.out.println("A user [" + connection.getUserName() + "] disconnected.");
    }

}
