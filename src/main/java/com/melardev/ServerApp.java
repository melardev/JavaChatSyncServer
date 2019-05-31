package com.melardev;

import com.melardev.chat.ClientThread;
import com.melardev.chat.db.JdbcService;
import com.melardev.chat.net.packets.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.melardev.chat.net.packets.PacketType.LOGIN_REQUEST;


public class ServerApp extends ServerSocket {

    private ArrayList<ClientThread> clients;
    private volatile boolean debug;

    private JdbcService jdbcService;

    public ServerApp(int port) throws IOException {
        super();

        setReuseAddress(true);
        bind(new InetSocketAddress(port));
        // this.conHandler = new Thread(this);
        clients = new ArrayList<ClientThread>();
        jdbcService = new JdbcService();
        jdbcService.initMySQLDb();
        // conHandler.start();
    }


    public void broadcast(PacketChat msg) {
        for (ClientThread client : clients) {
            client.send(msg);
        }
    }

    public void delete(ClientThread clientThread) {
        System.out.printf("%s has quited...\n", clientThread.getNickName());
        clients.remove(clientThread);
        broadcast(new PacketChatAnnounceDisconnect(clientThread.getNickName()));
    }

    public void handle(PacketChat packet, ClientThread clientThread) {

        if (packet.getPacketType() != LOGIN_REQUEST && !clientThread.isLoggedIn()) {
            // TODO: stop the socket and thread, user has intended to perform an operation
            // before authenticating
            return;
        }

        switch (packet.getPacketType()) {
            case LOGIN_REQUEST:
                String username = ((PacketLoginRequest) packet).getUsername();
                String inputPassword = ((PacketLoginRequest) packet).getPassword();
                String userDb = System.getenv("DB_USER");

                if (userDb == null)
                    userDb = "root";

                String passwordDb = System.getenv("DB_PASSWORD");
                if (passwordDb == null)
                    passwordDb = "";

                String hashedPassword = DigestUtils.sha1Hex(inputPassword);
                if (!jdbcService.validCredentials(username, hashedPassword)) {
                    clientThread.send(new PacketLoginResponse(false, null, "Invalid Credentials"));
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
                    Date date = new Date();
                    String lastConnected = sdf.format(date);

                    // String updateSql = "update users set `last-Connected`='" + lastConnected + "' where id=" + resultSet.getInt("id");
                    // statement.execute(updateSql);
                    clientThread.setNickName(username);
                    UUID uid = UUID.randomUUID();
                    clientThread.setUID(uid);
                    clientThread.send(new PacketLoginResponse(true, uid.toString(), "Successful login"));
                    clientThread.setLoggedIn(true);
                    broadcast(
                            new PacketAnnounce(clientThread.getNickName() + ":: has joint the chat room")
                    );

                    System.out.println("Got new authenticated connection " + clientThread.clientSocket.getInetAddress().getHostAddress());
                    clients.add(clientThread);
                    // PacketType.ANNOUNCE_JOIN
                    broadcast(new PacketUserList(getClientsMap(clients)));

                }
                break;
            case PUBLIC_MESSAGE:
                if (clientThread.isLoggedIn()) {
                    packet.setFrom(clientThread.getNickName());
                    System.out.println(clientThread.getNickName() + ": "
                            + ((PacketPublicMessage) packet).getMessage());
                    broadcast(packet);
                } else {
                    // TODO : log the event
                }
                break;
            case PRIVATE_MESSAGE:
                for (ClientThread client : clients) {
                    if (packet.getDestination().equals(client.getUID().toString())) {
                        client.send(new PacketPrivateMessage(((PacketPrivateMessage) packet).getMessage(),
                                clientThread.getNickName(),
                                packet.getDestination()));
                    }
                }
                break;
            case ANNOUNCE:
                break;
            case FILE_ATTACH:
                String from = clientThread.getUID().toString();
                ClientThread destClient = getClient(packet.getDestination());
                if (destClient != null) {
                    packet.setFrom(from);
                    packet.setDestination(destClient.getUID().toString());

                    destClient.send(packet);
                }
                break;
            case LOGIN_RESPONSE:
                break;
            default:
                break;
        }
    }

    private Map<String, String> getClientsMap(ArrayList<ClientThread> clients) {
        Map<String, String> result = new HashMap<>(clients.size());
        for (ClientThread user : clients) {
            result.put(user.getUID().toString(), user.getNickName());
        }
        return result;
    }

    private ClientThread getClient(String dst) {
        if (dst != null && !dst.isEmpty()) {
            for (ClientThread c : clients) {
                if (c.getUID().toString().equals(dst))
                    return c;
            }
        }
        return null;
    }

    public String getServerName() {
        return "server";
    }


    public static void main(String[] args) {

        try {
            ServerApp server = new ServerApp(3002);
            while (true) {
                try {
                    Socket client = server.accept();
                    // Create a thread handling this client, but do not add it to the list clients
                    // until this client authenticates successfully
                    ClientThread ct = new ClientThread(client, server);
                    ct.start();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
