package org.rosi.util ;

public class StackEntry {

      public static final int SUBSTITUTION = 1 ;

      private String  _string    = null ;
      private int     _number    = 0 ;
      private boolean _isSub     = false ;
      private boolean _isInt     = false ;

      public StackEntry( String string ){
        _string   = string ;
        _isSub    = false ;
        _isInt    = false ;
      }
      public StackEntry( int type , String string ){
        _string   = string ;
        _isInt    = false ;
        _isSub    = true ;
      }
      public StackEntry( int type , int number ){
        _number = number ;
        _isInt  = true ;
        _isSub  = true ;
      }
      public boolean isNumber(){ return _isInt ; }
      public boolean isSubstitution(){ return _isSub ; }
      public String  getString(){ return _string ; }
      public int     getNumber(){  return _number ; }
      public String toString(){
         if( ! isSubstitution() ){
            return _string; 
         }else if( isNumber() ){
            return "NUMBER{"+_number+"}" ;
         }else{
            return "VAR{"+_string+"}" ;
         }
      }
}

