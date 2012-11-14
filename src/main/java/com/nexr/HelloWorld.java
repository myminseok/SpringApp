package com.nexr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HelloWorld {
    private final Logger logger = LoggerFactory.getLogger(HelloWorld.class);
    private String name;
    
    @Autowired
    private StaticClass staticClass;
    
    public void setName(String name) {
        this.name = name;
    }
 
    public void printHello() {
        logger.info("Hello ! " + name);
    }
    
    public static String getenv(String key){
        return System.getenv(key);
    }
    
    
    public String getenvStaticClass(String key){
        return staticClass.getenv(key);
    }
   
}