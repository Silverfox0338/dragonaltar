package com.dragonaltar.persistence;
import java.util.*;
public final class CooldownCodec {
    private CooldownCodec(){}
    public static Map<String,Object> encode(Map<String,Long> values){Map<String,Object> result=new LinkedHashMap<>();values.forEach((key,value)->{if(key!=null&&!key.isBlank()&&value!=null&&value>=0)result.put(key,value);});return result;}
    public static Map<String,Long> decode(Map<?,?> values){Map<String,Long> result=new LinkedHashMap<>();values.forEach((key,value)->{if(key instanceof String text&&value instanceof Number number&&number.longValue()>=0)result.put(text,number.longValue());});return result;}
}
