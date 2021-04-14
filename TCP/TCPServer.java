// Recebe um pacote de algum cliente
// Separa o dado, o endere?o IP e a porta deste cliente
// Imprime o dado na tela

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.*;
import java.io.*;

class TCPServer {

   public static boolean enabled = true;
   private static final int serverPort = 9876;

   public static void main(String args[])  throws Exception
      {
         System.out.println("Server TCP rodando em: " + InetAddress.getByName("localhost") + ":" + serverPort);
            byte[] receiveData = new byte[1024];

            

            // declara o socket para responder recebimento
            ArrayList <String> list = new ArrayList();
            DatagramSocket serverSocket = new DatagramSocket(serverPort);

            while(enabled) {
               // cria socket do servidor com a porta 9876
               // limpa o array
               Arrays.fill( receiveData, (byte) 0 );
               // declara o pacote a ser recebido
               DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

               // recebe o pacote do cliente
               serverSocket.receive(receivePacket);
               // pega os dados, o endere?o IP e a porta do cliente
               // para poder mandar a msg de volta
               

               list.add()

               
               

               // fecha o socket
               serverSocket.close();
            }
      }

   public static void recived(DatagramPacket packet) {
      InetAddress IPAddress = packet.getAddress();
      int port = packet.getPort();
      String sentence = new String(Arrays.copyOf(packet.getData(), packet.getLength()));
      System.out.println("Mensagem recebida de "+
                                 IPAddress.toString() +":"+ port + " > " + 
                                 sentence);
   }
   public void reply(DatagramPacket packet) throws IOException {
      InetAddress IPAddress = packet.getAddress();
      int port = packet.getPort();
      // declarando resposta de recebimento
      byte[] confirmation = new byte[1024];
      
      DatagramSocket serverSocket = new DatagramSocket(serverPort);
      // responde avisando que o pacote foi recebido
      serverSocket.send(new DatagramPacket( (new byte[] {'2','0','0',' ',' ','O','K'}), confirmation.length, IPAddress, port));
      serverSocket.close();
   }

}



  