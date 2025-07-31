package org.rosi.drivers.rscm;

public class RscmAsyncClient {
    
   public static void outDebug( String msg ){
      System.out.println("DEBUG ("+System.currentTimeMillis()+","+Thread.currentThread().getName()+") : "+msg ) ;   
   }

  public static void main(String args[]) throws Exception {
       
     if( args.length < 3 ){
       System.err.println("Usage : [<hostname>:<portnumber> ...] device option");
       System.exit(0);
     }

     RscmClient client = new RscmClient();
     
     int i = 0 ;
     for(i = 0 ; i < args.length ; i++ ){
         
         String in = args[i] ;
         String [] ar = in.split(":") ;
         if( ar.length > 1 )
             client.addServer( ar[0] , Integer.parseInt( ar[1] ) ) ;
         else
             break ;
     }
     if( ( args.length - i ) > 0 ){
         if( ( args.length - i ) < 2 ){
           System.err.println("Usage : [<hostname>:<portnumber> ...] device option");
           System.exit(0);
         }
         String device    = args[i++] ;
         String option    = args[i++] ;
         
         while(true){
             option = option.equals("off") ? "on" : "off" ;
             outDebug("Sending x "+device+" "+option);
             try{
                client.sendSetDeviceRequest( 
                      device , 
                      option , 
                      10000L ,
                   new RscmClient.RscmMessageArrivable() {
                       public void messageArrived(  RscmClient.Envelope e ){
                           StringBuffer sb = new StringBuffer();
                           sb.append("Acync : ") ;
                           if( e.isTimeout() ){
                               sb.append("Request timed out");
                           }else{
                               String [] x = e.getReplyVector() ;
                               
                               sb.append(" Vector size : ");
                               if( x == null )sb.append("(NULL)");
                               else sb.append( x.length ) ;
                               
                               if( e instanceof RscmClient.SetDeviceEnvelope ){
                                  RscmClient.SetDeviceEnvelope se = (RscmClient.SetDeviceEnvelope)e;
                                  sb.append(";rc=").append(se.getReturnValue()).
                                     append(";rc=").append(se.getReturnMessage());
                               }else{
                                  sb.append("; Not a  SetDeviceEnvelope");   
                               }
                           }
                           outDebug(sb.toString());
                       }
                   }
                ) ;
             }catch(Exception eee ){
                outDebug("Exception in sendSetDeviceRequest "+eee) ; 
                eee.printStackTrace();
             }
             Thread.sleep(10000L);
        }
     }
   }
}
