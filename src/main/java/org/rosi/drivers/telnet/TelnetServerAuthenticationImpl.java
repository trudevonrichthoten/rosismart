package org.rosi.drivers.telnet ;

import  java.net.InetAddress ;

public class TelnetServerAuthenticationImpl {

     public boolean isHostOk( InetAddress host ){
        return true ;
     } 
     public boolean isUserOk( InetAddress host , String user ){
        return true ;
     } 
     public boolean isPasswordOk( InetAddress host , String user , String passwd ){
        return true ;
     } 

}
