package net.jmatrix.jproperties.parser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

import net.jmatrix.jproperties.JPRuntimeException;
import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.util.ClassLogFactory;
import net.jmatrix.jproperties.util.GenericLogConfig;
import net.jmatrix.jproperties.util.URLUtil;

import org.apache.commons.logging.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;


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
      log.debug("Done Postprocessing.");
      
      System.out.println("PostTree: "+writeAsJson(p));
      
      //System.out.println ("Home: "+p.get("home"));
   }
   
   public static JProperties parse(String surl) throws MalformedURLException, IOException {
      return parse(new URL(surl));
   }
   
   public static JProperties parse(File f) throws IOException {
      return parse(f.toURI().toURL());
   }
   
   public static JProperties parse(URL url) throws IOException {
      JProperties p=new JProperties();
      parseInto(p, url);
      return p;
   }
   
   /** */
   public static void parseInto(JProperties p, String surl) throws IOException {
      String iurl=URLUtil.convertClasspathURL(surl);
      log.debug("Converted URL '"+surl+"' -> '"+iurl+"'");
      URL url=new URL(iurl); 
      parseInto(p, url);
   }
   
   /** Loads JProperties from a file. */
   public static void parseInto(JProperties p, File f) throws IOException {
      parseInto(p, new FileReader(f),f.toURI().toURL().toString());
   }
   
   public static void parseInto(JProperties p, URL url) throws IOException {
      InputStream is=url.openStream();
      log.debug("Stream opened for "+url+": "+is);
      
      parseInto(p, new InputStreamReader(is), url.toString());
   }
   
   private static void parseInto(JProperties props, Reader r, String surl) throws JsonParseException, JsonMappingException, IOException {
      TypeReference<JProperties> typeRef  = 
            new TypeReference<JProperties>() {}; 
             
             
      JsonFactory factory=new JsonFactory();
      factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
      
      ObjectMapper om=new ObjectMapper(factory);
      
//      TypeFactory typeFactory = om.getTypeFactory();
//      MapType mapType = typeFactory.constructMapType(JProperties.class, String.class, Object.class);
//      
//      om.setTypeFactory(typeFactory.withModifier(new TypeModifier(){
//
//         @Override
//         public JavaType modifyType(JavaType javatype, Type type,
//               TypeBindings bindings, TypeFactory typefactory) {
//            
//            System.out.println("  Jackson asking for type "+javatype+", "+type+", typebinding: "+bindings);
//            return javatype;
//         }
//         
//      }));
      
      //JProperties p=new JProperties();
      props.setUrl(surl);
      
      om.readerForUpdating(props).readValue(r);
      // om.readValue(r, mapType);
   }
   
   /**
    * @throws IOException  */
   public static void write(JProperties jp, Writer w) throws IOException {
      jp.setProcessSubstitutions(false);
      String s=writeAsJson(jp);
      System.err.println("Serialized size: "+s.length());
      
      w.write(s);
      w.flush();
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
      om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      om.enable(SerializationFeature.INDENT_OUTPUT);
      
      try {
         if (o == null)
            return "null";
         return om.writeValueAsString(o);
      } catch (Exception ex) {
         throw new JPRuntimeException("Error in debug serialization.", ex);
      }
   }
}
