// Recebe um pacote de algum cliente
// Separa o dado, o endere?o IP e a porta deste cliente
// Imprime o dado na tela

import java.io.*;
import java.net.*;
import java.util.Arrays;

class UDPServer {

   public static final int serverPort = 9876;
   public static boolean enabled = true;

   public static void main(String args[])  throws Exception
      {
         System.out.println("Server UDP rodando em: " + InetAddress.getByName("localhost") + ":" + serverPort);
         // cria socket do servidor com a porta 9876
         DatagramSocket serverSocket = new DatagramSocket(serverPort);
            byte[] receiveData = new byte[1024];
            while(enabled)
               {
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
                  
                  if(sentence.contains("data:"))
                     sentence = sentence.replace("data:","Catch! {");

                  System.out.println("Mensagem recebida de "+
                                 IPAddress.toString() +":"+ port + " > " + 
                                 sentence);
                  
                  if(sentence.contains("exit"))
                     enabled = false;
               }
      }
}

