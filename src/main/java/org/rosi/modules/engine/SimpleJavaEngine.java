package org.rosi.modules.engine;

import org.rosi.modules.ModuleContext ;
import org.rosi.modules.engine.InspectionCompanion.FieldDetails;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap ;
import java.util.HashSet;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Collections;
import java.lang.reflect.*;


public class SimpleJavaEngine implements BasicEnginable, Runnable {

/* ------------------------------------------------------------------ 
 ____  _                   _                  
/ ___|| |_ _ __ _   _  ___| |_ _   _ _ __ ___ 
\___ \| __| '__| | | |/ __| __| | | | '__/ _ \
 ___) | |_| |  | |_| | (__| |_| |_| | | |  __/
|____/ \__|_|   \__,_|\___|\__|\__,_|_|  \___|

*/
    class MotionDetector {

        String cover = "closed" ;
        int brightness = 0 ;
        String battery = "low" ;

        @DeviceAnnotation("sensor(trigger)")
        String motionCount = "nothing" ;

    }
    class DoorDetector {
        String activity = "alive" ;
    
        @DeviceAnnotation("sensor(trigger)")
        int trigger_cnt = 0 ; 

        @DeviceAnnotation("mono(trigger_cnt,20,onChange)")
        boolean trigger_cnt_mono = false ;
    }
    class HeaterDevice {

        @DeviceAnnotation("sensor()")
        float temperature = (float) 0.0 ;

        @DeviceAnnotation("actor()")
        float desired_temperature = (float) 0.0 ;
    }
    class Room {

        DoorDetector door = new DoorDetector();
        MotionDetector motion = new MotionDetector() ;
        HeaterDevice heater = new HeaterDevice() ;
    }
    class rooms {
        Room livingroom = new Room() ;
        Room hallway    = new Room();
    }
    rooms devices = new rooms();
/* ------------------------------------------------------------------          
|  _ \ _ __ ___   __ _ _ __ __ _ _ __ ___  
| |_) | '__/ _ \ / _` | '__/ _` | '_ ` _ \ 
|  __/| | | (_) | (_| | | | (_| | | | | | |
|_|   |_|  \___/ \__, |_|  \__,_|_| |_| |_|
                 |___/                     
*/
    /*
     *     INITIALIZE
     */
    private Set<String> dummyDevices = new HashSet<String>();

    /*
     *  INITIALIZE
     * --------------------
     */
    public void initialize() throws Exception {
        
        scanDevices( devices , _devices, "devices"  ) ;
        /*
         * Print Devices.
         */
        dumpDevices();

    }
    /*
     *     EXECUTE
     * ---------------------------------
     */
    public void execute() {
        devices.hallway.heater.temperature = (float)23.5 ;
    }
    /*
     *     SETVALUE
     * ------------------
     */
    public boolean setValue( String key , String value ) 
        throws  Exception  {

        Device device = _devices.get(key);
        if( device == null ){
            if( ! dummyDevices.contains(key) ){
                dummyDevices.add(key);
                throw new
                    IllegalArgumentException("Device not found (reported only once): "+key);
            }
            return false ;
        }
        return device.setValue(value);
    }
    /*
     *      GET MODIFIED ACTORS
     * --------------------------------
     */
    public Map<String,String> getModifiedActors(){
        Map<String,String> outmap = new HashMap<String,String>();
        for( Device device : _devices.values() ){
            try{
                if( device.isReport() && device.isChanged() )
                    outmap.put( device.getName() , device.getString() ) ;
            }catch(Exception ee){

            }
        }
        return outmap;
    }
/*          
 _____             _            
| ____|_ __   __ _(_)_ __   ___ 
|  _| | '_ \ / _` | | '_ \ / _ \
| |___| | | | (_| | | | | |  __/
|_____|_| |_|\__, |_|_| |_|\___|
             |___/              
         
*/
    private Map<String,Device> _devices = new HashMap<String,Device>();
       private void dumpDevices(){
        System.out.println("\n ------ Listing devices!\n");
        //for( Device device : _devices.values() ){
        //    System.out.println(device.toString());
        //}
        List<String> sortedNames = new ArrayList<>(_devices.keySet()) ;
        Collections.sort(sortedNames);
        int maxNameLength = 0 ;
        for( String name : sortedNames )
            maxNameLength = Math.max(name.length(),maxNameLength);
        int maxValueLength    = 0 ;
        int maxPreviousLength = 0 ;
        for( Device device : _devices.values() )
            try{
                maxValueLength = Math.max(device.getString().length() , maxValueLength ) ;
                maxPreviousLength = Math.max(device.getPreviousString().length(),maxPreviousLength);
            }catch(Exception ee ){}

        for( String name : sortedNames ){
            Device device = _devices.get(name);
            StringBuffer sb = new StringBuffer() ;

            int n = maxNameLength - name.length() ;
            sb.append(name).append(" ").append(".".repeat(n)).append(" ") ;
            if( device instanceof Sensor ){
                sb.append("[S");
            }else if( device instanceof Actor ){
                sb.append("[A");
            }else if( device instanceof Mono ){
                sb.append("[M");
            }else{
                sb.append("[D");
            }
            sb.append(device.getValueClass().getName().substring(0,1));
            try{
                sb.append(device.isChanged()?"C":".") ;
            }catch(Exception ee){
                sb.append("X") ;
            }
            sb.append("] ");
            try{
                String value = device.getString() ;
                n = value.length() ;
                sb.append(" ".repeat(maxValueLength-n)).append(value).append(" ");
            }catch(Exception ee){
                sb.append("".repeat(maxValueLength-5)).append("error ");
            }
            String value = device.getPreviousString() ;
            n = value.length() ;
            sb.append( " [").append(" ".repeat(maxPreviousLength-n)).append(value).append("] ");

            sb.append("(").append(device.getOptionsByString()).append(")");
            System.out.println(sb.toString());
        }
    }
    public Device getDevice( String key ) throws Exception {
        Device device = _devices.get(key);
        if( device == null )throw new IllegalArgumentException("No such device : "+key);
        return device ;
    }
    private class Device {
        private String name ;
        private Set<String> set = null ;
        private Field  field ;
        private Class  clazz ;
        private Object object ;
        private String previous = "" ;
        private boolean trigger         = false ;
        private boolean triggerOnChange = false ;
        private boolean report          = false ;

        private Device( String name ){
            this.name = name ;
        }
        public boolean setValue( String value ) throws Exception {
            this.setReflectionValue( value );
            String previous = this.previous ;
            this.previous   = value ;
            if( this.previous.equals("") ){
                return false ;
            }else if( this.triggerOnChange && ! value.equals(previous) ){
                return true ;
            }else if( this.trigger ){
                return true ;
            }
            return false ;

        }
        public void setReflectionValue( String value ) throws Exception {
            if( this.clazz == int.class ){
                this.field.setInt( this.object , Integer.parseInt( value) ) ;
            }else if( this.clazz == float.class ){
                this.field.setFloat( this.object , Float.parseFloat( value) ) ;
            }else if( this.clazz == double.class ){
                this.field.setDouble( this.object , Double.parseDouble( value) ) ;
            }else if( this.clazz == long.class ){
                this.field.setBoolean( this.object , Boolean.parseBoolean( value) ) ;
            }else if( this.clazz == java.lang.String.class ){
                this.field.set( this.object , value ) ;
            }else{
                throw new 
                Exception("Unsupported internal Class of <"+this.name+"> "+this.clazz.toString());
            }
        }
        public void setBoolean( boolean value ) throws Exception {
            this.field.setBoolean( this.object , value ) ;
        }
        public String getOptionsByString(){
            StringBuffer sb = new StringBuffer() ;
            if( this.set != null ){
                for( String option : this.set )sb.append(option).append(",") ;
            }    
            return sb.toString();       
        }
        public String toString(){
            StringBuffer sb = new StringBuffer();
            sb.append("N=").append(this.name).append(";");
            sb.append("C=").append(this.clazz.getName()).append(";");
            sb.append("V=");
            try{
                sb.append(this.getString());
            }catch(Exception ee ){
                sb.append("Exception("+ee.getMessage()+")") ;
            }
            sb.append(";");
            if( this.set != null ){
                sb.append("O=[") ;
                sb.append(getOptionsByString()) ;
                sb.append("]");
            }
            return sb.toString();
        }
        public boolean isChanged() throws Exception {
            return ! this.getString().equals(this.previous);
        }
        public boolean isReport(){
            return this.report ;
        }
        public void reset() throws Exception {
            this.previous = this.getString() ;
        }
        public Class getValueClass(){
            return this.clazz ;
        }
        public int getInt() throws Exception {
            return this.field.getInt(this.object);
        }    
        public float getFloat() throws Exception {
            return this.field.getFloat(this.object);
        }
        public double getDouble() throws Exception {
            return this.field.getDouble(this.object);
        }
        public boolean getBoolean() throws Exception {
            return this.field.getBoolean(this.object);
        }
        public String getString() throws Exception {
            return this.field.get(this.object).toString();
        }
        public String getPreviousString(){
            return this.previous ;
        }
        private void setReflection( Field field , Object object , Class type ){
            this.field  = field ;
            this.object = object ;
            this.clazz  = type ;
        }
        void addOption(String option ){
            if( option.equals("trigger") ){
                this.trigger = true ;
            }else if( option.equals("tiggerOnChange" ) ){
                this.triggerOnChange = true ;
            }else if( option.equals("report") ){
                this.report = true ;
            }else{
                if( this.set ==  null )this.set = new HashSet<String>();
                this.set.add(option);
            }
        }
        private boolean hasOption(String option){
            return this.set == null ? false : this.set.contains(option) ;
        }
        String getName(){ return this.name ; }
    }
    private class Sensor extends Device {
        private Mono observer = null ;
        private Sensor( String name ){
            super( name ) ;
        }
        private void setObserver( Mono observer ){
            this.observer = observer ;
        }
        public boolean setValue( String value ) throws Exception {
            boolean mustTrigger = super.setValue(value);
            if( this.observer != null )this.observer.trigger(this) ;
            return mustTrigger ;
        }
        public String toString(){
            StringBuffer sb = new StringBuffer() ;
            sb.append("Sensor(") ;
            if( this.observer != null ){
                sb.append(this.observer.getName());
            }
            sb.append(");").append(super.toString());
            return sb.toString() ;
        }
    }
    private class Actor extends Device {
        private Actor( String name ){
            super( name ) ;
            addOption("report");
        }
        public String toString(){
            return "Actor();"+super.toString(); 
        }
    }
    private class Mono extends Device {
        private String target  = null ;
        private long   delay   = 0L ;
        private long   started = 0L ;
        private boolean noRetrigger     = false ;
        private boolean triggerOnChange = false ;
        private Mono( String name ){
            super( name ) ;
        }
        private void setTarget( String target ){
            this.target = target ;
        }
        private String getTarget(){
            return this.target ;
        }
        private void setDelay( long delay ){
            this.delay = delay ;
        }        
        private void setDescriminator(){
            this.delay = -1 ;
        }
        public void addOption( String option ) {
            if( option.equals("noRetrigger") ){
                this.noRetrigger = true ;
            }else if( option.equals("triggerOnChange" ) ){
                this.triggerOnChange = true ;
            }else{
                super.addOption(option);
            }
        }
        private void trigger( Device source ) throws Exception {
            if( delay < 0 ){

            }else{
                this.started = System.currentTimeMillis() ;
                super.setBoolean(true);
            }
        }
        public String toString(){
            return "Mono("+this.target+","+delay+");"+super.toString(); 
        }
        public boolean check() throws Exception {
            boolean isTriggered = ! ( ( System.currentTimeMillis() - started ) > delay ) ;
            super.setBoolean(isTriggered) ;
            return isTriggered ;
        }
        public void reset() throws Exception {
            super.reset() ;

        }
    }
    public void resetDevices(){
        for( Device device : _devices.values() ){
            try{
                device.reset();
            }catch(Exception ee ){

            }
        }
       
    }
    private void  scanDevices( Object obj , Map<String,Device> map , String prefix) 
        throws Exception {
        walkObject( obj , map , prefix ) ;
        for( Device device : map.values() ){
            if( device instanceof Mono ){
                Mono mono = (Mono)device ;
                String targetName = mono.getTarget() ;
                Device target = map.get(targetName) ;
                if( ( target == null ) || ! (target instanceof Sensor ) )
                    throw new
                    IllegalArgumentException(
                    "Program error: target("+targetName+
                    ") of Mono "+device.getName()+" not found or not a Sensor!");
                ((Sensor)target).setObserver(mono);
            }
        }
    }
    private void  walkObject( Object obj , Map<String,Device> map , String prefix) throws Exception {
        Class cls = obj.getClass();
     //   System.out.println("Walking starts at "+cls.getName());
        System.out.println("Walking starts at "+prefix+" ["+cls.getName()+"]" );
        
        Field fieldlist[] = cls.getDeclaredFields() ; 
        for( int i = 0 ; i < fieldlist.length ; i++ ){

            Field field  = fieldlist[i];
            if( field.getName().equals("this$0") )continue ;
            String name  = prefix+"."+field.getName();
            Class type   = field.getType();
            boolean p    = type.isPrimitive() ;
            
            System.out.println("Found : "+field.getName()+" ["+type.getName()+"]");

            DeviceAnnotation dd = field.getAnnotation(DeviceAnnotation.class);
            String annotations  = dd == null ? "" : dd.value();

            if( p  || ( type == java.lang.String.class ) ){
                    Device device = createDevice( name , field , obj, type, annotations );
                    map.put( name , device );
            }else{
                walkObject( field.get(obj) , map , name );
            }
        }
    }
    public List<String> parseAnnotation(String annotation ) throws IllegalArgumentException {

        List<String> list = new ArrayList<String>() ;
        String  pattern   = 
        "^(global|sensor|actor|mono)\\(((?:[a-zA-Z0-9_]"+
        "[a-zA-Z0-9_\\.]*)(?:,[a-zA-Z0-9_][a-zA-Z0-9_\\.]*)*)?\\)$";
        Pattern regex     = Pattern.compile(pattern);
        Matcher matcher   = regex.matcher(annotation);

        if (matcher.matches()) {
            String function = matcher.group(1);           // e.g., sensor
            list.add(function);

            String argList  = matcher.group(2);            // e.g., "a,b,c,d4"

            if (argList != null && !argList.isEmpty()) {
                String[] argsSplit = argList.split(",");
                for (int i = 0; i < argsSplit.length; i++)list.add(argsSplit[i]) ;
            }
        } else {
            throw new
            IllegalArgumentException("Syntax error in Annotation : "+annotation);
        }
        return list ;
    }

    private Device createDevice( String name , 
                                 Field field , 
                                 Object obj , 
                                 Class type , 
                                 String annotations )
                throws IllegalArgumentException {
        /*
         * Syntax :
         *     sensor(trigger|onChange)
         *     actor
         *     mono(<target>,<delay>[,onChange])
         */
        annotations = annotations.strip();
        if( annotations.length() ==0 ){
            /*
             * Just a global variable.
             */
            Device device = new Device(name);
            device.setReflection( field , obj , type ) ;
            return device ;
        }
        List<String> list = parseAnnotation( annotations ) ;
        Device device = null ;
        int    cursor = 0 ;
        int    max    = list.size() ;

        String func   = list.get(cursor++);

        if( func.equals("actor" ) ){

            device = new Actor( name ) ;

        }else if( func.equals("mono" ) ){
            if( cursor >= max)
                throw new 
                IllegalArgumentException("mono of <"+name+"> expects > 0, found 0!") ;

            Mono mono = new Mono( name ) ;

            String targetName = list.get(cursor++) ;
            if( targetName.indexOf(".") == -1 ){
                int dot = name.lastIndexOf(".") + 1 ;
                targetName = name.substring(0,dot) + targetName ;
            }
            mono.setTarget( targetName ) ;

            if( cursor < max )mono.setDelay( Integer.parseInt( list.get(cursor++) ) ) ;

            device = mono ;
                
        }else if( func.equals("sensor" ) ){
            device = new Sensor(name) ;
        }
        
        while( cursor < max )device.addOption(list.get(cursor++)) ;             
        
        device.setReflection( field , obj , type ) ;

        return device ;
    }
    public SimpleJavaEngine( ModuleContext context ){

      //  new Thread(this,"executing").start() ;
      //  new Thread(this,"ingest").start() ;
      //  new Thread(this,"reporting").start() ;

    }
    public void run()  {
        Thread current = Thread.currentThread() ;
        if( "executing".equals(current.getName())){
            run_execution() ;
        }else if("ingest".equals(current.getName())){
            run_ingest() ;
        }else if("reporting".equals(current.getName())){
            run_reporting() ;
        }
    }
    private void run_execution(){

    }
    private void run_ingest(){

    }
    private void run_reporting(){

    }
    public static void main( String [] args ) throws Exception {
        Map<String,String> contextMap = new HashMap<String,String>();
        ModuleContext context = new ModuleContext( "engine" , contextMap );
        SimpleJavaEngine engine = new SimpleJavaEngine( context ) ;
        engine.initialize();
        engine.execute() ;
        engine.dumpDevices() ;
        engine.resetDevices();
        engine.setValue("devices.hallway.heater.temperature","30.0");
        engine.dumpDevices();
        System.out.println("Result : "+
        engine.getDevice("devices.hallway.heater.temperature").getFloat());
    }
}
