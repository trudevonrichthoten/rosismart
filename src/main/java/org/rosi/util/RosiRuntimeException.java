package org.rosi.util ;

public class RosiRuntimeException extends Exception {

   private String _context = null ;
   public RosiRuntimeException( String message ){
        super(message);
   }
   public RosiRuntimeException( String message , String context ){
        super(message);
        _context = context ;
   }
   public String getMessage(){
      if( _context != null ) return "["+_context+"] "+super.getMessage() ;
      return super.getMessage();
   }
   public String getContext(){
     return _context ;
   }

}
