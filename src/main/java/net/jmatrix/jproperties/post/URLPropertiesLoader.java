package net.jmatrix.jproperties.post;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import net.jmatrix.jproperties.JPRuntimeException;
import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.parser.Parser;
import net.jmatrix.jproperties.util.StreamUtil;
import net.jmatrix.jproperties.util.URLUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads properties from file, http(s), or classpath locations. 
 */
public class URLPropertiesLoader {
   static Log log=LogFactory.getLog(URLPropertiesLoader.class);
   
   /** */
   public boolean acceptsURL(String s) {
      if (s == null)
         return false;
      
      if (s.toLowerCase().startsWith("file:/") ||
          s.toLowerCase().startsWith("http://") ||
          s.toLowerCase().startsWith("https://") ||
          s.toLowerCase().startsWith("classpath:/")) {
         return true;
      }
      return false;
   }

   /** */
   public Object loadProperties(JProperties parent, String surl, 
         Options options) {

      Object result=null;
      
      //surl=surl.toLowerCase();
      
      // Translate classpath:// URLs to Java internal URLs.
      surl=URLUtil.convertClasspathURL(surl);
      String lcurl=surl.toLowerCase();    
      
      if (lcurl.startsWith("http://") || lcurl.startsWith("https://") ||
          lcurl.startsWith("file:/") || lcurl.startsWith("jar:file:/")) {
         log.debug("Loading as absolute URL: "+surl); 
         result=loadFromURL(surl, options, parent);
      } else if (new File(surl).isFile()) { 
         log.debug("Loading as absolute file: "+surl);
         // ok, just try to open it as a local file.
         File f=new File(surl);
         if (f.exists()) {
            try {
               result=loadFromURL(f.toURI().toURL().toExternalForm(), options, parent);
               //props.load(f.toURI().toURL());
            } catch (IOException ex) {
               if (options.failonerror) {
                  throw new 
                  JPRuntimeException("Error loading included properties from '"+
                        surl+"'", ex);
               } else {
                  log.warn("Cannot find file '"+surl+"', failonerror=false");
               }
            }
         } else {
            // the file does not exist.  
            if (options.failonerror) {
               throw new JPRuntimeException("Cannot find file '"+surl+"' to include.");
            } else {
               log.warn("Cannot find file '"+surl+"' to include. failonerror=false");
            }
         }
      } else {
         // ok, the URL does not start with [http:// | https:// | file://], so
         // let's assume it is a relative reference to the most recent parent.
         // This means that if the parent is:
         //   file://../config/config.properties, and the include url is 'include.properties'
         // then the include should be processed from that relative location:
         //   file://../config/include.properties
         //
         // Similarly, if the parent URL is:
         //    http://jmatrix.net/properties/public/foo.properties 
         // and the url is:
         //    '../bar/baz.properties'
         // then the include should be
         //    http://jmatrix.net/properties/public/../bar/baz.properties
         
         String parentURL=parent.findUrl();
         
         log.debug("Loading as relative URL, parent URL is '"+parentURL+"'");
         
         if (parentURL == null) {
            // ok, just try to open it as a local file.
            File f=new File(surl);
            if (f.exists()) {
               try {
                  result=loadFromURL(f.toURI().toURL().toExternalForm(), options, parent);
                  //props.load(f.toURI().toURL());
               } catch (IOException ex) {
                  if (options.failonerror) {
                     throw new 
                     JPRuntimeException("Error loading included properties from '"+
                           surl+"'", ex);
                  } else {
                     log.warn("Cannot find file '"+surl+"', failonerror=false");
                  }
               }
            } else {
               // the file does not exist.  
               if (options.failonerror) {
                  throw new JPRuntimeException("Cannot find file '"+surl+"' to include.");
               } else {
                  log.warn("Cannot find file '"+surl+"' to include. failonerror=false");
               }
            }
         } else {
            // create relative URL string
            String workingSURL=parentURL.toString();
            workingSURL=workingSURL.substring(0, workingSURL.lastIndexOf("/")+1)+surl;
            log.debug("Loading w/ rel URL: '"+workingSURL+"'");
            result=loadFromURL(workingSURL, options, parent);
         }
      }
      
      return result;
   }

   
   /** 
    * Returns either a String or an EProperties object, dependent on 
    * whether the parse property is true or false.
    * 
    * */
   private Object loadFromURL(String surl, Options options, JProperties parent) {
      Object result=null;
      try {
         URL url=new URL(surl);
         if (options.parse) {
            
            if (options.format == Options.Format.PROPERITES) {
               log.debug("Options set to java.util.Properties format.");
               Properties p=new Properties();
               p.load(url.openStream());
               
               JProperties props=new JProperties(p);
               props.setParent(parent);;
               result=props;
            } else {
               JProperties props=Parser.parse(url);
               props.setParent(parent);
               result=props;
            }

         } else {
            // load it as a string
            InputStream is=url.openStream();
            try {
               String value=StreamUtil.readToString(is);
               result=value;
            } finally {
               if (is != null)
                  is.close();
            }
         }
      } catch (IOException ex) {
         if (options.failonerror) {
            throw new 
            JPRuntimeException("Error loading included properties from '"+surl+"'", ex);
         } else {
            log.info("Cannot load properties from  '"+surl+"', failonerror=false");
         }
      }
      return result;
   }
}
