package net.jmatrix.jproperties.parser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import net.jmatrix.jproperties.JPRuntimeException;
import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.post.IncludePostProcessor;
import net.jmatrix.jproperties.post.PostProcessor;
import net.jmatrix.jproperties.util.ClassLogFactory;
import net.jmatrix.jproperties.util.GenericLogConfig;
import net.jmatrix.jproperties.util.URLUtil;

import org.apache.commons.logging.Log;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.type.TypeReference;

public class Parser {
   public static Log log=ClassLogFactory.getLog();

   /** */
   public static void main(String[] args) throws Exception {
      GenericLogConfig.bootstrap();
      File input=new File(args[0]);
      
      JProperties p=parse(input);
      
      
      System.out.println("Parsed tree: "+p);
      
      System.out.println("PreTree: "+writeAsJson(p));
      
      for (String key:p.keySet()) {
         System.out.println ("   key: "+key);
      }
      
      PostProcessor post=new IncludePostProcessor();
      post.post(p);
      
      log.debug("Done Postprocessing.");
      
      System.out.println("PostTree: "+writeAsJson(p));
      
      //System.out.println ("Home: "+p.get("home"));
   }
   
   /** */
   public static JProperties parse(String surl) throws IOException {
      String iurl=URLUtil.convertClasspathURL(surl);
      log.debug("Parser convered URL '"+surl+"' -> '"+iurl+"'");
      URL url=new URL(iurl); 
      return parse(url);
   }
   
   /** Loads JProperties from a file. */
   public static JProperties parse(File f) throws IOException {
      JProperties p=parse(new FileReader(f));
      p.setUrl(f.toURI().toURL().toString());
      
      PostProcessor post=new IncludePostProcessor();
      post.post(p);
      
      return p;
   }
   
   public static JProperties parse(URL url) throws IOException {
      InputStream is=url.openStream();
      log.debug("Stream opend for "+url+": "+is);
      
      JProperties p=parse(new InputStreamReader(is));
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
   
   /**
    * @throws IOException  */
   public static void write(JProperties jp, Writer w) throws IOException {
      jp.setProcessSubstitutions(false);
      String s=writeAsJson(jp);
      System.err.println("Serialized size: "+s.length());
      
      w.write(s);
      w.flush();
      //w.close();
   }
   
   /** */
   public static void write(JProperties jp, File f) throws IOException {
      write(jp, new FileWriter(f));
   }
   
   public static String toJson(JProperties jp) {
      return writeAsJson(jp);
   }
   
   public static final String writeAsJson(Object o) {
      return writeAsJson(o, true);
   }
   
   public static final String writeAsJson(Object o, boolean indent) {
      ObjectMapper om=new ObjectMapper();
      om.configure(SerializationConfig.Feature.INDENT_OUTPUT, indent);
      om.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
      try {
         if (o == null)
            return "null";
         return om.writeValueAsString(o);
      } catch (Exception ex) {
         throw new JPRuntimeException("Error in debug serialization.", ex);
      }
   }
}
