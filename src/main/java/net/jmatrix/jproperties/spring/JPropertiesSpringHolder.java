package net.jmatrix.jproperties.spring;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.WrappedProperties;
import net.jmatrix.jproperties.parser.Parser;
import net.jmatrix.jproperties.util.URLUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * From within Spring, use this class as follows:
 * 
 * 
 * 
 * 
 * 
 */
public class JPropertiesSpringHolder {
   static final Log log=LogFactory.getLog(JPropertiesSpringHolder.class);

   String url;
   String delimiter=".";
   
   JProperties jp=null;
   
   public JPropertiesSpringHolder() {}

   public String getUrl() {
      return url;
   }
   
   /**
    * Fail-fast on the setter - if the proeprties do not exist 
    * at the URL loction, or if the URL is malformed, this should
    * throw - and cause startup to halt (as it should).
    */
   public void setUrl(String url) throws IOException {
      this.url = url;
      
      log.debug("sURL: "+url);

      URL jpUrl=new URL(URLUtil.convertClasspathURL(url));
      
      log.debug("URL: "+jpUrl);
      
      
      jp=Parser.parse(jpUrl);
   }
   
   /** */
   public Properties asProperties() {
      Character delim=null;
      if (delimiter == null)
         delim='.';
      else
         delim=delimiter.toCharArray()[0];
      
      WrappedProperties wp=new WrappedProperties(jp, delim);
      wp.debug=true;
      wp.setAllowKeysWithNullValues(false);
      return wp;
   }
   
   public JProperties getjProperties() {
      return jp;
   }
}
