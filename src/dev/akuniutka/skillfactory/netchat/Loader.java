package dev.akuniutka.skillfactory.netchat;

import java.io.IOException;

public class Loader {
    public static void main(String[] args) {
        int port = 1234;
        try {
            new Thread(new ChatServer(port)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
