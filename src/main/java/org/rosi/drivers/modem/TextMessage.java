package org.rosi.drivers.modem ;
/*
import java.io.* ;
import java.util.*;
import java.util.concurrent.BlockingQueue ;
import java.util.concurrent.ArrayBlockingQueue ;
import java.util.concurrent.TimeUnit ;
*/

public class TextMessage {

   private int _id = 0 ;
   private String _phoneNumber = null ;
   private String _mode        = null ;
   private String _date        = null ;
   private String _body        = null ;

   public TextMessage( int id , String phoneNumber , String mode , String date ){
      _id          = id ;
      _phoneNumber = phoneNumber ;
      _mode        = mode ;
      _date        = date ; 
   }
   public void setBody( String message ){ _body = message ; }
   public int getId(){ return _id ; }
   public String getPhoneNumber(){ return _phoneNumber ; }
   public String getMode(){ return _mode ; }
   public String getDate(){ return _date ; }
   public String getBody(){ return _body ; }

   public String toString(){

      return "ID="+_id+";mode="+_mode+";Phone="+_phoneNumber+";msg=\""+_body+"\"";
   }

}

