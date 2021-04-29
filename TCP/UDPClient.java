/*! UNCOMMENT  LINE 134 // keepAliveSync(); 
   MUDAR LINHA 144!
!*/

import java.io.RandomAccessFile;
import java.io.IOException;
import java.lang.Runnable;
import java.util.HashMap;
import java.util.Random;
import java.lang.Thread;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.net.*;
import java.io.*;

class UDPClient {
   public static void main(String args[]) throws Exception {
      new Client();
   }
}

class Client {

   private Map<Integer, List<byte[]>> aknowledgment;

   private static int generatePort ()
   { return (new Random().nextInt(65353-10000) + 10000); }
   private InetAddress serverAddress;
   private InetAddress clientAddress;
   private int controlPort;
   private int dataPort;

   private DatagramSocket controlSocket;
   private DatagramSocket dataSocket;

   private DatagramPacket sendPacket;

   // private byte[] receiveData;
   private DatagramPacket receivePacket;

   private boolean enableKeepAlive;
   private boolean interrupted;
   private boolean enabled;
   private String returned;

   private final int serverPort    = 9876;
   private final int keepAliveTime = 10;
   private final int timedOutSec   = 1;
   private final int attempts      = 4;
   private final int timedOut      = 
                     timedOutSec * 1000;

   private Thread keepAliveThread;

   private BufferedReader inFromUser;

   private static Client instance;
   public static Client getInstance () {
      try {
         if(instance == null)
            instance = new Client();
         } catch (Exception e) {}
      return instance;
   }

   private void setVariables () {
      try{
         clientAddress =
         serverAddress =
            InetAddress.getByName("localhost");
      } catch (Exception e) {}

      inFromUser = new BufferedReader(new InputStreamReader(System.in));
      receivePacket = new DatagramPacket(new byte[1024], 1024);
      aknowledgment = new HashMap<Integer, List<byte[]>>();
      enableKeepAlive = true;
      interrupted = true;
      enabled = true;

      do {
         controlPort = generatePort();
         dataPort    = generatePort();
         try{
            controlSocket = new DatagramSocket(controlPort);
            dataSocket = new DatagramSocket(dataPort);
         } catch (Exception e) {}
      } while (controlSocket == null || dataSocket == null);
   }


   public Client () throws Exception { setVariables();
      System.out.println("Client TCP rodando em: " +
      clientAddress + ":" + dataPort);
      new Thread(watchControlResponse).start();
      new Thread(watchDataResponse).start();
      run();
   }
 
   public void run() {
      String sentence="";
      while(enabled) {
         try{ sentence = inFromUser.readLine(); }
         catch(Exception e){}

         switch (sentence.toLowerCase()) {

            // Control
            case "connect"           : connect(sentence);  break;
            case "syn"               :  SYN();  break;
            case "ack"               :  ACK();  break;
            case "fin"               :  FIN();  break;

            case "disable keepalive" :  disableKeepAlive();  break;
            case "enable keepalive"  :  keepAliveSync();     break;
            case "exit"              :  exit();              break;
            default:
                    if(sentence.startsWith("join")) join(sentence);
               else if(sentence.startsWith("left")) left(sentence);
               else if(sentence.contains("file:>")) file(sentence);
               else
                  send(
                     createPacket(
                        sentence.getBytes()
                     )
                  );
         }
         // if(sentence.contains("throw")) sendData = fileBytes;
      }

      disableKeepAlive();
      dataSocket.close();
      controlSocket.close();
   }

   private void SYN(){
      sendControl("SYN");
   }
   private void ACK(){
      sendControl("ACK {ACK_ID:-1, DATA_PORT:" + dataPort + "}");
   }
   private void FIN(){
      sendControl("ACK {ACK_ID:-1, DATA_PORT:" + dataPort + "}");
   }

   /* 
    * Reorganize the request to connect
    * sending control port and timeout
    */
   private void connect (String sentence){
      sendControl(
         sentence + " "
         +"{"
            +"CONTROL_PORT:"  + controlPort         + ", "
            +"DATA_PORT:"     + dataPort            + ", "
            +"TIME_OUT:"      + (keepAliveTime * 2)
         +"}"
      );
      keepAliveSync();
   }

   private void send (DatagramPacket sendPacket) {
      try {
         this.sendPacket = sendPacket;
         dataSocket.send(sendPacket);
      } catch (Exception e)
      { System.out.println(e.getLocalizedMessage()); }
   }


   /* --------------------------------------------- CONTROL METHODS ---------------------------------------------*/

   private void left (String control) {
      sendControl(control);
   }

   private void join (String control) {
      sendControl(control);
   }

   private void file (String fileName) {
      // File file = new File("files/ball.txt");
      // RandomAccessFile fileReader = new RandomAccessFile(file, "r");
      // byte[] fileBytes = new byte[(int) fileReader.length()];
      // fileReader.readFully(fileBytes);
      // // return fileBytes;
   }

   /* Facilitate the exception handler requested of the socket*/
   private void sendControl (String sentence) {
      try{ controlSocket.send(createPacket(sentence)); }
      catch( Exception e ) {}
   }

   /* Cut the unused response size */
   private String response (DatagramPacket packet) {
      return new String(
            Arrays.copyOf(
               packet.getData(),
               packet.getLength()
         )
      );
   }

   /* --------------------------------------------- ASSISTANT METHODS ---------------------------------------------*/

   /* Create a packet to be sent to the server */
   private DatagramPacket createPacket (String data)
   { return createPacket( data.getBytes()); }
   private DatagramPacket createPacket (byte[] data) {
      return new
         DatagramPacket(
            data,
            data.length,
            serverAddress,
            serverPort
         );
   }


   /* Close the client and send a request
    * to disconnect from the server
    */
   private void exit() {
      send(
         createPacket(
            "exit".getBytes()
         )
      );
      disableKeepAlive();
      enabled = false;
   }

   /* Simply take the keep alive thread off
    * after the keep alive time is over.
    */
   private void disableKeepAlive() {
      enableKeepAlive = false;
   }

   /* Start the thread to keep sync alive
    * with the server.
    */
   private void keepAliveSync() {
      keepAliveThread = new Thread(keepAlive);
      enableKeepAlive = true;
      keepAliveThread.start();
   }

   /* --------------------------------------------- ASYNC METHODS ---------------------------------------------*/

   /* This method is used to send a message to the server
    * requesting to keep alive on the server list preventing
    * to face a Timed Out response.
    */
   private Runnable keepAlive = new Runnable() {
      public void run() {
         while(enableKeepAlive) {
            sendControl("keepalive -"+dataPort);
            try { keepAliveThread.sleep(keepAliveTime * 1000); }
            catch ( Exception e ) {}
         }
      }
   };


   /* This method is used to send a message to the server
    * requesting to keep alive on the server list preventing
    * to face a Timed Out response.
    */
   private Runnable watchControlResponse = new Runnable() {
      public void run() {
         byte[] data = new byte[1024]; 
         DatagramPacket receiveControl =
            new DatagramPacket(data, data.length);
         while(enabled) {
            Arrays.fill(data, (byte) 0);
            try {
               controlSocket.receive(receiveControl);
               String res = response(receiveControl);
               switch (res.toUpperCase()) {
                  case "{408: REQUEST TIME OUT}" : disableKeepAlive(); break;
                  default:
                     System.out.println("Control port> " + res);
               }
            } catch (Exception e) {}
         }
      }
   };

   /* This method is used to send a message to the server
    * requesting to keep alive on the server list preventing
    * to face a Timed Out response.
    */
   private Runnable watchDataResponse = new Runnable() {
      public void run() {
         byte[] data = new byte[1024];
         DatagramPacket receiveData =
            new DatagramPacket(data, data.length);
         while(enabled) {
            try {
               dataSocket.receive(receiveData);
               System.out.println("Data port> " + response(receiveData));
            } catch (Exception e) {}
         }
      }
   };

   /* Watch is a method to validade the receing responde
    * during the attempts sent while not trigger time out
    */
   private Runnable watch = new Runnable() {
      public void run() {
         Thread connectionTimeThread = new Thread(connectionTime);
         connectionTimeThread.start();
         interrupted = false;
         try {
            dataSocket.receive(receivePacket);
            interrupted = true;
         } catch (Exception e) {}
      }
   };
   /* This is a control method which is responsable to send
    * a new package to the server since the client does not
    * receive a response on the defined time
    */
   private Runnable connectionTime = new Runnable() {
      public void run() {

         for( int i = 0; i < attempts; i++ ){
            try{ Thread.currentThread().sleep(timedOut); }
            catch (InterruptedException ex) {}

            if(interrupted){ return; }
            else {
               try { dataSocket.send(sendPacket); } catch (IOException ioe) {}
               System.out.println("> Nao houve resposta. Enviando novo pacote");
            }
         }

         dataSocket.close();
         System.out.println("> Connection timed out.");
         returned = "";
      }
   };
}






   // private Runnable watch = new Runnable() {
   //   public void run() {
   //     while(timedOutIn >= 0) {
   //       timedOutIn--;
   //       try { thread.sleep(1000); }
   //       catch ( Exception e ) {}
   //     }
   //     System.out.println("Left counter: " + timedOutIn);
   //     Server
   //       .getInstance()
   //       .disconnect(
   //         new DatagramPacket(
   //             "null".getBytes(), 4
   //            ,address
   //            ,dataPort
   //         ),
   //         thread
   //       );
   //   }
   // };