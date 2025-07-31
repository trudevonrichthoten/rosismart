package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.net.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.telnet.*;

public class TelnetActorModule extends RosiModule {

   private RosiCommandProcessor  _commandProcessor = null ; 
   private ModuleContext         _context = null ;
   private RosiRuntimeExecution  _process = null ;
   private PatternTranslator     _filter  = null ;

   private TelnetDriver    _telnet = null ;

   public TelnetActorModule( String moduleName , ModuleContext context  )
      throws Exception
   {
      super(moduleName,context);
      log("Initiating.");
      _context = context ;

      _initializeTelnet() ;

      String filterFile = context.get( "filterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Filer file not found : "+ filterFile ) ; 
 
         _filter = new PatternTranslator( f ) ;
      }

   } 
   private void _initializeTelnet() throws Exception {

      String userString  = _context.get("user"     , true ) ; 
      String passString  = _context.get("password" , true ) ; 
      String urlString   = _context.get("URL"      , true ) ; 

      String [] url = urlString.split(":") ;

      if( url.length != 2 )
         throw new 
         URISyntaxException( urlString , "Malformated URL, should be <host>:<port>");

      String hostName = url[0] ;
      int portNumber = Integer.parseInt( url[1] ) ;

      String dryrun = _context.get("dryrun" ) ;
      if( ( dryrun != null ) && ( dryrun.equals("yes") )){
         _telnet = null ;
         return ;
      }

      _telnet = new TelnetDriver( hostName , portNumber ) ;
      _telnet.setCredentials( userString , passString ) ;

   } 
   public RosiSetterCommand createTelnetCommand( RosiSetterCommand command )
       throws Exception
   {
 
      command = new RosiSetterCommand( command ) ;

      if( _filter != null ){

          String [] sub = _filter.translate( command.getKey() ) ;

          if( ( sub != null ) && ( sub.length >=1 ) )command.setKey( sub[0] ) ; 
          else return null ;
      }
      return command ;

   }
   private void executeTelnetCommand( RosiSetterCommand command ) throws Exception {
  
      if( _telnet == null )return ;  

      String deviceName = command.getKey() ;
      String value      = command.getValue() ;

      _telnet.setDevice( deviceName , value ) ;

   }
   public void run(){

      log("Starting.");

      while(true){
          try{

             RosiCommand command  =  take() ;

             if( command instanceof RosiSetterCommand ){

                log("Setter '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

                try{

                   RosiSetterCommand fbc = createTelnetCommand( (RosiSetterCommand) command ) ;
                   if( fbc != null ){

                       log("Sending to telnet : '"+fbc+"'");

                       executeTelnetCommand( fbc ) ;
                   }

                }catch(Exception ee ){
                   errorLog( "Exception executing telnet command '"+command+"' : "+ee ); 
                }
             }else{
                log("unkown (ignored) '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;
             }

          }catch(InterruptedException ieee ){
             errorLog("Interrupted in main loop: "+ieee.getMessage() ) ;
             break ;
          }catch(Exception eee ){
             errorLog("Runtime Error in main loop : "+eee.getMessage() ) ;
          }
       }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
