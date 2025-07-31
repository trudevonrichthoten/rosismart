package org.rosi.modules ;

import java.util.* ;
import java.io.*;
import java.net.*;
import java.util.concurrent.* ;

import org.rosi.util.*;
import org.rosi.execution.*;
import org.rosi.drivers.fritzbox.*;
import org.rosi.drivers.generic.*;



public class FritzboxModuleV3 extends GenericModule {

   private ModuleContext   _context   = null ;
   private FbDriver        _fritzbox  = null ;

   private String _loginURI      = "/login_sid.lua" ;
   private String _serviceURI    = "/webservices/homeautoswitch.lua" ;


   public FritzboxModuleV3( String moduleName , ModuleContext context  )
      throws Exception
   {
      super(moduleName,context);

      log("Initiating. (FritzboxModuleV3)");

      _context = context ;

      initializeFritzbox() ;
   } 
   public void initialize() throws Exception {
   }
   public void initializeFritzbox() throws Exception {

      String userString  = _context.get("user"     , true ) ; 
      String passString  = _context.get("password" , true ) ; 
      String urlString   = _context.get("URL"      , true ) ; 

      _fritzbox = new FbDriver( urlString , _loginURI , _serviceURI ) ;

      _fritzbox.setCredentials( userString , passString ) ;

      try{
          _fritzbox.authenticate() ;
          log("Fritzbox authentication succeeded.");
      }catch(FileNotFoundException fnf ){
         errorLog("Server Error. URL not found at server : "+fnf.getMessage() ) ;
      }catch(HttpRetryException httpe ){
         int rc = httpe.responseCode() ;
         if( rc == 403 )errorLog("Authentication Failed" ) ;
         else errorLog("Login Failed : "+httpe.getMessage() ) ;
         throw httpe ;
      }catch(Exception ee ){
         errorLog("Contacting fritzbox failed due to : "+ee ) ;
         throw ee ;
      }

      setDriver( _fritzbox );

   } 
   public boolean handleException( Exception ee ){

      
      if( ee instanceof java.net.UnknownHostException ){

         errorLog(" UnknownHostException in keep alive thread (continuing) : "+ee);
         return false ;

      }else if( ee instanceof java.net.HttpRetryException ){

         errorLog(" HttpRetryException in keep alive thread : "+ee);

         int rc = ((java.net.HttpRetryException)ee).responseCode() ;
         if( rc == 403 ){
             errorLog(" HttpRetryException (403). No longer authenticated. Re-authenticating." ) ;
             try{
                _fritzbox.authenticate() ;
                errorLog(" _fritzbox.authenticate() . Re-authentication successful." ) ;
                return false ;
             }catch(HttpRetryException httpe ){
                int rci = httpe.responseCode() ;
                if( rci == 403 ){
                   errorLog(" HttpRetryException(304): Re-authentication Failed" ) ;
                }else{
                   errorLog(" HttpRetryException: Login Failed (retrying) :"+
                                       " details=[ "+httpe.getMessage()+" ]" ) ;
                }
                return false ;
             }catch(java.net.UnknownHostException uhe ){
                errorLog(" UnknownHostException during re-authentication (continuing) : "+uhe);
                return false ;
             }catch(Exception eee ){
                errorLog(" Exception during re-authentication. Giving up.  details=[ "+eee+" ]" ) ;
                return true ;
             }
         }else{
             errorLog(" Unexpected Exception. Continuing.  details=[ "+ee+" ]" ) ;
             return false ;
         }
     }else{
         errorLog(" Unexpected Exception. Giving up.  details=[ "+ee+" ]" ) ;
         return true ;
     }
   }
}
