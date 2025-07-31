package org.rosi.modules ;

import java.util.* ;
import org.rosi.execution.*;
import org.rosi.util.*;

public class ShellCommandProcessor implements RosiCommandProcessor {
   private ModuleContext  _context = null ;
   public ShellCommandProcessor( ModuleContext context ){
      _context = context ;
   } 
   public RosiCommand process( String command ){

       if( command == null )return null ;

       command = command.trim() ;

       if( command.equals("") )return null ;

       String [] args = command.split(" ") ;

       try{
          if( args[0].equals("timer" ) ){
            if( args.length > 1 ){
               return new RosiTimerCommand( args[1] ) ;
            }else{
               return new RosiTimerCommand() ;
            }

          }else if( args[0].equals("set") ){
            if( args.length < 3 )return null ;
            return new RosiSetterCommand( args[1] , args[2] ) ;
          }else{
             return null ;
          }
       }catch(Exception ee ){
          return null ;
       }
   }
}
