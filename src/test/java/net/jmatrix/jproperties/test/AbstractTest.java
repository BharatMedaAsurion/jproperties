package net.jmatrix.jproperties.test;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.jmatrix.jproperties.util.GenericLogConfig;

public class AbstractTest {
   
   static DateFormat df=new SimpleDateFormat("HH:mm:ss.SSS");
   
   static {
      GenericLogConfig.debug=true;
      GenericLogConfig.bootstrap();
   }
   
   protected void log(String s) {
      log(s, null);
   }
   
   protected void log(String s, Throwable t) {
      String time=null;
      synchronized (df) {
         time=df.format(new Date());
      }
      
      
      System.out.println (time+" ["+Thread.currentThread().getName()+"]: "+s);
      
      if (t != null)
         t.printStackTrace();
   }
   
   protected void log(Throwable t) {
      log(null, t);
   }
   
   File getTestFile(String path) {
      URL url=getTestFileURL(path);
      if( url == null)
         return null;
      return new File(url.getFile());
   }
   
   URL getTestFileURL(String path) {
      URL url=getClass().getResource(path);
      
      log("URL for path "+path);
      log("             "+url);
      
      return url;
   }
}
