package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.net.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.generic.*;
import org.rosi.drivers.homematic.*;

public class HomematicModule extends GenericModule {

   private HmDriver      _driver  = null ; 
   private ModuleContext _context = null ;

   public HomematicModule( String moduleName , ModuleContext context ) throws Exception {
  
      super( moduleName , context ) ;

      log("Initiating. (Homematic Module)");

      _context = context ;

      String urlString  = _context.get("URL" , true ) ;

      String dryrun = _context.get("dryrun" ) ;
      if( ( dryrun != null ) && ( dryrun.equals("yes") )){
         _driver = null ;
         return ;
      }

      setDriver( _driver = new HmDriver( urlString ) );

   }
   public void initialize() throws Exception {

   }
   public boolean handleException( Exception ee ){
      return false ;
   }

}



