package net.jmatrix.jproperties.test;

import java.util.List;

import net.jmatrix.jproperties.JProperties;

public class JPValidate {
   
   
   public static void validate(JProperties jp) {
      
      for (String key:jp.keySet()) {
         Object val=jp.get(key);
         
         if (val != null) {
            if (!(val instanceof String) &&
                !(val instanceof List) &&
                !(val instanceof JProperties)) {
               throw new RuntimeException("Exception at key '"+key+"' value "+val+" is class "+val.getClass());
            }
         }
      }
   }
}
