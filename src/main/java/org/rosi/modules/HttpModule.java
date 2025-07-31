package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.net.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.generic.*;
import org.rosi.drivers.http.*;

public class HttpModule extends GenericModule {

   private HttpSimpleDriverV2 _driver  = null ; 
   private ModuleContext      _context = null ;

   public HttpModule( String moduleName , ModuleContext context ) throws Exception {
  
      super( moduleName , context ) ;

      _context = context ;

      String urlString  = _context.get("URL" , true ) ;

      String dryrun = _context.get("dryrun" ) ;
      if( ( dryrun != null ) && ( dryrun.equals("yes") )){
         _driver = null ;
         return ;
      }

      setDriver( _driver = new HttpSimpleDriverV2( urlString ) );

   }
   public void initialize() throws Exception {

   }
   public boolean handleException( Exception ee ){
      return false ;
   }

}



