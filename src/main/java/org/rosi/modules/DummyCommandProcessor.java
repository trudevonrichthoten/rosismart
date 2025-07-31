package org.rosi.modules ;

import org.rosi.util.*;

public class DummyCommandProcessor implements RosiCommandProcessor {
   private ModuleContext  _context = null ;
   public DummyCommandProcessor( ModuleContext context ){
      _context = context ;
   } 
   public RosiCommand process( String command ){
       if( command.equals("dummy") )return null ;
       return new RosiCommand( "PROCESSED: "+command);
   }
}
