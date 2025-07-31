package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.fhem.*;

public class FhemActorModule extends RosiModule {

   private RosiCommandProcessor  _commandProcessor = null ; 
   private ModuleContext         _context = null ;
   private Commander             _process = null ;
   private PatternTranslator     _filter  = null ;

   private String _fhemPath = "/home/homematic/fhem-5.6/fhem.pl" ;
   private int    _fhemPort = 7072 ;
   private String _fhemHost = "localhost" ;

   private String _actionLogFile  = "/var/log/rosi/rosiCommands.log" ;
 
   public interface Commander {
      public void runCommand( String command ) ;
   }
   private class TelnetProcessor extends FhemTelnetDriver implements Commander {
      public TelnetProcessor( String hostname , int port ) throws Exception {
           super(hostname,port);
      }
      public void runCommand( String command ){
         try{
           sendCommand(command);
         }catch(Exception ee ){
             errorLog("Message got lost ("+command+") due to "+ee);
         }
      }
   } 
   private class CommandProcessor extends RosiRuntimeExecution implements Commander {
      public CommandProcessor( String path , int port ){
           super(path,port);
      }
 
      public void runCommand( String command ) {
         try{
           execute(command);
         }catch(Exception ee ){
             errorLog("Message got lost ("+command+") due to "+ee);
         }
      }
   } 

   public FhemActorModule( String moduleName , ModuleContext context  )
      throws Exception
   {
      super(moduleName,context);
      log("Initiating.");

      _context = context ;


      String mode = _context.get("fhemProtocol") ;
      if( ( mode == null ) || ( mode.trim().equals("") ) )mode = "local" ;

      if( mode.equals("local") ){

          _fhemPath = _context.get("fhemPath" , true  ) ; 
          _fhemPort = Integer.parseInt( _context.get("fhemPort" , true )  ) ; 
          log("Using fhem (local) with "+_fhemPath+":"+_fhemPort);
          _process  = new CommandProcessor( _fhemPath , _fhemPort ) ;

      }else if( mode.equals("telnet") ){
          _fhemHost = _context.get("fhemServerHostname" , true  ) ; 
          _fhemPort = Integer.parseInt( _context.get("fhemPort" , true )  ) ; 
          log("Using fhem (telnet) with "+_fhemHost+":"+_fhemPort);
          _process  = new TelnetProcessor( _fhemHost , _fhemPort ) ;

      }else{
           throw new
           IllegalArgumentException("Unsupported Fhem protocol : "+ mode ) ; 
      }

      String filterFile = context.get( "filterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Filter file not found : "+ filterFile ) ; 
 
         _filter = new PatternTranslator( f ) ;
      }

   } 
   public String composeFhemCommand( RosiSetterCommand command )
       throws Exception
   {

      String result = "set "+command.getKey()+" "+command.getValue() ;
      if( _filter != null ){

          String [] sub = _filter.translate( command.getKey() ) ;

          if( ( sub != null ) && ( sub.length >=1 ) )
             result = "set "+sub[0]+" "+command.getValue() ;
          else
             result = null;

      }
      return result ;

   }
   public void run(){

      log("Starting.");

      while(true){
          try{

             RosiCommand command  =  take() ;

             if( command instanceof RosiSetterCommand ){

                log("Setter '"+command.getSource()+"' -> '"+getName()+"' cmd="+command ) ;

                try{

                   String fhemCommand = composeFhemCommand( (RosiSetterCommand) command ) ;

                   if( fhemCommand != null ){
                      log("Sending to fhem : '"+fhemCommand+"'");

                      if( _fhemPort > 0 )_process.runCommand( fhemCommand ) ;
                   }

                }catch(Exception ee ){
                   errorLog( "Exception executing command '"+command+"' : "+ee ); 
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
