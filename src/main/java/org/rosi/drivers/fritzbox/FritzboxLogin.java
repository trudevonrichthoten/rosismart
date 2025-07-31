package org.rosi.drivers.fritzbox ;

import java.util.Map ;
import java.util.HashMap ;

public class FritzboxLogin {
   
    private String   SID       = null ;
    private String   Challenge = null ;
    private int      BlockTime = 0 ;
    
    private Map<String,String> Rights = new HashMap<String,String>() ;
    
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
        this.Rights.put( name , value ) ;
    }
    public String getChallenge(){ return this.Challenge ; }
    public String getSID(){ return this.SID ; }
    public String toString(){
    
       StringBuffer sb = new StringBuffer() ;
       
       sb.append("SID=").append( SID == null ? "(null)" : SID ).append("\n");
       sb.append("Challenge=").append( Challenge == null ? "(null)" : Challenge ).append("\n");
       sb.append("BlockTime=").append( BlockTime ).append("\n");
       sb.append("Rights\n");
       for( Map.Entry<String,String> entry : Rights.entrySet() ){
          sb.append("   ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
       }
       return sb.toString();
    }
    public boolean isValid(){
        return ( SID != null ) && ! SID.equals("0000000000000000") ;
    }
}
 
