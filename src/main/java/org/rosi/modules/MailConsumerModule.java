package org.rosi.modules ;

import java.util.* ;
import java.text.SimpleDateFormat ;
import java.io.* ;

import org.rosi.util.*;

import org.apache.commons.mail.*;
import javax.mail.*;


public class MailConsumerModule extends RosiModule {

   private class MailContext {
      private String serverName   = null  ;
      private int    serverPort   = 0;
      private String fromAddress  = null ;
      private String toAddress    = null ;
      private String subject      = null ;
      private String accountName  = null ;
      private String accountPassword = null;  
      
      private void setSubject( String subject ){ this.subject = subject ; }
      private void sendMessage( String message ) throws Exception{

         Email email = new SimpleEmail();
         email.setHostName( serverName );
         email.setSmtpPort(serverPort);
    
         email.setAuthenticator(
            new DefaultAuthenticator( accountName , 
                                      accountPassword ));

         email.setStartTLSEnabled(true);

         email.setFrom( fromAddress  , "Rosi");
         email.setSubject( subject );
         email.addTo( toAddress );
         email.setMsg( message );
         email.send();

      }
   }
   private RosiCommandProcessor _commandProcessor = null ; 
   private ModuleContext        _context = null ;
   private SimpleDateFormat     _sdf     = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
   private File                 _file    = null ;
   private PatternTranslator    _filter  = null ;

   private MailContext _mailContext = null ;

   public MailConsumerModule( String moduleName , ModuleContext context  )
      throws Exception {

      super(moduleName,context);

      _context = context ;

      log( "Started");
 
      _mailContext = new MailContext();

      _mailContext.serverName = context.get("mailserverName" , true );
      _mailContext.serverPort = Integer.parseInt( context.get("mailserverPort" , true ) );
      _mailContext.fromAddress = context.get("from") ;
      _mailContext.toAddress   = context.get("to" );
      _mailContext.accountName = context.get("accountName");
      _mailContext.accountPassword = context.get("accountPassword");
      _mailContext.subject         = context.get("subject");

      String filterFile = context.get( "filterFile" ) ;
      if( filterFile != null ){
         File f = new File( filterFile ) ;
         if( ! f.exists() )
           throw new
           IllegalArgumentException("Filer file not found : "+ filterFile ) ;

         _filter = new PatternTranslator( f ) ;
      }


   } 
   private String buildMessage( String key , String value ){
  
      StringBuffer sb = new StringBuffer() ;
 
      sb.append( "\n" );
      sb.append( "    Dear Trude\n\n" );
      sb.append(" An important parameter of your appartment changed.\n\n");
      sb.append(" Time  : ").append(_sdf.format( new Date() )).append("\n"); 
      sb.append(" Key   : ").append(key).append("\n");
      sb.append(" Value : ").append(value).append("\n\n");
      sb.append("\n    Best regards\n");
      sb.append("       Rosi\n\n");
      return sb.toString();
   } 
   public void run(){

       while(true){

          try{

             RosiCommand c  = take() ;
 
             if( ! ( c instanceof RosiSetterCommand ) ){
                 errorLog("Received an unexpected command type : "+c.getClass().getName() ) ;
                 continue ;
             }

             RosiSetterCommand command = new RosiSetterCommand( (RosiSetterCommand) c ) ; 

             debug("Mail received : "+command);
             if( _filter != null ){

                String [] sub = _filter.translate( command.getKey()+":"+command.getValue() ) ;

                if( ( sub == null ) || ( sub.length == 0 ) )continue ;

                command.setKey( sub[0] ) ;
                if( sub.length > 1 )command.setValue( sub[1] ) ;

                log("Mailing : "+command);

                _mailContext.setSubject("Message from Rosi : ("+command.getKey()+":"+command.getValue()+")");
                _mailContext.sendMessage( buildMessage( command.getKey() , command.getValue() ) ) ; 
 
             }

          }catch(InterruptedException ieee ){
             errorLog("Was interrupted and stopped" ) ;
             break ;
          }catch(Exception eee ){
             errorLog( "Got exeception in main loop: "+eee ) ;
             if( isDebugMode() )eee.printStackTrace();
             break ;
          }
       }

   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
