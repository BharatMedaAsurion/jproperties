package net.jmatrix.jproperties.parser;

import java.io.*;
import java.net.URL;
import java.util.*;

import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.post.*;
import net.jmatrix.jproperties.util.*;

import org.apache.commons.logging.Log;
import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.TypeReference;

public class Parser {
   public static Log log=ClassLogFactory.getLog();

   /** */
   public static void main(String[] args) throws Exception {
      JDK14LogConfig.startup();

      File input=new File(args[0]);
      
      JProperties p=parse(input);
      
      
      System.out.println("Parsed tree: "+p);
      
      System.out.println("PreTree: "+jsonDebug(p));
      
      for (String key:p.keySet()) {
         System.out.println ("   key: "+key);
      }
      
      
      PostProcessor post=new IncludePostProcessor();
      post.post(p);
      
      log.debug("Done Postprocessing.");
      
      System.out.println("PostTree: "+jsonDebug(p));
      
      //System.out.println ("Home: "+p.get("home"));
   }
   
   public static JProperties parse(File f) throws IOException {
      JProperties p=parse(new FileReader(f));
      p.setUrl(f.toURI().toURL().toString());
      
      PostProcessor post=new IncludePostProcessor();
      post.post(p);
      
      return p;
   }
   
   public static JProperties parse(URL url) throws IOException {
      JProperties p=parse(new InputStreamReader(url.openStream()));
      p.setUrl(url.toString());
      
      PostProcessor post=new IncludePostProcessor();
      post.post(p);
      
      return p;
   }
   
   public static JProperties parse(Reader r) throws JsonParseException, JsonMappingException, IOException {
      TypeReference<LinkedHashMap<String,Object>> typeRef  = 
            new TypeReference<LinkedHashMap<String,Object>>() {}; 
             
             
      JsonFactory factory=new JsonFactory();
      factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
      
      ObjectMapper om=new ObjectMapper(factory);
      Map<String, Object> map=om.readValue(r, typeRef);
      
      JProperties p=new JProperties(map);
      
      return p;
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
