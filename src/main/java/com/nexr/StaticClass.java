package com.nexr;

import org.springframework.stereotype.Service;

@Service
public class StaticClass {
    public static String getenv(String key){
        return System.getenv(key);
    }
}
