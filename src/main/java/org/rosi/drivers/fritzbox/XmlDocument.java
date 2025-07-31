package org.rosi.drivers.fritzbox ;

import java.util.* ;
import javax.xml.parsers.*;
import org.w3c.dom.*;
   


public class XmlDocument {

   private Node _topNode = null ;

   public XmlDocument( Node node ){
      _topNode = node ;
//      System.out.println(printXmlDocument());
   }

   public Node getTopNode(){ return _topNode ; }

   public static String getNextLevelValue( Node node ) throws IllegalArgumentException {
   
       String value = node.getNodeValue() ;
       
       if( value != null )return value ;
       
       NodeList list = node.getChildNodes() ;

       if( ( list == null ) || ( list.getLength() == 0 ) )
            return "undefined";
//          throw new 
//	  IllegalArgumentException("Next Level value not found (no child) "+node.getNodeName());  
	  
       Node it = list.item(0) ;
       
       value = it.getNodeValue() ;
       if( value == null )
          throw new 
	  IllegalArgumentException("Next Level value is not set (null)");  
        
       return value ;
       
   }
   public FritzboxLogin getLogin() throws IllegalArgumentException {
   
       FritzboxLogin login = new FritzboxLogin() ;
       
       _searchNode( login , _topNode ) ;
       
       return login ;
        
   }
   public List<FritzboxDeviceInfo>  getFritzboxDeviceInfos()throws IllegalArgumentException {
    
       List<FritzboxDeviceInfo> list = new ArrayList<FritzboxDeviceInfo>() ;
       
       _searchForDeviceList( list , _topNode ) ;
       
       return list ;
   
   }
   private void _searchForDeviceList( List<FritzboxDeviceInfo> list , Node node ) throws IllegalArgumentException {
   
       String nodeName = node.getNodeName() ;
       
       if( nodeName.equals( "devicelist" ) ){
      
	  NodeList deviceList = node.getChildNodes() ;
           
 	  for( int i = 0 ; i < deviceList.getLength() ; i++ ){

	      Node deviceNode = deviceList.item(i) ;
 
              NodeList deviceProperties = deviceNode.getChildNodes() ;
	       
	      FritzboxDeviceInfo info = new FritzboxDeviceInfo() ;

              info.setDeviceID( deviceNode.getAttributes().getNamedItem("identifier").getNodeValue() );
	      
              for( int j = 0 ; j < deviceProperties.getLength() ; j++ ){
	      
	           Node deviceProperty = deviceProperties.item(j);
		   
		   if( deviceProperty.getNodeName().equals("name") ){
		   
		       info.setName( getNextLevelValue( deviceProperty ) ) ;
		       
		   }else if( deviceProperty.getNodeName().equals("powermeter") ){

                       NodeList tempList = deviceProperty.getChildNodes() ;

                       for( int l = 0 ; l < tempList.getLength() ; l++ ){

                          Node listItem = tempList.item(l) ; 

                          if( listItem.getNodeName().equals("power") ){
                             info.setPower( getNextLevelValue( listItem ) ); 
                          }
                       }

		   }else if( deviceProperty.getNodeName().equals("temperature") ){

                       NodeList tempList = deviceProperty.getChildNodes() ;

                       for( int l = 0 ; l < tempList.getLength() ; l++ ){

                          Node listItem = tempList.item(l) ; 

                          if( listItem.getNodeName().equals("celsius") ){
                             info.setTemperature( getNextLevelValue( listItem ) ); 
                          }
                       }

		   }else if( deviceProperty.getNodeName().equals("switch") ){

                       NodeList tempList = deviceProperty.getChildNodes() ;

                       for( int l = 0 ; l < tempList.getLength() ; l++ ){

                          Node listItem = tempList.item(l) ; 

                          if( listItem.getNodeName().equals("state") ){
                             info.setState( getNextLevelValue( listItem ) ); 
                          }
                       }

		   }else if( deviceProperty.getNodeName().equals("present") ){
		   
		       info.setPresent( getNextLevelValue( deviceProperty )  );
		       
		   }
	      }
	      
	      list.add( info );
 
 	  }    
     
       }else{
       
	  NodeList l = node.getChildNodes() ;
	  
	  for( int i = 0 ; i < l.getLength() ; i++ ){

             _searchForDeviceList( list ,  l.item(i) ) ;
	     
	  }    
       }
       
       
   }
   private static void _searchNode( FritzboxLogin login , Node node ){
   
       String nodeName = node.getNodeName() ;
       
       if( nodeName.equals( "SID" ) ){
       
           login.setSID( getNextLevelValue( node ) ) ;
	   
       }else if( nodeName.equals( "Challenge" ) ){
       
           login.setChallenge( getNextLevelValue( node ) ) ;
	   
       }else if( nodeName.equals( "BlockTime" ) ){
       
	   String value = getNextLevelValue( node ) ;

           try{
              login.setBlockTime( Integer.parseInt( value ) ) ;
	   }catch( NumberFormatException nfe ){
              throw new 
	      IllegalArgumentException("Not a number : "+value );  
	   }
	   
       }else if( nodeName.equals( "Rights" ) ){
       
	  NodeList list = node.getChildNodes() ;
	  
	  String rightsName  = null ;
	  String rightsValue = null ;
	  
	  for( int i = 0 ; i < list.getLength() ; i++ ){

	     Node it = list.item(i) ;
 
             if( ( i % 2 ) == 0 ){
                 rightsName  = getNextLevelValue( it ) ;		 
             }else{
                  rightsValue = getNextLevelValue( it ) ;
		  if( rightsValue.equals( "1" ) )rightsValue = "RO" ;
		  else if( rightsValue.equals("2") )rightsValue = "RW" ;
		  else rightsValue = "("+rightsValue+")" ;
		  
		  login.addRights( rightsName , rightsValue ) ;
		  		 
	     }
	  }    

       }else{
	  NodeList list = node.getChildNodes() ;
	  
	  for( int i = 0 ; i < list.getLength() ; i++ ){

	     Node it = list.item(i) ;

             _searchNode( login ,  it ) ;
	     
	  }    
       }
   }
   public static String printNode( Node node ){
   
       StringBuffer sb = new StringBuffer() ;
       
       _printNode( sb , node , "" ) ;
       
       return sb.toString() ;
   
   }
   public static void _printNode( StringBuffer sb , Node node , String gap ){
   

       String name = node.getNodeName() ;
       sb.append( gap ).append( "Name : " ).append( name ) ;
       String value = node.getNodeValue() ;
       if( value != null ){
           if( name.equals("functionbitmask") ){
              sb.append(" ; Value = " ).append( Integer.toBinaryString(Integer.parseInt(value)) ) ;
           }else{
              sb.append(" ; Value = " ).append( value ) ;
           }
       }
       sb.append("\n");
       sb.append( gap ).append( "Attributes\n" );
       NamedNodeMap map = node.getAttributes() ;
       if( map != null ){
          for( int i = 0 ; i < map.getLength() ; i++ ){
            Node n = map.item(i);
            _printNode( sb , n , gap + "   " ) ;
          }
       }
       
       sb.append( gap ).append( "Children\n" );
       NodeList list = node.getChildNodes() ;
       for( int i = 0 ; i < list.getLength() ; i++ ){
          
	  Node it = list.item(i) ;
	  
          _printNode( sb ,  it , gap + "   " ) ;
       }    
       
   }
   public String printXmlDocument(){
       return printNode( _topNode ) ;
   }

}

