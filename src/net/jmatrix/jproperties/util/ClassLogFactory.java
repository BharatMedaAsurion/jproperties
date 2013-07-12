package net.jmatrix.jproperties.util;

import org.apache.commons.logging.Log;

/** */
public class ClassLogFactory {
   /** */
   public static final Log getLog() {
      String callingClassname=getCallingClassName(1);
      Log log=org.apache.commons.logging.LogFactory.getLog(callingClassname);
      return log;
   }
   
   /** */
   public static final String getCallingClassName(int depth) {
      StackTraceElement stack[]=Thread.currentThread().getStackTrace();
      String thisClassname=ClassLogFactory.class.getName();
      
      int i=0;
      for (i=0; i<stack.length; i++) {
         String classname=stack[i].getClassName();
         //System.out.println (i+":"+classname);
         if (classname != null && classname.equals(thisClassname))
            break;
      }
      int callingEle=i+1+depth;
      if (callingEle<stack.length) 
         return stack[callingEle].getClassName();
      
      return null;
   }
}
