package org.rosi.modules.engine;
import java.lang.annotation.* ;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DeviceAnnotation { String value() ; }
