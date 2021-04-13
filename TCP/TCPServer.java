// Recebe um pacote de algum cliente
// Separa o dado, o endere?o IP e a porta deste cliente
// Imprime o dado na tela

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.net.*;
import java.io.*;

class TCPServer {

   public static boolean enabled = true;
   private static final int serverPort = 9876;

   public static void main(String args[])  throws Exception
      {
         System.out.println("Server TCP rodando em: " + InetAddress.getByName("localhost") + ":" + serverPort);
            byte[] receiveData = new byte[1024];

            // declarando resposta de recebimento
            byte[] confirmation = new byte[1024];
            confirmation = "200 OK".getBytes();

            // declara o socket para responder recebimento

            while(enabled) {
               DatagramSocket serverSocket = new DatagramSocket(serverPort);
               // cria socket do servidor com a porta 9876
               // limpa o array
               Arrays.fill( receiveData, (byte) 0 );
               // declara o pacote a ser recebido
               DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

               // recebe o pacote do cliente
               serverSocket.receive(receivePacket);
               // pega os dados, o endere?o IP e a porta do cliente
               // para poder mandar a msg de volta
               String sentence = new String(Arrays.copyOf(receivePacket.getData(), receivePacket.getLength()));
               InetAddress IPAddress = receivePacket.getAddress();
               int port = receivePacket.getPort();

               System.out.println("Mensagem recebida de "+
                                 IPAddress.toString() +":"+ port + " > " + 
                                 sentence);

               // responde avisando que o pacote foi recebido
               serverSocket.send(new DatagramPacket(confirmation, confirmation.length, IPAddress, port));

               if(sentence.contains("exit"))
                  enabled = false;

               // fecha o socket
               serverSocket.close();
            }
      }
}



   // public static void main(String args[])  throws Exception
   //    {
   //       System.out.println("SERVERUP");
   //       // cria socket do servidor com a porta 9876
   //       DatagramSocket serverSocket = new DatagramSocket(9876);
   //          byte[] receiveData = new byte[1024];
   //          while(enabled)
   //             {
   //                Arrays.fill( receiveData, (byte) 0 );
                  
   //                // declara o pacote a ser recebido

   //                // recebe o pacote do cliente
   //                serverSocket.receive(receivePacket);
                  
   //                if(sentence.contains("data:"))
   //                   sentence = sentence.replace("data:","Catch! {");

   //                System.out.println("Mensagem recebida: " + sentence);
                  
   //                if(sentence.contains("exit"))
   //                   enabled = false;
   //             }
   //    }