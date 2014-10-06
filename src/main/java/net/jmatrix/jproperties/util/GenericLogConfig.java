package net.jmatrix.jproperties.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;


/**
 * Depending on what jars are in the classpath, attemp to startup a 
 * logging system with some sort of debug mode.
 * 
 * Commons logging is used as a log abstraction, but actually initializing
 * an appender is a PITA depending on what's in the classpath.
 * 
 * For now - support either log4j or java.util.logging.  
 * 
 * Minimize compile time dependencies with introspection.
 */
public class GenericLogConfig {
   
   // For verbose initialization of logging itself, set to true and
   // things will come out on System.out.
   public static boolean debug=false;
   
   static Log log=ClassLogFactory.getLog();
   
   
   static final String LOG4J_DEBUG="DEBUG";
   
   /**
    * Attempst to bootstrap some logging system into debug mode, depending
    * on what is in the classpath.
    * 
    * This method should generally be called by CLI main() methods trying 
    * to initialize logging.
    */
   public static void bootstrap() {
      String loggerClassname=log.getClass().getName();
      
      System.out.println ("Log is "+loggerClassname);
      
      if (loggerClassname.toLowerCase().contains("log4j")) {
         initLog4J(LOG4J_DEBUG);
      } else {
         initJavaUtilLogging();
      }
   }
   
   /** */
   public static final void initLog4J(String level) {
      System.out.println("Bootstrapping Log4J w/ Default level '"+level+"'");
      
      Properties p=new Properties();
      p.put("log4j.rootLogger", level+", stdout");
      p.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
      p.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
      p.put("log4j.appender.stdout.layout.ConversionPattern", "%d{HH:mm:ss.SSS} [%t] %-5p %c{1} %x - %m%n");
      
      if (debug) {
         System.out.println ("Trying to bootstrap log4j with:");
         p.list(System.out);
      }
      
      try {
         Class clazz=Class.forName("org.apache.log4j.PropertyConfigurator");
         Method method=clazz.getMethod("configure", new Class[]{Properties.class});
         
         method.invoke(null, new Object[] {p});
      } catch (Exception ex) {
         System.out.println ("Cannot initialize Log4J even though Commons Logger is '"+log.getClass().getName()+"'");
      }
   }
   
   /** */
   public static final void initJavaUtilLogging() {
      LogManager logManager=LogManager.getLogManager();
      
      Logger logger=Logger.getLogger("");
      logger.removeHandler(logger.getHandlers()[0]);
      
      ConsoleHandler consoleHandler=new ConsoleHandler();
      consoleHandler.setFormatter(new LocalLogFormatter());
      consoleHandler.setLevel(Level.FINEST);
      logger.addHandler(consoleHandler);
      
      logger.setLevel(Level.ALL);
//      
//      logger=Logger.getLogger(SubstitutionProcessor.class.getName());
//      logger.setLevel(Level.INFO);
//      
//      logger=Logger.getLogger(EProperties.class.getName());
//      logger.setLevel(Level.INFO);
      
      if (debug) {
         Enumeration loggers=logManager.getLoggerNames();
         
         while (loggers.hasMoreElements()) {
            String name=(String)loggers.nextElement();
            Logger l=Logger.getLogger(name);
            System.out.println ("   Loggger: "+name);
            System.out.println ("          level: "+l.getLevel());
            System.out.println ("       handlers: "+Arrays.asList(l.getHandlers()));
            System.out.println ("       use parent?: "+l.getUseParentHandlers());
         }
      }
   }
   
   
   
   /**
   *
   */
  static class LocalLogFormatter extends Formatter {
     
     DateFormat df=new SimpleDateFormat("HH:mm:ss.SSS");
     
     /** */
     @Override
     public String format(LogRecord record) {
        
        StringBuilder sb=new StringBuilder();
        
        synchronized(df) {
           sb.append(df.format(new Date(record.getMillis())) + " ");
        }
        
        sb.append(record.getLevel()+" ");
        sb.append(record.getThreadID()+":"+Thread.currentThread().getName()+" ");
        sb.append(shortLoggerName(record.getLoggerName()+" "));
        sb.append(record.getMessage());
        
        if (record.getThrown() != null) {
           ByteArrayOutputStream baos=new ByteArrayOutputStream();
           PrintWriter pw=new PrintWriter(new OutputStreamWriter(baos));
           record.getThrown().printStackTrace(pw);
           pw.flush();
           sb.append("\n"+baos.toString());
        } else {
           sb.append("\n");
        }
        return sb.toString();
     }
     
     private static final String shortLoggerName(String loggerName) {
        return loggerName.substring(loggerName.lastIndexOf(".")+1);
     }
  }
}
