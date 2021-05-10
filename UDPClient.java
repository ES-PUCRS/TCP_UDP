import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.io.IOException;
import java.lang.Runnable;
import java.util.HashMap;
import java.util.Random;
import java.lang.Thread;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// import java.io.IOException;
import java.net.*;
import java.io.*;

import libs.FileManager;
import libs.Decompiler;
import libs.Protocol;
import libs.HTTP;

class UDPClient {
	public static void main(String args[]) throws Exception {
		new Client();
	}
}

class Client {

	private Map<Integer, Map<Integer, String>> aknowledgment;
	private Map<Integer, String> dictionary;

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
		aknowledgment = new HashMap<Integer, Map<Integer, String>>();
		dictionary = new HashMap<Integer, String>();
		enableKeepAlive = true;
		interrupted = true;
		enabled = true;
		cache = "";

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
		System.out.println("Client UDP rodando em: " +
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

			String echo = sentence.toLowerCase();
			switch (echo) {

				// Control
				case "connect"           : connect(sentence);  break;

				case "keepalive"         : keepalive();  break;
				case "disable keepalive" : disableKeepAlive();  break;
				case "enable keepalive"  : keepAliveSync();     break;
				case "exit"              : exit();              break;
				default:
						  if(echo.startsWith("join"))      join(sentence);
					else if(echo.startsWith("left"))      left(sentence);
					else if(echo.contains("file:>"))      file(sentence);
					else if(echo.length() > 1024) {
						if(echo.startsWith("broadcast")) broadcast(sentence);
						else send(sentence);
					} else
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





	/* --------------------------------------------- CONTROL METHODS ---------------------------------------------*/
	
	/*
	 *  *
	 */
	private void broadcast (String sentence) {
		String msg = Decompiler.packetMessage(sentence);
		int ack_id = Protocol.genAckID(aknowledgment);
		cache = "broadcast";

		try {
			dictionary.put(ack_id, "display");
			aknowledgment.put(ack_id, FileManager.split(msg.getBytes()));
		} catch (IOException e) {System.out.println(e);}
		requestSYN(ack_id);
	}

	/* Send text asking for sync due to prevent package overflow */
	private void send (String sentence) {
		String msg = Decompiler.packetMessage(sentence);
		int ack_id = Protocol.genAckID(aknowledgment);
		cache = Decompiler.packetDestination(sentence);

		try {
			dictionary.put(ack_id, "display");
			aknowledgment.put(ack_id, FileManager.split(msg.getBytes()));
		} catch (IOException e) {System.out.println(e);}
		requestSYN(ack_id);
	}

	/* Send file asking sync due to the file size exceed the package size */
	private void file (String sentence) {
		String fileName = sentence.replaceAll(".*file:>","");
		cache = Decompiler.packetDestination(sentence);
		if(cache.equals(HTTP.BAD_REQUEST.toString())) cache = "broadcast";
		int ack_id = -1;
		try {
			byte[] file = FileManager.read(fileName);
			ack_id = Protocol.genAckID(aknowledgment);
			// System.out.println(Arrays.toString(file));
			dictionary.put(ack_id, fileName);
			aknowledgment.put(ack_id, FileManager.split(file));
		} catch (IOException e) {System.out.println(e);}
		requestSYN(ack_id);
	}

	/* 
	 * Reorganize the request to connect
	 * sending control port and timeout
	 */
	private void connect (String sentence) {
		sendControl (
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


	private void left (String control) {
		sendControl(control);
	}

	private void join (String control) {
		sendControl(control);
	}


	/* Facilitate the exception handler requested of the socket*/
	private void sendControl (String sentence) {
		try{ controlSocket.send(createPacket(sentence)); }
		catch( Exception e ) {e.printStackTrace();}
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

	/* Clone the packet to be able tochange the state and resend it */
	private DatagramPacket clonePacket(DatagramPacket packet){
		return
			new DatagramPacket (
				packet.getData(),
				packet.getLength(),
				packet.getAddress(),
				packet.getPort()
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

	private void keepalive() {
		System.out.println(enableKeepAlive);
	}


  /* --------------------------------------------- SYNC METHODS ---------------------------------------------*/
 
	/*
	 *   This method to start a syncronization with the server
	 *   due to send bigger messages
	 */
	private void requestSYN (int ack_id) {
		sendControl(
			 "SYN {"
			+  "ACK_ID:"+ack_id+", "
			+  "REQ:ALOC, "
			+  "FILE_NAME:"+dictionary.get(ack_id)
			+"}"
		);
	}

	/*
	 *  This variable is used to store the first send command which
	 *  defines the destination of the message or file. 
	 *
	 *  This file must contain ONLY the destination of the send before
	 *  SYN command. 
	 */
	String cache;

	/*
	 *   This method is called by the server response
	 *   use to syncronize with the server patterns due to
	 *   send the data or even request ACK to the server.
	 */
	private void SYN (DatagramPacket packet) {
		sendControl(Protocol.SYN(packet, aknowledgment, dictionary, cache));
	}

	private void ACK (DatagramPacket packet){
		sendControl(Protocol.ACK(packet, aknowledgment, cache));
	}
	
	private void FIN (DatagramPacket packet){
		Map<String, String> header = Decompiler.packetHeader(packet);
		String res = Protocol.FIN(packet, aknowledgment, cache);
		Map<Integer, String> ackBook;
		
		try {
			int ack_id = Integer.parseInt(header.get("ACK_ID"));
			ackBook = aknowledgment.get(ack_id);
			if (ackBook != null) {
				if (cache.isEmpty()) {
					// sendControl(res); res = "";
					ackBook = aknowledgment.get(ack_id);

					byte[] file = Decompiler.toArray(Decompiler.merge(ackBook));
					System.out.println("RECEIVED FILE BYTE SIZE: " + file.length);
					if (dictionary.get(ack_id).equals("display"))
						System.out.println(new String(file, "UTF-8"));
					else
						FileManager.write(("Client_"+dataPort+"-"+dictionary.get(ack_id)), file);
				}
				aknowledgment.remove(ack_id);
				dictionary.remove(ack_id);
				cache = "";
			}
		} catch(Exception e) {e.printStackTrace();}

		if(!res.isEmpty())
			sendControl(res);
	}

	/* --------------------------------------------- THREAD METHODS ---------------------------------------------*/

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
					int limitDisplay = 3;
					controlSocket.receive(receiveControl);
					DatagramPacket packet =
						clonePacket(receiveControl);
					String res = response(packet);

					if (res.equals(HTTP.TIME_OUT.toString())) disableKeepAlive();
					switch(Decompiler.packetMethod(packet).toUpperCase()){
						case "SYN": SYN(packet); break;
						case "ACK": ACK(packet); break;
						case "FIN": FIN(packet); break;
					}
					int seq = 0;
					Map<String, String> header = Decompiler.packetHeader(packet);

					try {
						if(header.get("ACK") == null)
							seq = Integer.parseInt(header.get("SEQ"));
						seq = Integer.parseInt(header.get("ACK"));
					} catch (Exception e) {e.getStackTrace();}
					String disply = res;
					if(seq <= limitDisplay) {
						if (disply.length() > 60)
							disply = disply.substring(0, 52) + "...]}";
						if(seq == limitDisplay)
							disply = "\t\t\t...";
						if(seq <= limitDisplay)
						System.out.println("Control port> " + disply);
					}
				} catch (Exception e) {e.getStackTrace();}
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