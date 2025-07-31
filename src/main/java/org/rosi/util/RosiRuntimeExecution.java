package org.rosi.util ;


public class RosiRuntimeExecution {

    private String  _processPath = null ;
    private int     _port        = 7072 ;
    private Runtime _runtime     = Runtime.getRuntime() ;
    
    public RosiRuntimeExecution( String processPath , int port ){
        _processPath = processPath ;
	_port        = port ;
    }
    public int execute( String command ) throws Exception {
    
         String [] x = new String[3] ;
	 
	 x[0] = _processPath ;
	 x[1] = "" + _port ;	 
	 x[2] = command ;
	 
	 Process p = _runtime.exec( x ) ;
	 try{ 
	     p.waitFor() ;
	 }catch(Exception ee ){
	    System.err.println("Got runtime execption : "+ee ) ;
	    return -1 ;
	 }
	 return p.exitValue() ;
    }

    public static void main( String [] args ) throws Exception {
    
        RosiRuntimeExecution e = new RosiRuntimeExecution( args[0] , Integer.parseInt(args[1]) ) ;
	
	int rc = e.execute(args[2]) ;
	
	System.out.println("Process terminated with : "+rc ) ;
	
    }
}
