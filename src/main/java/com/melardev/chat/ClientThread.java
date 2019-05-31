package com.melardev.chat;

import com.melardev.ServerApp;
import com.melardev.chat.net.packets.PacketChat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;


public class ClientThread extends Thread {

    public Socket clientSocket;
    private ServerApp server;
    private ObjectInputStream is;
    private ObjectOutputStream os;
    private String nickname;
    private boolean loggedIn;
    private UUID id;
    boolean isLoggedIn;

    public ClientThread(Socket clientSocket, ServerApp serverApp) {
        this.clientSocket = clientSocket;
        this.server = serverApp;
        this.isLoggedIn = false;
        loggedIn = false;
        try {
            is = new ObjectInputStream(this.clientSocket.getInputStream());
            os = new ObjectOutputStream(this.clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            try {
                PacketChat packet;
                try {
                    packet = (PacketChat) is.readObject();
                    server.handle(packet, this);
                } catch (ClassNotFoundException e) {
                    System.out.println("Inside class not found exception");
                    e.printStackTrace();
                    server.delete(this);
                    running = false;
                }

            } catch (IOException e) {
                System.out.println("Inside IOException exception");
                server.delete(this);
                running = false;
            }
        }
    }

    public void send(PacketChat msg) {
        try {
            os.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setNickName(String nickName) {
        this.nickname = nickName;
    }

    public String getNickName() {
        return nickname;
    }

    public void setLoggedIn(boolean b) {
        loggedIn = true;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public UUID getUID() {
        return id;
    }

    @Override
    public String toString() {
        return getNickName() + "=" + getUID().toString();
    }

    public void setUID(UUID randomUUID) {
        id = randomUUID;
    }

}
