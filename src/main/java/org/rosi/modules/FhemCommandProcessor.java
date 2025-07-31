package org.rosi.modules ;

import java.util.* ;
import java.io.* ;
import org.rosi.execution.*;
import org.rosi.util.*;

public class FhemCommandProcessor implements RosiCommandProcessor {

   private ModuleContext      _context = null ;
   private PatternTranslator  _pattern = null ;

   public FhemCommandProcessor( ModuleContext context ){
      _context = context ;
      String pattern = context.get("filter") ;

      if( pattern == null )
         throw new
         IllegalArgumentException("'filter' keyword not found in config");

      File patternFile = new File( pattern ) ;
      if( ! patternFile.exists() )
         throw new
         IllegalArgumentException("Filter file not found : "+pattern);

      try{
          _pattern = new PatternTranslator( patternFile ) ;
      }catch(Exception ee ){
         throw new
         IllegalArgumentException("Problem in pattern file : "+ee);
      }
   } 
   public RosiCommand process( String command ){
       try{
          String [] result = _pattern.translate( command ) ;
          if( result == null )return null ; 
          if( result.length != 2 )return null;
          return new RosiSetterCommand( result[0] , result[1] ) ;
       }catch(Exception ee ){
          System.err.println("Problem in 'process' of FhemCommandProcessor : "+ee);
          return null ;
       }
   }
}
