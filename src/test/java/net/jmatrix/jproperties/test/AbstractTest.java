package net.jmatrix.jproperties.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AbstractTest {
   
   static DateFormat df=new SimpleDateFormat("HH:mm:ss.SSS");
   
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
}
