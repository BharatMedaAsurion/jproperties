package net.jmatrix.jproperties.parser;

import java.io.File;
import java.util.*;

import net.jmatrix.eproperties.utils.JDK14LogConfig;
import net.jmatrix.jproperties.JProperties;

import org.apache.commons.logging.*;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.TypeReference;

public class JsonEPropertiesParser {
   public static Log log=LogFactory.getLog(JsonEPropertiesParser.class);

   /** */
   public static void main(String[] args) throws Exception {
      JDK14LogConfig.startup();

      File input=new File(args[0]);
      
      System.out.println("Parsing "+input);
      log.debug("parsing "+input);
      
      TypeReference<HashMap<String,Object>> typeRef  = 
            new TypeReference<HashMap<String,Object>>() {}; 
             
             
      JsonFactory factory=new JsonFactory();
      factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
      
      ObjectMapper om=new ObjectMapper(factory);
      Map<String, Object> map=om.readValue(input, typeRef);
      
      JProperties p=new JProperties(map);
      
      System.out.println("Parsed tree: "+p);
      
      System.out.println("Tree: "+jsonDebug(p));
      
      System.out.println ("Nested object is "+p.get("nested").getClass().getName());
      System.out.println("List is "+p.get("list").getClass().getName());
      
   }
   
   public static final String jsonDebug(Object o) {
      return jsonDebug(o, true);
   }
   
   public static final String jsonDebug(Object o, boolean indent) {
      ObjectMapper om=new ObjectMapper();
      om.configure(SerializationConfig.Feature.INDENT_OUTPUT, indent);
      om.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
      try {
         if (o == null)
            return "null";
         return om.writeValueAsString(o);
      } catch (Exception ex) {
         throw new RuntimeException("Error in debug serialization.", ex);
      }
   }
}
