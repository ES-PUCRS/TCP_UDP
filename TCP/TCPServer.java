// Recebe um pacote de algum cliente
// Separa o dado, o endere?o IP e a porta deste cliente
// Imprime o dado na tela

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.io.*;

class TCPServer {
   public static void main(String args[]) throws Exception {
      new Server();
   }
}


class Server {

   /*
    *   Client class which defines client informations
    *   due to prevent space usage on Map structure
    */
   class Client{
      Client(int p, int t)
      { port = p; timer = t; }
      
      int port;
      int timer;

      @Override
      public String toString(){
         return "Port: " + port + " | Timer: " + timer;
      }
   }


   private final int serverPort = 9876;
   private final DatagramSocket socket;

   private Map<InetAddress, Client> register;
   private boolean enabled;


   Server () throws Exception {
      System.out.println("Server TCP rodando em: " + InetAddress.getByName("localhost") + ":" + serverPort);
      register = new HashMap<InetAddress, Client>();
      socket = new DatagramSocket(serverPort);
      ArrayList <String> list = new ArrayList();
      byte[] receiveData = new byte[1024];
      enabled = true;


      while(enabled) {
         Arrays.fill( receiveData, (byte) 0 );
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         socket.receive(receivePacket);
         
         received(receivePacket);

      }
      socket.close();
   }


   /*
    *   Order the request to corret method
    *   Needed because Java is ...
    */
   public void received(DatagramPacket packet) throws IOException {
      InetAddress IPAddress = packet.getAddress();
      int port = packet.getPort();
      String sentence = new String(
         Arrays.copyOf(
            packet.getData(),
            packet.getLength()
         )
      );

      System.out.println(
         "Mensagem recebida de "+
         IPAddress.toString() +":"+ port + " > " + 
         sentence
      );

      switch (sentence.toLowerCase()) {
         case "connect"    : connect    (packet); break;
         case "disconnect" : disconnect (packet); break;
         case "send"       : send       (packet); break;
         case "broadcast"  : broadcast  (packet); break;
         case "keepAlive"  : keepAlive  (packet); break;
         case "list"       : list       (packet); break;

         case "exit"       : exit       (packet); break;
         default:
            reply(packet, "400 Bad Request");
      }
   }

   /*  
    *   Generic Method reply
    *   Used due to responde the client with the message
    *   or the requested action status 
    *   Throws IOException
    */
   public void reply (DatagramPacket packet, String response) {
      try{
         socket.send(
            new DatagramPacket(
               response.getBytes() ,
               response.length()   ,
               packet.getAddress() ,
               packet.getPort()
            )
         );
      } catch (IOException ioe) {}
   }



   /*  
    *   Insert a new client on the list
    */
   public void connect    (DatagramPacket packet) {
      register.put(
         packet.getAddress(),
         new Client(
            packet.getPort(),
            0
         )
      );
      reply(packet, "200 OK");
   }

   /*  
    *   Remove the client of the list
    */
   public void disconnect (DatagramPacket packet) {
      register.remove(packet.getAddress());
      reply(packet, "200 OK");
   }

   // send -<<DESTINY IP>> "<<MESSAGE>>"
   public void send       (DatagramPacket packet) {
      // String message = 
      // reply(
      //    new DatagramPacket(
      //        null
      //       ,0
      //       ,0
      //       ,0
      //    ),
      //    message
      // );      
   }


   public void broadcast  (DatagramPacket packet) {
      String message = "" ;
      register.forEach((ip, client) -> {
         reply(
            new DatagramPacket(
                null
               ,0
               ,ip
               ,client.port
            ),
            message
         );
      });
   }


   public void keepAlive  (DatagramPacket packet) {}


   /*  
    *   List all registered clients on the server
    */
   public void list  (DatagramPacket packet) {
      reply(packet, register.toString());
   }

   /*  
    *   ! DevTool !
    *   Close the server
    */
   public void exit  (DatagramPacket packet) {
      enabled = false;
   }
}  