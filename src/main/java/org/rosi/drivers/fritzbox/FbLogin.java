package org.rosi.drivers.fritzbox ;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.util.Map ;
import java.util.HashMap ;

public class FbLogin {
   
    private String   SID       = null ;
    private String   Challenge = null ;
    private int      BlockTime = 0 ;
    
    private NodeEntry _nodeEntry = null ; 
    private Map<String,String> _rights = new HashMap<String,String>();

    public FbLogin( Node node ) throws IllegalArgumentException {
       _nodeEntry = new NodeEntry(node);
       if( ! _nodeEntry.getNodeType().equals("SessionInfo") )
         throw new
         IllegalArgumentException("XML Document is not a 'SessionInfo' but '"+_nodeEntry.getNodeType()+"'!");
       processNodeEntry( _nodeEntry );
    }   
    public void setSID( String SID ){
       this.SID = SID ;
    }
    public void setChallenge( String challenge ){
       this.Challenge  = challenge ;
    }
    public void setBlockTime( int blockTime ){
       this.BlockTime = blockTime ;
    }
    public void addRights( String name , String value ){
       _rights.put(name,value);
    }

    public String getChallenge(){ return this.Challenge ; }

    public String getSID(){ return this.SID ; }

    private void processNodeEntry( NodeEntry nodeEntry ){
 
        for( NodeEntry entry : nodeEntry.getValues() ){
           scanSessionInfoDetails( this ,  entry ) ;
        }
    }
    private void scanSessionInfoDetails( FbLogin login , NodeEntry nodeEntry ){
  
       String nodeName = nodeEntry.getName() ;
       if( nodeName.equals( "SID" ) ){
       
           login.setSID( nodeEntry.getValue() ) ;
	   
       }else if( nodeName.equals( "Challenge" ) ){
            
           login.setChallenge( nodeEntry.getValue()  ) ;
	   
       }else if( nodeName.equals( "BlockTime" ) ){
       
	   String value = nodeEntry.getValue() ;

           try{
              login.setBlockTime( Integer.parseInt( value ) ) ;
	   }catch( NumberFormatException nfe ){
              throw new 
	      IllegalArgumentException("Value for BlockTime is not a number : "+value );  
	   }
	   
       }else if( nodeName.equals( "Rights" ) ){
	  
	  String rightsName  = null ;
	  String rightsValue = null ;
	  int i = 0 ; 
	  for( NodeEntry it : nodeEntry.getValues() ){

             if( ( i++ % 2 ) == 0 ){
                 rightsName  = it.getValue() ;		 
             }else{
                  rightsValue = it.getValue() ;
		  if( rightsValue.equals( "1" ) )rightsValue = "RO" ;
		  else if( rightsValue.equals("2") )rightsValue = "RW" ;
		  else rightsValue = "("+rightsValue+")" ;
		  
		  login.addRights( rightsName , rightsValue ) ;
		  		 
	     }
	  }    
       }

    }
    public String toString(){
    
       StringBuffer sb = new StringBuffer() ;
       
       sb.append("SID=").append( SID == null ? "(null)" : SID ).append("\n");
       sb.append("Challenge=").append( Challenge == null ? "(null)" : Challenge ).append("\n");
       sb.append("BlockTime=").append( BlockTime ).append("\n");
       sb.append("Rights\n");
       for( Map.Entry<String,String> entry : _rights.entrySet() ){
          sb.append("   ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
       }
       return sb.toString();
    }
    public boolean isValid(){
        return ( SID != null ) && ! SID.equals("0000000000000000") ;
    }
}
 
