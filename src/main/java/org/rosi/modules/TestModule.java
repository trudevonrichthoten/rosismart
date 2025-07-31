package org.rosi.modules ;

import java.util.* ;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;

public class TestModule extends RosiModule {

   private RosiCommandProcessor       _commandProcessor = null ; 
   private ModuleContext              _context = null ;
   private String                     _name  = null ;

   public TestModule( String moduleName , ModuleContext context ){
      super(moduleName,context);
      log("RUNNER initiated");
      _name    = moduleName ;
      _context = context ;
   } 

   public void run(){
      log("RUNNER started");
   }
   public void setCommandProcessor( RosiCommandProcessor commandProcessor ){
     _commandProcessor = commandProcessor ;
   }
}
