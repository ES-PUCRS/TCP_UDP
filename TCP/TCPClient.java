
// L? uma linha do teclado
// Envia o pacote (linha digitada) ao servidor

import java.io.*; // classes para input e output streams e
import java.net.*;// DatagramaSocket,InetAddress,DatagramaPacket
import java.lang.Thread;
import java.lang.Runnable;
import java.io.IOException;
import java.io.RandomAccessFile;

class TCPClient {


   private static final int timedOutSec = 1;
   private static final int attempts = 4;

   private static boolean enabled = true;
   private static boolean interrupted = true;
   private static String returned;

   private static DatagramSocket clientSocket;
   private static DatagramPacket receivePacket;
   private static DatagramPacket sendPacket;

   private static final int timedOut = timedOutSec * 1000;
   public static void main(String args[]) throws Exception {
      System.out.println("Origin");

      // cria o stream do teclado
      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

      File file = new File("ball.txt");
      //Instantiate the input stread
      RandomAccessFile fileReader = new RandomAccessFile(file, "r");
      byte[] fileBytes = new byte[(int) fileReader.length()];
      fileReader.readFully(fileBytes);
      

      while(enabled) {
         // Limpando a variável String
         returned = "";
         // declara socket cliente
         clientSocket = new DatagramSocket(9877);
         // obtem endere?o IP do servidor com o DNS
         InetAddress IPAddress = InetAddress.getByName("localhost");
         // InetAddress IPAddress = InetAddress.getByName("192.168.0.163");

         byte[] sendData = new byte[1024];
         byte[] receiveData = new byte[1024];

         // l? uma linha do teclado
         String sentence = inFromUser.readLine();
         if(sentence.contains("throw"))
            sendData = fileBytes;
         else
            sendData = sentence.getBytes();

         // cria pacote com o dado, o endere?o do server e porta do servidor
         sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
         receivePacket = new DatagramPacket(receiveData, receiveData.length);

         // try {

            //envia o pacote
            clientSocket.send(sendPacket);
            if(sentence.contains("exit")){
               enabled = false;
               clientSocket.close();
               break;
            }
   
            Thread watchThread = new Thread(watch);
            watchThread.start();
            watchThread.join();

            // Atualizar formato da mensagem recebida
            returned = new String(receivePacket.getData());
            
            if(returned.contains("200"))
               System.out.println("Resposta: 200 OK");

         // fecha o cliente
         clientSocket.close();
      }
   }


   private static Runnable watch = new Runnable() {
      public void run() {
         Thread connectionTimeThread = new Thread(connectionTime);
         connectionTimeThread.start();
         interrupted = false;
         try {
            clientSocket.receive(receivePacket);
            interrupted = true;
         } catch (Exception e) {}
      }
   };

   private static Runnable connectionTime = new Runnable() {
      public void run() {

         for( int i = 0; i < attempts; i++ ){
            try{
               Thread.currentThread().sleep(timedOut);
            } catch (InterruptedException ex) {}

            if(interrupted){
               return;
            } else {            
               try { clientSocket.send(sendPacket); } catch (IOException ioe) {}
               System.out.println("> Nao houve resposta. Enviando novo pacote"+returned);
            }
         }

         clientSocket.close();
         System.out.println("> Connection timed out.");
         returned = "";
      }
   };
}

