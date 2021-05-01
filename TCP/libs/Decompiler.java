/*
 *   Regex due to decompile the client sent message and
 *   discover if it is a known action
 *
 *   return String with a known method listed or error
 *   {405} if the request is not known
 */
package libs;

import static java.util.stream.Collectors.joining;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.net.*;
import java.io.*;

public class Decompiler {

  /*
   *   Regex due to decompile the client sent message and
   *   discover if it is a known action
   *
   *   return String with a known method listed or error
   *   {405} if the request is not known
   */
  public static String packetMethod(DatagramPacket packet) {
    String sentence = new String(packet.getData());
    Matcher matcher
    = Pattern
        .compile(
           "("
          +"connect|disconnect|join|left|send|broadcast|help|"
          +"keepalive|register|groups|exit|syn|ack|fin|"
          +"brewcoffee|pudim.com.br|"
          +"showheart|hideheart"
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
  public static Integer packetDestinationNumber(DatagramPacket packet){
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
  public static String packetDestination(DatagramPacket packet) { return packetDestination(new String(packet.getData())); }
  public static String packetDestination(String sentence) {
    if(sentence == null) return null;
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
  public static String packetMessage(DatagramPacket packet){ return packetMessage(new String(packet.getData())); }
  public static String packetMessage(String sentence) {
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
  public static Map<String, String> packetHeader(DatagramPacket packet) {
    Map<String, String> map = new HashMap<String, String>();
    String sentenceMap, sentence;
    try{
      sentenceMap = sentence = new String(packet.getData());

      Matcher arrays
        = Pattern
          .compile("(\\[.*?\\])")
          .matcher(sentence);
      for (int i = 0; arrays.find(); i++) {
        String array = sentence.substring(arrays.start(), arrays.end());
        sentenceMap = sentenceMap.replace(array,"^"+i);
        map.put("^"+i,array);
      }

      Matcher matcher
        = Pattern
          .compile("(\\{.*[^\\]]\\})")
          .matcher(sentenceMap);

      if(matcher.find()){
        Map<String, String> res =
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

        if(map.size() > 0) {
          for (Map.Entry<String, String> entry : res.entrySet()) {
            if(entry.getValue().contains("^"))
              res.replace(entry.getKey(), map.get(entry.getValue()));
          }
        }
        return res;
      }
    } catch (Exception e) {e.printStackTrace();}
    return null;
  }

  /*
   *   As the data is transported by strings on the header
   *   there is needed this method to convert the data string
   *   into the original byte array values.
   *
   *   There may be an exception if the sender does not convert
   *   to byte before send because the String need to be casted
   *   to int before became a byte array.
   *   Therefore, is known that this exception can happen on the
   *   conversion.
   */
  public static byte[] toArray(String str) {
    try{
      int[] list =
        Arrays.stream(
            str.replaceAll("\\[|\\]","")
            .split(", "))
          .map(Integer::valueOf)
          .mapToInt(i -> i)
          .toArray();

      byte[] array = new byte[list.length];
      for (int i = 0; i < list.length; i++)
          array[i] = (byte) list[i];
      return array;
    }catch(Exception e){System.out.println(e);}

    return null;
  }


  /*
   *   Since the connection need to send different packages
   *   due to manage the size of the file, this method is used
   *   to put all together as a String array = "[0, 0, 0]"
   */
  public static String merge(Map<Integer, String> ackBook) {
    return 
     "["+
        ackBook.entrySet()
           .stream()
           .map(e -> e.getValue().replaceAll("\\[|\\]",""))
           .collect(joining(", "))
    +"]";
  }

}