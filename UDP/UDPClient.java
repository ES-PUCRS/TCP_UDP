
// L? uma linha do teclado
// Envia o pacote (linha digitada) ao servidor

import java.io.*; // classes para input e output streams e
import java.net.*;// DatagramaSocket,InetAddress,DatagramaPacket

class UDPClient {

   public static boolean enabled = true;

   public static void main(String args[]) throws Exception
   {
      // cria o stream do teclado
      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));


      File file = new File("ball.txt");
      //Instantiate the input stread
      InputStream insputStream = new FileInputStream(file);
      byte[] fileBytes = new byte[(int) file.length()];
      
      int offset = 0, n = 0;
      //Read the data into bytes array
      while (offset < fileBytes.length
         && (n = insputStream.read(fileBytes, offset, fileBytes.length - offset)) >= 0) {
        offset += n;
      }
      insputStream.close();

      // declara socket cliente
      while(enabled){
         DatagramSocket clientSocket = new DatagramSocket();
         // obtem endere?o IP do servidor com o DNS
         InetAddress IPAddress = InetAddress.getByName("192.168.0.163");

         byte[] sendData = new byte[1024];
         byte[] receiveData = new byte[1024];

         // l? uma linha do teclado
         String sentence = inFromUser.readLine();
         if(sentence.contains("throw"))
            sendData = fileBytes;
         else
            sendData = sentence.getBytes();

         // cria pacote com o dado, o endere?o do server e porta do servidor
         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

         //envia o pacote
         clientSocket.send(sendPacket);

         // fecha o cliente
         clientSocket.close();


         if(sentence.contains("exit"))
            enabled = false;
      }
   }
}

