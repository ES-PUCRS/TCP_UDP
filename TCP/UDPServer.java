/*! UNCOMMENT  LINE 38 // thread.start(); !*/

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
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
    Client(InetAddress ad, int p, int cp, int t)
    { address=ad; dataPort=p; controlPort=cp;
      timeOut=t; timedOutIn=t; 
      thread = new Thread(watch);
      thread.start();
      packet =
      new DatagramPacket(
          "null".getBytes(), 4
         ,ad
         ,cp
      );
    }

    DatagramPacket packet;
    InetAddress address;
    int controlPort;
    int dataPort;

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
             "\r\n";
    }
  }


  private Map<Integer, HashMap<String, Client>> groups;
  private Map<Integer, List<byte[]>> aknowledgment;
  private Map<String, String> identification;
  private Map<String, Client> register;
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
    groups = new HashMap<Integer, HashMap<String, Client>>();
    aknowledgment = new HashMap<Integer, List<byte[]>>();
    identification = new HashMap<String, String>();
    register = new HashMap<String, Client>();
    socket = new DatagramSocket(serverPort);
    enabled = true;

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
    if(packet.getLength() <= 0){
      reply(packet,"");
      return;
   }
    String sentence = new String(
       Arrays.copyOf(
          packet.getData(),
          packet.getLength()
       )
    );

    System.out.println(
       ""+
       packet
          .getAddress()
          .toString()
          + ":" + packet.getPort() +
          " (" + String.format("%04d",packet.getLength()) + ") > " + 
       sentence
    );

    switch (requestedMethod(packet)) {
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
      default:
        reply(packet, HTTP.BAD_REQUEST);
    }
  }

  /*  
   *   Generic Method to reply
   *   Used due to responde the client with the message
   *   or the requested action status 
   *   Throws IOException
   */
  private void reply (DatagramPacket packet, HTTP response)
  { reply(packet, response.toString()); }
  private void reply (Client client, HTTP http, String response)
  { reply(client.packet, http.toString() + "\n" + response.toString()); }
  private void reply (DatagramPacket packet, HTTP http, String response)
  { reply(packet, http.toString() + "\n" + response.toString()); }

  private void reply (Client client, HTTP response)
  { reply(client.packet, response.toString()); }
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
    Map<String, String> header = packetHeader(packet);
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

  private Client getRegister (DatagramPacket packet)
  { return getRegister( packetKey(packet)); }
  private Client getRegister (String clientKey){
    if(identification.containsKey(clientKey))
      return register.get(identification.get(clientKey));
    return register.get(clientKey);
  }

   /*  
    *   Remove the client of the server list
    */
  private void disconnect (DatagramPacket packet) {
    String clientKey = packetKey(packet);
    Client client = getRegister(clientKey);
    client.thread.stop();
    register.remove(clientKey);
    identification.remove
    (packetAddress(packet)+":"+client.dataPort);
    reply(packet, HTTP.OK);
  }
  private void disconnect (DatagramPacket packet, Thread thread) {
    register.remove(packetKey(packet));
    reply(packet, HTTP.TIME_OUT);
  }


   /*  
    *   Insert a new client on the group list
    */
  private void join (DatagramPacket packet) {
    if(validateUser(packet)) return;
    Integer groupID;
    try {
      groupID = packetDestinationNumber(packet);
    } catch (NumberFormatException e) {
      reply(packet, e.getMessage());
      return;
    }

    HashMap<String, Client> group = (HashMap) groups.get(groupID);
    String clientKey = packetKey(packet);
    Client client = getRegister(clientKey);

    if(group == null){
      groups.put(
        groupID,
        new HashMap<String, Client>() {{
          put(clientKey, client);
        }}
     );
    } else {
      group.put(clientKey, client);
    }

    reply(client, HTTP.OK);
  }

   /*  
    *   Remove the client of the group list
    */
  private void left (DatagramPacket packet) {
    if(validateUser(packet)) return;
    Integer groupID;
    try {
      groupID = packetDestinationNumber(packet);
    } catch (NumberFormatException e) {
      reply(packet, e.getMessage());
      return;
    }

    HashMap<String, Client> group = (HashMap) groups.get(groupID);
    group.remove(packetKey(packet));

    reply(packet, HTTP.OK);
  }



  private void send (DatagramPacket packet) {
    if(validateUser(packet)) return;
    String dst = packetDestination(packet);
    Client clientDst = getRegister(dst);
    String srcKey = packetKey(packet);

    Integer groupID = 0;
    try {
      groupID = packetDestinationNumber(packet);
    } catch (NumberFormatException e) {}
    HashMap<String, Client> groupDst = (HashMap) groups.get(groupID);
    String message = packetMessage(packet);

    if (clientDst != null) {
      reply(
        new DatagramPacket(
          "null".getBytes(), 4
          ,clientDst.address
          ,clientDst.dataPort
       ),
        message
      );
    } else if (groupDst != null) {
      if(!groupDst.containsKey(srcKey)){
        reply(packet, HTTP.UNAUTHORIZED);
        return;
      }
      groupDst.forEach( (uri, client) -> {
        if(!srcKey.equals(uri)) {
          DatagramPacket target =
            new DatagramPacket(
                "null".getBytes(), 4
               ,client.address
               ,client.dataPort
            );
         reply(target, message);
        };
      });
    } else {
      reply(packet, HTTP.NOT_FOUND);
      return;
    }
    
    reply(packet, HTTP.OK);
  }


  private void broadcast (DatagramPacket packet) {
    if(validateUser(packet)) return;
    String message = packetMessage(packet);
    String clientSrc = packetKey(packet);

    register.forEach((ip, client) -> {
      if(!clientSrc.equals(ip)) {
        DatagramPacket target =
          new DatagramPacket(
              "null".getBytes(), 4
             ,client.address
             ,client.dataPort
          );
       reply(target, message);
      };
    });

    reply(packet, HTTP.OK);
  }

  public void help  (DatagramPacket packet) {
    reply(packet,
        "Server commands:"
      + "\nconnect"     + "\t\t" + "Connect the client with the server"
      + "\ndisconnect"  + "\t"   + "Remove the client of the server connections"
      + "\njoin"        + "\t\t" + "\'join -<<GROUP ID>>\' Will add the client on the group"
      + "\nleft"        + "\t\t" + "\'left -<<GROUP ID>>\' Will remove the client of group."
      + "\nsend"        + "\t\t" + "\'send -<<DESTINY IP:PORT||GROUP ID>> \"<<MESSAGE||FILE:File_Name.ext>>\"\' Will send a message or file to the target"
      + "\nbroadcast"   + "\t"   + "\'broadcast \"<<MESSAGE||FILE:File_Name.ext>>\"\' Will send to every server client the message wroten"
    );
  }


  /* --------------------------------------------- CONTROL METHODS ---------------------------------------------*/


   private void SYN(DatagramPacket packet){
      // sendControl("SYN");
   }

   private void FIN(DatagramPacket packet){
      // sendControl("ACK {ACK_ID:-1, DATA_PORT:" + dataPort + "}");
   }


  /*  
   *   List all registered clients on the server
   */
  private void ACK (DatagramPacket packet) {
    Map<String, String> header = packetHeader(packet);
    String clientKey = packetAddress(packet) + ":" + header.get("DATA_PORT");
    if(validateUser(clientKey)) return;

    try{
      int ack_id = Integer.parseInt(header.get("ACK_ID"));
      List<byte[]> ackBook = aknowledgment.get(ack_id);
      if(ackBook == null){
        do { ack_id = new Random().nextInt(); }
        while(aknowledgment.containsKey(ack_id));
        aknowledgment.put(ack_id, new LinkedList<byte[]>());

      System.out.println(ackBook);
      }
    } catch(Exception e) {
      // reply(packet, );
      return;
    }

    reply(
      new DatagramPacket(
          "null".getBytes(), 4
         ,packet.getAddress()
         ,Integer.parseInt(header.get("DATA_PORT"))
      ),
      HTTP.OK
    );
  }


  /*  
   *   Call the client requesting health check
   */
  private void keepAlive  (DatagramPacket packet) {
    int clientDataPort = 0;
    try{ clientDataPort = packetDestinationNumber(packet); }
    catch(NumberFormatException nfe) {}

    Client client =
      getRegister(packetKey(packet));
    if (client == null) {
      reply(packet, HTTP.TIME_OUT);
      return;
    }

    client.timedOutIn = client.timeOut;
  }


  /*  
   *   List all registered clients on the server
   */
  private void register  (DatagramPacket packet) {
    if(validateUser(packet)) return;
    reply(packet, HTTP.FOUND, register.toString());
  }

  /*  
   *   List all registered clients on the server
   */
  private void groups    (DatagramPacket packet) {
    if(validateUser(packet)) return;
    reply(packet, HTTP.FOUND, groups.toString());
  }

   /*  
    *   ! DevTool !
    *   Close the server
    */
  public void exit       (DatagramPacket packet) {
    disconnect(packet);
    if(register.isEmpty())
      enabled = false;
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
    // 5875
    reply(
      packet,
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
      +"\n(c) 2021 topster.de############################################################"
       // "Imagine um pudim aqui."
      );
  }



  /* --------------------------------------------- ASSISTANT METHODS ---------------------------------------------*/

  private String packetAddress(DatagramPacket packet){
    return
      packet.getAddress()
            .toString()
            .replace("/","");
  }

  /* Used to broke the packet into ClientKey */
  private String packetKey (DatagramPacket packet) {
    return (packetAddress(packet) + ":" + packet.getPort());
  }

  /* Grantee that the user is connected to the server */
  private boolean validateUser (DatagramPacket packet) {
    boolean valid = validateUser(packetKey(packet));
    if(valid) reply(packet, HTTP.UNAUTHORIZED);
    return valid;
  } // ++ Overload
  private boolean validateUser (String clientKey) {
    if (getRegister(clientKey) == null)
      return true;
    return false;
  }

  /* Regex due to decompile the client sent message and
   * discover if it is a known action
   * 
   * return String with a known method listed or error
   * {405} if the request is not known
   */
  private String requestedMethod(DatagramPacket packet) {
    String sentence = new String(packet.getData());
    Matcher matcher
    = Pattern
        .compile(
           "^("
          +"connect|disconnect|join|left|send|broadcast|help|"
          +"keepalive|register|groups|exit|ack|"
          +"brewcoffee|pudim.com.br"
          +")")
        .matcher(sentence.toLowerCase());
        
    if(matcher.find() && matcher.groupCount() == 1)
      return matcher.group(1);
    return HTTP.METHOD_NOT_ALLOWED.toString();
  }

  /*  
   *   Return the destination provided for the Client on number
   *   As used on groupId or client data/control port
   */
  private Integer packetDestinationNumber(DatagramPacket packet){
    String dst =
      packetDestination(packet)
        .replaceAll("\"","");
    Integer number;
    try {
      number = new Integer( 
        Integer.parseInt(dst)
      );
    } catch (NumberFormatException e) {
      throw new NumberFormatException(dst);
    }
    return number;
  }

  /*
   *   Regex due to decompile the client sent message and get the
   *   client destination
   *
   *   return String with the client destination key ("ip:port")
   *   or the group id
   */
  private String packetDestination(DatagramPacket packet) {
    String sentence = new String(packet.getData());
    Matcher matcher
    = Pattern
        .compile(
          "-\\b([01]?[0-9][0-9]?|2[0-4][0-9]|25[0-5])."
          +"\\b([01]?[0-9][0-9]?|2[0-4][0-9]|25[0-5])."
          +"\\b([01]?[0-9][0-9]?|2[0-4][0-9]|25[0-5])."
          +"\\b([01]?[0-9][0-9]?|2[0-4][0-9]|25[0-5]):"
          +"\\b([1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-2][0-9]{2}|653[0-4][0-9]|6535[0-3])")
        .matcher(sentence);
  
    if(matcher.find())
      return matcher.group(0)
                    .substring(
                      1,
                      matcher.group(0)
                             .length()
                    );

    matcher
    = Pattern
        .compile("-[0-9]++\\.")
        .matcher(sentence);
    if(matcher.find()){
      return HTTP.BAD_REQUEST.toString();
    }

    matcher
    = Pattern
        .compile("-[0-9]++")
        .matcher(sentence);
    if(matcher.find()){
      return matcher.group(0)
                    .substring(
                      1,
                      matcher.group(0)
                             .length()
                    );
    }
    return HTTP.BAD_REQUEST.toString();
  }

  /* Regex due to decompile the message to be sent */
  private String packetMessage(DatagramPacket packet) {
    String sentence = new String(packet.getData());
    Matcher matcher
    = Pattern
        .compile("(\".*\")")
        .matcher(sentence);

    if(matcher.find())
      return matcher.group(0);
    return HTTP.NO_CONTENT.toString();
  }

  /*
   *   Regex due to decompile the client message header due to
   *   provide more informations to control the data flow
   *
   *   return an list with all parameters given on the header
   */
  private Map<String, String> packetHeader(DatagramPacket packet) {
    try{
    String sentence = new String(packet.getData());    
    Matcher matcher
      = Pattern
          .compile("(\\{.*\\})")
          .matcher(sentence);

    if(matcher.find())
      return 
        Arrays.stream(
          matcher.group(0)
                 .replaceAll("\\{|\\}","")
                 .split(", "))
          .map(string -> string.split(":"))
          .collect(
            Collectors.toMap(
              array -> array[0],
              array -> array[1]
            )
          );
      }catch(Exception e) {e.printStackTrace();}
    return null;
  }
}