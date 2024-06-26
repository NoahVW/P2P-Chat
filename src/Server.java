import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread{ 
    

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }
    
    @Override
    public void run(){
        try{
             server = new ServerSocket( 9999);
             pool = Executors.newCachedThreadPool();

            while (!done){
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        }
        catch (Exception e) {
            shutdown();
        }
    }

   

    public void broadcast(String message, String groupID){
        for(ConnectionHandler ch : connections) {
            if (ch != null) {
                if(ch.getGroupID().equals((groupID))){
                    ch.sendMessage(message);
                }
            }
       } 
    }
    
        
     public void shutdown(){
        try {
            done = true;
            pool.shutdown();
            if(!server.isClosed()){
                server.close();

            }
            for (ConnectionHandler ch : connections){
                ch.shutdown();
            }
        } catch (IOException e){
        
        }
    }


    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String groupID = "-1";

        public ConnectionHandler (Socket client) {
            this.client = client;


        }
        public String getGroupID() {

            while (groupID.equals("-1")) {}
            return groupID;
        }

            @Override
            public void run(){
                try {
                    out = new PrintWriter(client.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    
                    out.println("Please enter a Group Number: ");
                    groupID = in.readLine();
                    out.println("Please enter a nickname: ");
                    nickname = in.readLine();
                
                    System.out.println(nickname + " connected to Group: " + groupID);
                    broadcast(nickname + " joined the chat in group: " + groupID + "!", groupID);
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("/nickname")){
                            String[] messageSplit = message.split(" ", 2);
                            if (messageSplit.length == 2) {
                                broadcast(nickname + " renamed themselves to " + messageSplit[1], groupID);
                                System.out.println(nickname + " renamed themselves to " + messageSplit[1]);
                                nickname = messageSplit[1];
                                out.println("Successfully changed nickname to " + nickname);
                            
                            } else {
                                out.println("No Nickname Provided!");
                            }
                        } else if (message.startsWith("/quit")) {
                            
                            broadcast(nickname + " left the chat!", groupID);
                            shutdown();
                        
                        } else {
                            if (getGroupID().equals(groupID)) {
                                broadcast(nickname + ": " + message, groupID);

                            }    //"Group " + groupID + ": " + nickname + ": " + message
                        }

                    }

                } catch (IOException e) {
                    shutdown();
                }

            }    
            
            public void sendMessage(String message){
                    out.println(message);
    
            }    
            public void shutdown() {
                try {
                    in.close();
                    out.close();
                    if (!client.isClosed()) {
                    client.close();

                    }
                } catch (IOException e) {
                    // ignore
                }
            }
           
    
    }
        public static void main(String[] args) {
            Server server = new Server();
            server.run();


    }
}
