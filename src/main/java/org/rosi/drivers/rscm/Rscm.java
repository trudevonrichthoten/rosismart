package org.rosi.drivers.rscm;

import java.io.*;
import java.net.*;
import java.util.*;

class Rscm {



   public static void main(String args[]) throws Exception {
     if( args.length < 3 ){
       System.err.println("Usage : hostname portnumber message");
       System.exit(0);
     }
     String hostname   = args[0] ;
     int    portNumber = Integer.parseInt( args[1] ) ;
     String message    = args[2] ;

     DatagramSocket clientSocket = new DatagramSocket();

     InetAddress IPAddress = InetAddress.getByName( hostname );

     byte [] sendData   = message.getBytes();

     DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, portNumber);
     clientSocket.send(sendPacket);

     byte[] receiveData = new byte[1024];
     DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
     clientSocket.receive(receivePacket);

     String modifiedSentence = new String(receivePacket.getData());

     System.out.println(modifiedSentence);
     clientSocket.close();
   }
   
}
