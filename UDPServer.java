   import java.nio.charset.StandardCharsets;

   import java.io.ByteArrayOutputStream;

   import java.util.regex.Matcher;
   import java.util.regex.Pattern;
   import java.util.LinkedList;
   import java.nio.ByteBuffer;
   import java.util.ArrayList;
   import java.util.HashMap;
   import java.lang.Thread;
   import java.util.Arrays;
   import java.util.Random;
   import java.util.List;
   import java.util.Map;
   import java.net.*;
   import java.io.*;

   import libs.FileManager;
   import libs.Decompiler;
   import libs.Protocol;
   import libs.HTTP;

   class UDPServer {
      public static void main(String args[]) throws Exception {
         Server.getInstance();
      }
   }

   class Server {

   /*
   *   Client class which defines client informations
   *   due to prevent space usage on Map structure
   */
   class Client {
    Client(InetAddress ad, int dp, int cp, int t)
    { address=ad; dataPort=dp; controlPort=cp;
      timeOut=t; timedOutIn=t; 
      thread = new Thread(watch);
      thread.start();
      controlPacket =
        new DatagramPacket(
          "null".getBytes(), 4
         ,ad
         ,cp
      );
      dataPacket =
        new DatagramPacket(
          "null".getBytes(), 4
         ,ad
         ,dp
      );
        controlKey = dataKey = ad.toString().replace("/","") + ":";
        controlKey += cp;
        dataKey += dp;
    }

    DatagramPacket controlPacket;
    DatagramPacket dataPacket;
    InetAddress address;
    int controlPort;
    int dataPort;

    String controlKey;
    String dataKey;

    int timedOutIn;
    int timeOut;

    Thread thread;
    private Runnable watch = new Runnable() {
      boolean threadAlive;

      public void run() {
        threadAlive = true;
        while(timedOutIn > 0 && threadAlive) {
          timedOutIn--;
          // System.out.println(timedOutIn);
          try { thread.sleep(1000); }
          catch ( Exception e ) {}
        }
        Server
          .getInstance()
          .disconnect(
            new DatagramPacket(
                "null".getBytes(), 4
               ,address
               ,dataPort
            ),
            thread
          );
      }

      public void stop(){ threadAlive = false; }
    };

    @Override
    public String toString() {
      return "\n\tAddress: "     + address     +
             "\n\tData Port:  "  + dataPort    +
             "\n\tControl Port: "+ controlPort +
             "\n\tTime out: "    + timeOut     +
             "\n\tRemaining: "   + timedOutIn  +
             "\r\n";
    }
   }

   private Map<Integer, List<Integer>> keyHolder;
   private Map<Integer, Integer> keyPointer;

   private Map<Integer, HashMap<String, Client>> groups;
   private Map<Integer, Map<Integer, String>> aknowledgment;
   private Map<String, String> identification;
   private Map<Integer, String> dictionary;
   private Map<String, Boolean> queueMap;
   private Map<String, Client> register;
   private boolean heartBeat;
   private boolean enabled;

   private final int serverPort = 9876;
   private final DatagramSocket socket;

   private static Server instance;
   public static Server getInstance () {
      try {
         if(instance == null)
            instance = new Server();
      } catch (Exception e) {}
    return instance;
   }

   private Server () throws Exception {
      System.out.println("Server TCP rodando em: " + InetAddress.getByName("localhost") + ":" + serverPort);
      keyHolder = new HashMap<Integer, List<Integer>>();
      keyPointer = new HashMap<Integer, Integer>();

      groups = new HashMap<Integer, HashMap<String, Client>>();
      aknowledgment = new HashMap<Integer, Map<Integer, String>>();
      identification = new HashMap<String, String>();
      dictionary = new HashMap<Integer, String>();
      queueMap = new HashMap<String, Boolean>();
      register = new HashMap<String, Client>();
      socket = new DatagramSocket(serverPort);
      heartBeat = false;
      enabled = true;
      threeHandShake = false;
      queue = -1;
      cache = null;
      Thread server = new Thread(deploy);
      server.start();
   }

   private Runnable deploy = new Runnable() {
    public void run() {
      byte[] receiveData = new byte[1024];

      while(enabled) {
        Arrays.fill( receiveData, (byte) 0 );
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        
        try{
          socket.receive(receivePacket);
          received(receivePacket);
        } catch (Exception e) {}
      }
      
      socket.close();
    }
   };


   /*
   *   Order the request to corret method
   *   Needed because Java is ...
   *   Possible server calls:
   *    - connect
   *    - disconnect
   *    - join -<<GROUP ID>>
   *    - left -<<GROUP ID>>
   *    - send -<<DESTINY IP:PORT || GROUP ID>> "<<MESSAGE>>"
   *    - broadcast "MESSAGE"
   *    - keepalive -<<DATA PORT>>
   *    - list
   *    - listgroups
   *    - exit
   */
   private void received(DatagramPacket packet) throws IOException {
      if(packet.getLength() <= 0) return;
      int limitDisplay = 3;
      int seq = 0;
      Map<String, String> header = Decompiler.packetHeader(packet);
      try {
         if(header.get("ACK") == null)
            seq = Integer.parseInt(header.get("SEQ"));
         seq = Integer.parseInt(header.get("ACK"));   
      } catch (Exception e) {}

      String sentence = new String(
         Arrays.copyOf(
            packet.getData(),
            packet.getLength()
         )
      );

      String display = "" +
      packet
         .getAddress()
         .toString()
         + ":" + packet.getPort() +
         " (" + String.format("%04d",packet.getLength()) + ") > " + 
      sentence;

      if(sentence.toLowerCase().contains("keepalive") && !heartBeat) {} else {
         if(seq <= limitDisplay) {
            if (sentence.length() > 60)
               display = display.substring(0, 72) + "...]}";
            if (seq == limitDisplay )
               display = "\t\t\t...";
            System.out.println(display);
        }
      }


      switch (Decompiler.packetMethod(packet)) {
         // Client User
         case "connect"    : connect    (packet); break;
         case "disconnect" : disconnect (packet); break;
         case "join"       : join       (packet); break;
         case "left"       : left       (packet); break;
         case "send"       : send       (packet); break;
         case "help"       : help       (packet); break;
         case "broadcast"  : broadcast  (packet); break;
         // Client connection
         case "keepalive"  : keepAlive  (packet); break;
         case "register"   : register   (packet); break;
         case "groups"     : groups     (packet); break;
         case "exit"       : exit       (packet); break;
         case "syn"        : SYN        (packet); break;
         case "ack"        : ACK        (packet); break;
         case "fin"        : FIN        (packet); break;
         // Easter eggs
         case"brewcoffee"  : brewCoffee (packet); break;
         case"pudim.com.br": pudim      (packet); break;
         // None
         case "showheart"  : heartBeat = true ; break;
         case "hideheart"  : heartBeat = false; break;
         default:
            System.out.println(Decompiler.packetMethod(packet));
            reply(packet, HTTP.BAD_REQUEST);
      }
   }

   /*  
   *   Generic Method to reply
   *   Used due to responde the client with the message
   *   or the requested action status 
   *   Throws IOException
   */
   private void reply (Client client, HTTP response)                       { reply(client.dataPacket, response.toString()); }
   private void reply (Client client, String response)                     { reply(client.dataPacket, response.toString()); }
   private void reply (Client client, HTTP http, String response)          { reply(client.dataPacket, http.toString() + "\n" + response.toString()); }
   private void reply (DatagramPacket packet, HTTP http, String response)  { reply(packet, http.toString() + "\n" + response.toString()); }
   private void reply (DatagramPacket packet, HTTP response)               { reply(packet, response.toString()); }
   private void reply (DatagramPacket packet, String response) {
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
   *   Insert a new client on the server list
   */
   private void connect (DatagramPacket packet) {
      Map<String, String> header = Decompiler.packetHeader(packet);
      String clientKey = packetKey(packet);
   
      int dataPort = Integer.parseInt(header.get("DATA_PORT"));
      int timeout  = Integer.parseInt(header.get("TIME_OUT"));
          
      Client client = getRegister(clientKey);
      if(client == null) {
        client = new Client(
            packet.getAddress(),
            dataPort,
            packet.getPort(),
            timeout
        );
      } else {
         client.dataPort = dataPort;
         client.timedOutIn = timeout;
         client.timeOut = timeout;
      }
   
      identification.put(packetAddress(packet) + ":" + header.get("DATA_PORT"), clientKey);
      register.put(
         clientKey,
         client
      );
      reply(packet, HTTP.CREATED, "[\n\tClientKey: \'"+clientKey+"\'\n]");
   }


   /*  
    *   Remove the client of the server list
    */
   private void disconnect (DatagramPacket packet) {
      String clientKey = packetKey(packet);
      Client client = getRegister(clientKey);
      client.thread.stop();
      identification.remove(packetKey(client));
      register.remove(packetKey(client));
      reply(packet, HTTP.OK);
   }
   private void disconnect (DatagramPacket packet, Thread thread) {
      Client client = getRegister(packetKey(packet));
      identification.remove(packetKey(client));
      register.remove(packetKey(client));
      reply(packet, HTTP.TIME_OUT);
   }


   /*  
    *   Insert a new client on the group list
    */
   private void join (DatagramPacket packet) {
      if(validateUserControl(packet)) return;
      Integer groupID;
      try {
         groupID = Decompiler.packetDestinationNumber(packet);
      } catch (NumberFormatException e) {
         reply(packet, e.getMessage());
         return;
      }
   
      HashMap<String, Client> group = (HashMap) groups.get(groupID);
      Client client = getRegister(packet);
   
      if(group == null){
         groups.put(
            groupID,
            new HashMap<String, Client>() {{
               put(client.dataKey, client);
            }}
         );
      } else {
        group.put(client.dataKey, client);
      }
   
      reply(client, HTTP.OK);
   }

   /*  
    *   Remove the client of the group list
    */
   private void left (DatagramPacket packet) {
      if(validateUserControl(packet)) return;
      Integer groupID;
      try {
         groupID = Decompiler.packetDestinationNumber(packet);
      } catch (NumberFormatException e) {
         reply(packet, e.getMessage());
         return;
      }
   
      HashMap<String, Client> group = (HashMap) groups.get(groupID);
      group.remove(getRegister(packetKey(packet)).dataKey);
   
      reply(packet, HTTP.OK);
   }


   private void send (DatagramPacket packet) {
      if(validateUser(packet)) return;
      String dst = Decompiler.packetDestination(packet);
      String message = Decompiler.packetMessage(packet);
      String method = Decompiler.packetMethod(packet);
      Client srcClient = getRegister(packet);
      Map<String, Client> targetGroup = null;
      Client targetClient = null;
      int ack_id = 0;

      try { ack_id = Integer.parseInt(Decompiler.packetHeader(packet).get("ACK_ID")); }
      catch (Exception e) { }
   
      try { 
         targetClient = getRegister(dst);
         if (targetClient == null)
            targetGroup = groups.get
            (Decompiler.packetDestinationNumber(packet));
      } catch(Exception e) { }
   
      queue(method, packet, targetClient, targetGroup);
   
      if(targetClient != null){
         if(!method.equals("send")){
            if(method.equals("fin"))
                  queue--;
            requestSYN(targetClient, cache);
         } else reply(targetClient, message);
      } else if (targetGroup != null) {
         if(validateGroup(packet, targetGroup)) return;
         for (Map.Entry<String, Client> entry : targetGroup.entrySet()) {
            if (!srcClient.dataKey.equals(entry.getKey())) {
               if (!method.equals("send")) {
                  requestSYN(entry.getValue(), cache);
                  if (method.equals("fin")) queue--;
               } else { reply(entry.getValue(), message); }
            }
         }
      } else { reply(packet, HTTP.NOT_FOUND); return; }
      reply(packet, HTTP.OK);
   }


   private void broadcast (DatagramPacket packet) {
      if(validateUser(packet)) return;
      String message = Decompiler.packetMessage(packet);
      String method = Decompiler.packetMethod(packet);
      String clientKey = packetKey(packet);
      int ack_id = 0;

      try { ack_id = Integer.parseInt(Decompiler.packetHeader(packet).get("ACK_ID")); }
      catch (Exception e) { }
      
      queue(method, packet, null, register);

      for (Map.Entry<String, Client> entry : register.entrySet()) {
         if (!clientKey.equals(entry.getKey())) {
            if (!method.equals("broadcast")) {
               requestSYN(entry.getValue(), cache);
               if (method.equals("fin"))
                  if(!queueMap.containsKey(entry.getKey())) {
                     queueMap.put(entry.getKey(), true);
                     queue--;
                  }
            } else { reply(entry.getValue(), message); }
         }
      }
      reply(packet, HTTP.OK);
   }

   public void help (DatagramPacket packet) {
      reply(packet,
           "Server commands:"
         + "\nconnect"     + "\t\t" + "Connect the client with the server"
         + "\ndisconnect"  + "\t"   + "Remove the client of the server connections"
         + "\njoin"        + "\t\t" + "\'join -<<GROUP ID>>\' Will add the client on the group"
         + "\nleft"        + "\t\t" + "\'left -<<GROUP ID>>\' Will remove the client of group."
         + "\nsend"        + "\t\t" + "\'send -<<DESTINY IP:PORT||GROUP ID>> \"<<MESSAGE||FILE:>File_Name.ext>>\"\' Will send a message or file to the target"
         + "\nbroadcast"   + "\t"   + "\'broadcast \"<<MESSAGE||FILE:>File_Name.ext>>\"\' Will send to every server client the message wroten"
         + "\nshowheart"   + "\t"   + "Show all requests to health check"
         + "\nhideheart"   + "\t"   + "Hide all requests to health check"
      );
   }

   /*  
   *   Update client health check when called
   */
   private void keepAlive (DatagramPacket packet) {
      int clientDataPort = 0;
      try{ clientDataPort = Decompiler.packetDestinationNumber(packet); }
      catch(NumberFormatException nfe) {}

      Client client =
        getRegister(packet);
      if (client == null) {
        reply(packet, HTTP.TIME_OUT);
        return;
      }

      client.timedOutIn = client.timeOut;
   }

   /*  
   *   List all registered clients on the server
   */
   private void register (DatagramPacket packet) {
      if(validateUser(packet)) return;
      reply(packet, HTTP.FOUND, register.toString());
   }

   /*  
   *   List all groups on the server
   */
   private void groups (DatagramPacket packet) {
      if(validateUser(packet)) return;
      reply(packet, HTTP.FOUND, groups.toString());
   }

   /*  
    *   ! DevTool !
    *   Close the server
    */
   public void exit (DatagramPacket packet) {
      disconnect(packet);
      if(register.isEmpty())
         enabled = false;
   }


   /* --------------------------------------------- SYNC METHODS ---------------------------------------------*/


   /*
   *   This method is to start a syncronization with
   *   the client due to send bigger messages
   */
   private void requestSYN (Client client, int ack_id)         { requestSYN(client.controlPacket, ack_id); }
   private void requestSYN (DatagramPacket packet, int ack_id) {
      reply(
         packet,
         "SYN {"
            +  "ACK_ID:"+ack_id+", "
            +  "REQ:ALOC, "
            +  "FILE_NAME:"+dictionary.get(ack_id)
         +"}"
      );
   }

   /*
   *  This variable is used to store the number of clients which
   *  will receive the message or file. 
   *
   *  This file must contain the number of clients that will be
   *  reach with SYN command. 
   */
   Integer queue;
   /*
   *  The cache will be an memory to carry the ACK_ID to the send
   *  command, which will alocate the messate properly to the client
   *  or the desired group to deliver.
   */
   Integer cache;
   /*
   *  Three hand shake boolean is used to grantee that the server
   *  will not respond the destiny client with the SYN method twice  
   */
   Boolean threeHandShake;

   /*
   *   This method is called by the server response
   *   use to syncronize with the server patterns due to
   *   send the data or even request ACK to the server.
   */
   private void SYN (DatagramPacket packet) {
      if(validateUserControl(packet)) return;

      try {
         String req = Decompiler.packetHeader(packet).get("REQ");
         if(req.equals("REPL")){
            Map<String, String> header = Decompiler.packetHeader(packet);
            int ack_id, ack_id_replace;
            String res = "";
            ack_id_replace =  Integer.parseInt(header.get("ACK_ID_REPLACE"));
            ack_id = Integer.parseInt(header.get("ACK_ID"));

            if(aknowledgment.containsKey(ack_id) && ack_id_replace != ack_id) {
               ack_id = ack_id_replace;
               res = "ALOC";
            } else {
               aknowledgment.put(ack_id, aknowledgment.get(ack_id_replace));
               dictionary.put(ack_id, dictionary.get(ack_id_replace));
               keyPointer.put(ack_id, ack_id_replace);

               if (keyHolder.containsKey(ack_id_replace))
                 keyHolder.get(ack_id_replace).add(ack_id);
               else
                 keyHolder.put(ack_id_replace, new LinkedList<Integer>(Arrays.asList(ack_id)));

               res = "OK";
            }

            reply(packet,
             "SYN {"
            +   "ACK_ID:"  + ack_id  + ", "
            +   "REQ:"     + res
            +"}");
         } else
            reply(packet, Protocol.SYN(packet, aknowledgment, dictionary, null));
      } catch (Exception e) { e.printStackTrace(); }
   }

   /*
   *   ACK is used to aknowledgment of a file bigger than
   *   the connection can handle on a single send connect
   *   
   *   Also is called when the server is responding a client
   *   sequence request.
   */
   private void ACK (DatagramPacket packet) {
      if(validateUserControl(packet)) return;
      reply(packet, Protocol.ACK(packet, aknowledgment, null));
   }

   /*
   *   Is need to communicate that the SYN-ACK is finish
   *   also convert all the data received and prepare to
   *   send it to the destination.
   */
   private void FIN (DatagramPacket packet) {
      if(validateUserControl(packet)) return;
      Map<String, String> header = Decompiler.packetHeader(packet);
      String res = Protocol.FIN(packet, aknowledgment, null);
      try {
         int ack_id = Integer.parseInt(header.get("ACK_ID"));
         byte[] dst;
         try{ dst = ("FIN -"+header.get("DST").replace("^$",":")+" {ACK_ID:"+ack_id+"} ").getBytes(); }
         catch (Exception e) { dst = new byte[0]; }
         if(aknowledgment.containsKey(ack_id) && (threeHandShake == true || dst.length == 0)) {
            Map<Integer, String> ackBook = aknowledgment.get(ack_id);
            if (ackBook != null && (threeHandShake == true || dst.length == 0)) {

               if (queue == 0) {
                  int _ack_id = keyPointer.get(ack_id);
                  List<Integer> keyList = keyHolder.get(_ack_id);
                     queueMap.remove(_ack_id);
                  for(Integer key : keyList){
                     aknowledgment.remove(key);
                     dictionary.remove(key);
                     queueMap.remove(key);
                  }
                  cache = null;
                  queue = -1;
                  System.out.println("SYN-ACK FINISH");
               } else if(queue < 0) {
                  reply(packet, res); res = null;

                  ackBook = aknowledgment.get(ack_id);
                  byte[] file = Decompiler.toArray(Decompiler.merge(ackBook));
                  System.out.println("/SERVER \t\t> RECEIVED FILE BYTE SIZE: " + file.length);
                  ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                  buffer.write(dst); buffer.write(file);
                  threeHandShake = false;
                  cache = ack_id;
                  packet.setData(
                    buffer.toByteArray(),
                    0,
                    file.length + dst.length
                  );

                  if(header.get("DST").equals("broadcast"))
                     broadcast(packet);
                  else
                     send(packet);
               }
            }
         } else
            threeHandShake = true;
      } catch(Exception e) {
         e.printStackTrace();
         reply(packet, HTTP.BAD_REQUEST);
      }

      if(!res.isEmpty())
         reply(packet, res);
   }

   /*  
    *   This is used to calculate how many users the SYN
    *   Will be reaching due to be able to clear cache
    *   without missing a client delivery
    */
   private void queue (
      String method, DatagramPacket packet,
      Client targetClient, Map<String, Client> targetGroup ){
      if(method.equals("fin")){
         if(targetClient != null) queue = 1;
         else if(targetGroup != null)
            if (!validateGroup(packet, targetGroup))
               queue = targetGroup.size() - 1;
            
      }
   }


   /* --------------------------------------------- EASTER EGG ---------------------------------------------*/

   public void brewCoffee  (DatagramPacket packet) {
    reply(
      packet,
      HTTP.TEA_POT,
       "\n\t The server refuses the attempt to brew coffee with a teapot~~ \n"
      +"\n\t             ;,'  "
      +"\n\t     _o_    ;:;'  "
      +"\n\t ,-.'---`.__ ;    "
      +"\n\t((j`=====',-'     "
      +"\n\t `-\\     /       "
      +"\n\t    `-=-'         "
      );
   }

   public void pudim  (DatagramPacket packet) {
      if(validateUser(packet)) return;
      String msg =
       "\n+:+++:::+:+:::::::*@@@@@@@@@@@@@@@@@@@@@@@##@@@@@#############@@@@@@@@@@@@@@###"
      +"\n++++++++++++::++=@@@@@@@@@@@@@@@@@@@@@##@@######=###=#=#########@@@@@@@@@@@@#@#"
      +"\n+++++++++:+++++#@@@@@@@@@@@@@@@@@@@@@@@@@########======####=#####@@@@@@@@@@@@@#"
      +"\n+++++++++++++*#@@@@@@@@@@@@@@@@@@@#@@@@##############========######@@#@@@@@@@@#"
      +"\n++++++++++**=#@@@@@@@@@@@@@@@@@@@@@@@#######==##====###=#=======#=##@@@@@@@@@@#"
      +"\n+++++++**==##@@@@@@@@@@@@@@@@@@@@@@@@#####======================*==@@@@@@@@@@@#"
      +"\n+++++**==####@@@@@@@@@@@@@@@@@@@@@@@@#####====#########==#=======#@@@@@@@@@@@@#"
      +"\n+++**==#######@@@@@@@@@@@@@@@@@@@@@@@@@################@@@##==##@@@@@@@@@@@@###"
      +"\n+**==#########@@@@@@@@@@@@@@@@@@@@@@@@@@##################@@@@@@@@@@@@@@@@@####"
      +"\n*===###########@@@@@@@@@@@@@@@@@@@@@@####====############@@@@@@@@@@@@@@@@@@@#@@"
      +"\n==##############@@@@@@@@@@@@@@@@@####===****==###########@@@@@@@@@@@@@@@@@@@@@@"
      +"\n=#################@#@@@@@@@@@@####===*********=##########@@@@@@@@@@@@@@@@@@@@@@"
      +"\n#######################@@@@####==*************=#########@@@@@@@@@@@@@@@@@@@@@@@"
      +"\n#######################@#@###==*************==####@@@@@#@@@@@@@@@@@@@@@@@@@@@@@"
      +"\n#########################@##==************=##########@##@@@@@@@@@@@@@@@@@@@@@@@"
      +"\n###########################==**********==###############@@@@@@@@@@@@@@@@@@@@@##"
      +"\n####################==###===*********=##################@@@@@@@@@#@@@@#########"
      +"\n################=####==#====*****==##########################@@###@############"
      +"\n########################========###############################################"
      +"\n########################=======#######=########################################"
      +"\n#######################=#######*=#==##=########################################"
      +"\n#################################=*==####=#################=###################"
      +"\n=################################===###################=#####################@#"
      +"\n==##########################==#########################=####################=##"
      +"\n===#####################################################=######################"
      +"\n=====##########################################################################"
      +"\n=======##########################################################=#############"
      +"\n=========######################################################################"
      +"\n=*=========####################################################################"
      +"\n=***=========##################################################################"
      +"\nImagine um pudim aqui##########################################################";

      int ack_id = cache = Protocol.genAckID(aknowledgment);
      try {
         dictionary.put(ack_id, "display");
         aknowledgment.put(ack_id, FileManager.split(msg.getBytes()));
      } catch (IOException e) { System.out.println(e); }
      queue = 0;
      requestSYN(getRegister(packet), ack_id);
   }



   /* --------------------------------------------- ASSISTANT METHODS ---------------------------------------------*/



   private Client getRegister (DatagramPacket packet) { return getRegister( packetKey(packet)); }
   private Client getRegister (String clientKey)      {
      if(identification.containsKey(clientKey))
         return register.get(identification.get(clientKey));
      return register.get(clientKey);
   }


   private String packetAddress(DatagramPacket packet){
    return
      packet.getAddress()
            .toString()
            .replace("/","");
   }

   /* Used to broke the packet into ClientKey */
   private String packetKey (Client client) {
      return 
         client.address.toString().replace("/","") + ":" + client.controlPort;
   }
   private String packetKey (DatagramPacket packet) {
      return (packetAddress(packet) + ":" + packet.getPort());
   }

   /* Grantee that the user is connected to the server */
   private boolean validateUserControl (DatagramPacket packet) {
      Client client = getRegister(packetKey(packet));
      boolean valid = client == null || client.controlPort != packet.getPort();
      if(valid) reply(packet, HTTP.UNAUTHORIZED);
      return valid; 
   }

   private boolean validateGroup (DatagramPacket packet, Map<String, Client> group) {
      boolean   valid = !group.containsKey(getRegister(packetKey(packet)).controlKey);
      if(valid) valid = !group.containsKey(getRegister(packetKey(packet)).dataKey);
      if(valid) reply(packet, HTTP.UNAUTHORIZED);
      return valid;
   }

   private boolean validateUser (DatagramPacket packet) {
      boolean valid = validateUser(packetKey(packet));
      if(valid) reply(packet, HTTP.UNAUTHORIZED);
      return valid;
   }
   private boolean validateUser (String clientKey) {
      return getRegister(clientKey) == null;
   }
}