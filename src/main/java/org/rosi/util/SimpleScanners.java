package org.rosi.util ;

import java.io.* ;
import java.util.* ;
import java.util.regex.* ;


public class SimpleScanners {

   private static  final int NONE     = 0 ;
   private static  final int REGULAR  = 1 ;
   private static  final int DOLLAR   = 2 ;
   private static  final int LEFT_B   = 4 ;
   private static  final int RIGHT_B  = 8 ;
   private static  final int NUMBER   = 0x10 ;
   private static  final int VARIABLE = 0x20 ;
   private static  final int LITERAL  = 0x40 ;

   public static List<StackEntry> compile( String code ) throws Exception {

      StringReader reader = new StringReader( code ) ;
      int n ;
      int state = REGULAR ;
      int type  = NONE ;
      StringBuffer sb = new StringBuffer() ;
      List<StackEntry> stack = new ArrayList<StackEntry>() ;
      while( ( n = reader.read() ) > 0 ){
         char c = (char) n ;
         //System.out.println("Got : "+c + ";mode : "+state+"; type : "+type ) ;
         switch( state ){

           case REGULAR :
             if( c == '$' ){
                if( sb.length() > 0 ){
                  stack.add( new StackEntry( sb.toString() ) );
                  sb.setLength(0) ;
                }
                state = DOLLAR ;
             }else{
                sb.append(c) ;
             }
           break ;
           case DOLLAR :
             if( c == '{' ){
                state = LEFT_B ;
             }else{
                throw new
                IllegalArgumentException("Compile error: expected '{', got "+c) ;
             }
           break ;
           case RIGHT_B :
             state = REGULAR ; 
             break ;
           case LEFT_B :
             if( ( c >= '0' ) && ( c <= '9' ) )
             {
                sb.append( c ) ;
                state =  VARIABLE ;
                type  =  NUMBER ; 

             }else if( ( ( c >= 'a' ) && ( c <= 'z' ) ) ||
                       ( ( c >= 'A' ) && ( c <= 'Z' ) ) ||
                         ( c == '.' ) || ( c == '-' ) || ( c == '_' )
                    ) 
             {
                sb.append( c ) ;
                state = VARIABLE ;
                type  = LITERAL ;
             }else{
                throw new
                IllegalArgumentException("Compile error: expected 'number', got "+c) ;
             }
           break ;
           case VARIABLE :
             if( ( c >= '0' ) && ( c <= '9' ) ){
                sb.append( c ) ;
             }else if( ( ( c >= 'a' ) && ( c <= 'z' ) ) ||
                       ( ( c >= 'A' ) && ( c <= 'Z' ) ) ||
                         ( c == '.' ) || ( c == '-' ) || ( c == '_' )
                    ) {
                type = LITERAL ;
                sb.append( c ) ;
             }else if( c == '}' ){
                if( type == NUMBER ){
                   stack.add( new StackEntry( StackEntry.SUBSTITUTION , Integer.parseInt( sb.toString() ) ) ) ;
                }else{
                   stack.add( new StackEntry( StackEntry.SUBSTITUTION ,  sb.toString() ) ) ;
                } 
                sb.setLength(0);
                state = REGULAR ;
             }else{
                throw new 
                IllegalArgumentException("Compile error: expected 'number' or '}', got "+c) ;
             }
             break ;
           default:
             throw new 
             IllegalArgumentException("Compile error (internal error, can't happen)") ;
         }
      }
      if( state != REGULAR )
        throw new 
        IllegalArgumentException("Compile error: permaturly found EOI") ;

      if( sb.length() > 0 )stack.add( new StackEntry( sb.toString() ) ) ;

      return stack ;
   }
   public static void main( String [] args ) throws Exception {

      if( args.length < 1 ){
        System.out.println("Usage : ... <input>");
        System.exit(2);
      }
      List<StackEntry> list = SimpleScanners.compile( args[0] ) ;
      for( StackEntry e : list ){
         System.out.println(e.toString());
      }
   }


}
