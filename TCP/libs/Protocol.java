package libs;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.io.*;

public class Protocol {

  private static int test = 0;

  	public static int genAckID (Map<Integer, Map<Integer, String>> aknowledgment) {
  		int ack_id = 0;
		do { ack_id = new Random().nextInt() & Integer.MAX_VALUE; }
		while(aknowledgment.containsKey(ack_id));
		return ack_id;
  	}

	public static String SYN (DatagramPacket packet, Map<Integer, Map<Integer, String>> aknowledgment, Map<Integer, String> dictionary, String cache) {
	    Map<String, String> header = Decompiler.packetHeader(packet);
	    int ack_id = -1, ack_id_replace;
	    String res = "", params = "";

	    try{
			String req = header.get("REQ");
			switch(req.toUpperCase()){
			case "ALOC" :
				ack_id_replace 	= Integer.parseInt(header.get("ACK_ID"));
				ack_id 			= genAckID(aknowledgment);
				String ack_file = header.get("FILE_NAME");
				// if(test == 0) { test++; ack_id = -2; }
				aknowledgment.put(ack_id, new HashMap<Integer, String>());
				dictionary.put(ack_id, ack_file);
				params = ", ACK_ID_REPLACE:" + ack_id_replace;
				res = "REPL";
				break;
			case "REPL" :
				ack_id_replace =  Integer.parseInt(header.get("ACK_ID_REPLACE"));
				ack_id =  Integer.parseInt(header.get("ACK_ID"));
				if(aknowledgment.containsKey(ack_id) && ack_id_replace != ack_id) {
					ack_id = ack_id_replace;
					res = "ALOC";
					break;
				}
				aknowledgment.put(ack_id, aknowledgment.get(ack_id_replace));
				dictionary.put(ack_id, dictionary.get(ack_id_replace));
				aknowledgment.remove(ack_id_replace);
				dictionary.remove(ack_id_replace);
				res = "OK";
				break;
			case "OK":
				ack_id = Integer.parseInt(header.get("ACK_ID"));
				packet.setData(("SYN {ACK_ID:"+ack_id+"}").getBytes());
				return ACK(packet, aknowledgment, cache);
			}
	    } catch(Exception e) {
	    	e.printStackTrace();
	    	return HTTP.BAD_REQUEST.toString();
	    }

	    res =  
  	     "SYN {"
	    +   "ACK_ID:"         + ack_id  + ", "
	    +   "REQ:"            + res
	    +   params
	    +"}";
	    return res;
	}
	

	public static String ACK (DatagramPacket packet, Map<Integer, Map<Integer, String>> aknowledgment, String cache) {
		Map<String, String> header = Decompiler.packetHeader(packet);
	    Map<Integer, String> ackBook;
	    int ack_id, ack = -1;
		String type = "", params = "";

		try {
			ack_id = Integer.parseInt(header.get("ACK_ID"));
			ackBook = aknowledgment.get(ack_id);
			// System.out.println("ACK:: "+header.get("ACK"));
			// System.out.println("SEQ:: "+header.get("SEQ"));
			if(header.get("ACK") != null) {
				ack = Integer.parseInt(header.get("ACK"));
				if(ackBook.size() >= ack) return FIN(packet, aknowledgment, cache);

				params = ", DATA:" + ackBook.get(ack);
				// ackBook.remove(ack);
				type = "SEQ";
			} else {
				if(header.get("SEQ") != null){
					ack = Integer.parseInt(header.get("SEQ"));
					ackBook.put(ack, header.get("DATA"));
				}
				ack = ack + 1;
				type = "ACK";
			}
		} catch(Exception e) {
			e.printStackTrace();
			return HTTP.BAD_REQUEST.toString();
		}

		return
		"ACK {"
		+   "ACK_ID:" + ack_id + ", "
		+    type+":" + ack
		+	 params
		+"}";
	}
	

	public static String FIN (DatagramPacket packet, Map<Integer, Map<Integer, String>> aknowledgment, String cache) {
		Map<String, String> header = Decompiler.packetHeader(packet);
	    String res = "";
	    int ack_id;
		
		try {
			ack_id = Integer.parseInt(header.get("ACK_ID"));
			String params = "";
			
			if(cache != null && !cache.isEmpty())
				params = ", DST:" + cache.replace(":","^$");
			if (aknowledgment.containsKey(ack_id))
				res = "FIN {ACK_ID:" + ack_id + params + "}";
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
			return HTTP.BAD_REQUEST.toString();
		}

		return res;
	}

}