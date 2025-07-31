package org.rosi.util  ;
import java.text.* ;
import java.util.*;


public class Dt {


   public static void main( String [] args )throws Exception {
/*
       SimpleDateFormat sdf = new SimpleDateFormat(args[0]) ;
       
       Date d = sdf.parse(args[1]);
       
       System.out.println("Result : "+d ) ;
       */
       Calendar c = Calendar.getInstance() ;
       
       c.setTime( new Date() ) ;
       System.out.println(" time : "+( new Date() ) ) ;
       /*
       c.set( Calendar.YEAR , 2015  ) ;
       c.set( Calendar.MONTH , 3 ) ;
       c.set( Calendar.DAY_OF_MONTH , 4 ) ;
       c.set( Calendar.HOUR_OF_DAY , 0  ) ;
       c.set( Calendar.MINUTE , 0 ) ;
       c.set( Calendar.SECOND , 0 ) ;
       c.set( Calendar.MILLISECOND , 0 ) ;
       */
       long millis = c.getTimeInMillis() ; 
       
       System.out.println(" Tiem : "+millis+" Cal : "+c );

if( args.length >  0 ){

       SimpleTimeZone timezone = new SimpleTimeZone( 0 ,  args[0] ) ;
       
       timezone.useDaylightTime() ;
       
       c = Calendar.getInstance( timezone , new Locale("German") ) ;
       //c.setTimeZone( timezone ) ;
       c.setTime( new Date() ) ;
       
       System.out.println(" Tiem : "+millis+" Cal : "+c );
}
       String [] ID = TimeZone.getAvailableIDs() ;
 /*   
       for( int i = 0 ; i < ID.length ; i++ ){
          System.out.println(ID[i]);
       
       }
 */      
 
   }


}
