package org.rosi.util ;
import java.util.regex.* ;
import java.lang.reflect.*;
import java.util.*;

public class pt extends  ArrayList {

   public static void main( String [] args )throws Exception {

/*
      if( args.length < 1 ){
        System.out.println("Usage : ... <patter>");
        System.exit(2);
      }

      Class<? extends ArrayList> s = Class.forName( args[0] ).asSubclass( java.util.ArrayList.class ) ;
      Constructor<? extends ArrayList> x = s.getConstructor( java.util.ArrayList.class );
      ArrayList hallo = x.newInstance(  ) ; 
      System.out.println(" is : "+hallo);
*/
      if( args.length < 2 ){
        System.out.println("Usage : ... <patter> <test>");
        System.exit(2);
      }
      Pattern p = Pattern.compile( args[0] );
      Matcher m = p.matcher( args[1] );
      boolean b = m.matches();
      System.out.println(" Result : "+b ) ;
      if( b ){
         int gc = m.groupCount() ;

         for( int i = 0 ; i < (gc+1) ; i++ ){

            System.out.println("["+i+"] >"+m.group(i)+"<");

         }
      }
   }


} 
