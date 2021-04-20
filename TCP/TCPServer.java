// Recebe um pacote de algum cliente
// Separa o dado, o endere?o IP e a porta deste cliente
// Imprime o dado na tela

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
      Client(InetAddress ad, int p, int t)
      { address = ad; port = p; timer = t; }
      Client(int p, int t)
      { port = p; timer = t; }
      
      InetAddress address;
      int port;

      int timer;

      @Override
      public String toString(){
         return "Timer: " + timer;
      }
   }


   private final int serverPort = 9876;
   private final DatagramSocket socket;
   private static int registerID = 0;

   private HashMap<Integer, HashMap<String, Client>> groups;
   private Map<String, Client> register;
   private boolean enabled;


  Server () throws Exception {
    System.out.println("Server TCP rodando em: " + InetAddress.getByName("localhost") + ":" + serverPort);
    register = new HashMap<String, Client>();
    groups = new HashMap<Integer, HashMap<String, Client>>();
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
   *   Possible server calls:
   *    - connect
   *    - disconnect
   *    - join <<GROUP ID>>
   *    - left <<GROUP ID>>
   *    - send -<<DESTINY IP:PORT || GROUP ID>> "<<MESSAGE>>"
   *    - broadcast "MESSAGE"
   *    - keepAlive
   *    - list
   *    - listgroups
   *    - exit
   */
  public void received(DatagramPacket packet) throws IOException {
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
       "Mensagem recebida de "+
       packet.getAddress().toString() +":"+ packet.getPort() +" (" + packet.getLength() + ") > " + 
       sentence
    );


    switch (requestedMethod(packet)) {
      // Client User
      case "connect"    : connect    (packet); break;
      case "disconnect" : disconnect (packet); break;
      case "join"       : join       (packet); break;
      case "left"       : left       (packet); break;
      case "send"       : send       (packet); break;
      case "broadcast"  : broadcast  (packet); break;
      // Client connection
      case "ack"        : ACK        (packet); break;
      case "keepAlive"  : keepAlive  (packet); break;
      case "list"       : list       (packet); break;
      case "listgroups" : listGroups (packet); break;
      case "exit"       : exit       (packet); break;
      // Easter eggs
      case"brewcoffee"  : brewCoffee (packet); break;
      case"pudim.com.br": pudim      (packet); break;
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
  public void reply      (DatagramPacket packet, String response) {
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
  public void connect    (DatagramPacket packet) {
    String clientKey = packetKey(packet);
    register.put(
       clientKey,
       new Client(
          packet.getAddress(),
          packet.getPort(),
          0
       )
    );
    reply(packet, "{201 CREATED} \n[\n\tClientKey: \'"+clientKey+"\'\n]");
  }

   /*  
    *   Remove the client of the server list
    */
  public void disconnect (DatagramPacket packet) {
    register.remove(packetKey(packet));
    reply(packet, "200 OK");
  }

   

   /*  
    *   Insert a new client on the group list
    */
  public void join    (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
    // Integer groupID = 0;
    // HashMap<Integer, Client> group = groups.get(groupID);

    // Client client = new Client(
    //   packet.getAddress(),
    //   packet.getPort(),
    //   0
    // );


    // if(group == null){
    //   groups.put(
    //     groupID,
    //     new HashMap<Integer, Client>() {{
    //       put(2, client);
    //     }}
    //  );
    // } else {
    //   groups.put(
    //     groupID,
    //     group.add(client)
    //   );
    // }

    // reply(packet, "200 OK");
  }

   /*  
    *   Remove the client of the group list
    */
  public void left       (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
    groups.remove(packet.getAddress());
    reply(packet, "{200 OK}");
  }



  public void send       (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
    String dst = sendDestination(packet);
    HashMap<String, Client> groupDst = groups.get(dst);  
    Client clientDst = register.get(dst);

    String message = sendMessage(packet);
    if(message.length() > 1024){
      reply(packet, "{413 PAYLOAD TOO LARGE}");
      return;
    }

    if (clientDst != null) {
      reply(
        new DatagramPacket(
          "null".getBytes(), 4
          ,clientDst.address
          ,clientDst.port
       ),
        message
      );
    } else if (groupDst != null) {
      groupDst.forEach( (uri, client) -> {
          reply(
            new DatagramPacket(
              "null".getBytes(), 4
              ,clientDst.address
              ,clientDst.port
            ),
            message
          );
      });
    } else {
      reply(packet,"{404 NOT FOUND}");
      return;
    }
    
    reply(packet, "{200 OK}");
  }


  public void broadcast  (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
    // String message = "" ;
    // register.forEach((ip, client) -> {
    //    reply(
    //       new DatagramPacket(
    //           null
    //          ,0
    //          ,ip
    //          ,client.port
    //       ),
    //       message
    //    );
    // });
  }




  /* --------------------------------------------- CONTROL METHODS ---------------------------------------------*/

  private String ACK (DatagramPacket packet) {
    return packet.getAddress().toString() +":"+ packet.getPort();
  }


  public void keepAlive  (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
  }


  /*  
  *   List all registered clients on the server
  */
  public void list       (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
    reply(packet, "{302 FOUND}\t" + register.toString());
  }

  /*  
  *   List all registered clients on the server
  */
  public void listGroups (DatagramPacket packet) {
    if(validateUser(packet)){ reply(packet, "{401 UNAUTHORIZED}"); return; }
    reply(packet, "{302 FOUND}\t" + groups.toString());
  }

   /*  
    *   ! DevTool !
    *   Close the server
    */
  public void exit       (DatagramPacket packet) {
    enabled = false;
  }




  /* --------------------------------------------- EASTER EGG ---------------------------------------------*/

  public void brewCoffee  (DatagramPacket packet) {
    reply(
      packet,
      "{418 I'm a teapot}     "
      +"\n\t The server refuses the attempt to brew coffee with a teapot~~ \n"
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
  private String packetKey (DatagramPacket packet) {
    String key = packet.getAddress().toString();
    key = key.substring(1, key.length());
    key += ":" + packet.getPort();
    return key;
  }

  private boolean validateUser(DatagramPacket packet) {
    return register.get(packetKey(packet)) == null;
  }

  private String requestedMethod(DatagramPacket packet) {
    String sentence = new String(packet.getData());
    Matcher matcher
    = Pattern
        .compile(
           "^("
          +"connect|disconnect|join|left|send|broadcast|"
          +"keepAlive|list|listgroups|exit|"
          +"brewcoffee|pudim.com.br"
          +")")
        .matcher(sentence.toLowerCase());
        
    if(matcher.find() && matcher.groupCount() == 1)
      return matcher.group(1);
    return "{405 METHOD NOT ALLOWED}";
  }

  private String sendDestination(DatagramPacket packet) {
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
    return "{400 BAD REQUEST}";
  }

  private String sendMessage(DatagramPacket packet) {
    String sentence = new String(packet.getData());
    Matcher matcher
    = Pattern
        .compile("(\".*\")")
        .matcher(sentence.toLowerCase());

    if(matcher.find())
      return matcher.group(0);
    return "{400 BAD REQUEST}";
  }

} 

