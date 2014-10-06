package net.jmatrix.jproperties.post;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jmatrix.jproperties.JPRuntimeException;
import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.substitution.SubstitutionProcessor;
import net.jmatrix.jproperties.util.ClassLogFactory;

import org.apache.commons.logging.Log;

/**
 * Processes any includes.  Includes must be VALUES in valid 
 * json "KEY":"VALUE" constructs.
 * 
 * Include syntax is: 
 * 
 *   $[]
 *   
 *   To include a file, relative or absolute:
 *   $[services/foo.jproperties]
 *   
 *   $[services/bar.properties|parse=false,failonerror=false]
 *   
 *   Can also accept method includsion: 
 *   $[method://java.lang.System.getProperties()]
 *   
 *   or URL based inclusion: 
 *   $[http://foo.bar.com/baz.properties]
 */
public class IncludeProcessor {
   static final Log log=ClassLogFactory.getLog();
   
   static String INCLUDE_REGEX="^\\$\\[(.*)\\]$";
   
   static Pattern INCLUDE_PATTERN=Pattern.compile(INCLUDE_REGEX);
   
   //@Override
   public void post(JProperties p) {
      for (String key:p.keySet()) {
         Object ovalue=p.get(key);
         
         if (ovalue instanceof String) {
            String svalue=(String) ovalue;
            
            if (Pattern.matches(INCLUDE_REGEX, svalue)) {
               log.debug(key+" include: "+svalue);
               
               // Can return JProperties or Object.
               Object included=include(svalue, p);
               
               if (included != null && included instanceof JProperties) {
                  ((JProperties)included).setParent(p);
               }
               // replace, may be replacing with null
               p.put(key, included);
            }
         } else if (ovalue instanceof JProperties) {
            post((JProperties)ovalue); // recursion
         } else {
            // nothing to do.
         }
      }
   }
   
   public static final boolean containsInclude(Object value) {
      if (value == null)
         return false;
      if (value instanceof String) {
         String svalue=(String)value;
         return Pattern.matches(INCLUDE_REGEX, svalue);
      }
      return false;
   }
   
   ///////////////////////////////////////////////////////////////////////////
   
   /** Top entry into include, delegates to various types of includers */
   public static final Object include(String value, JProperties parent) {
      log.debug("include "+value);
      
      Matcher matcher=INCLUDE_PATTERN.matcher(value);
      matcher.matches();
      
      String include=matcher.group(1);
      log.debug("include: "+include);
      
      if (SubstitutionProcessor.containsTokens(include)) {
         include=SubstitutionProcessor.processSubstitution(include, parent);
         log.debug("Post Substitution: '"+include+"'");
      }
      
      // if it still contains tokens, that's an error (substitution failed).
      if (SubstitutionProcessor.containsTokens(include)) {
         // fail?
         throw new 
         JPRuntimeException("Unresolvable Substitution in Include directive '"+
               include+"'");
      }
      
      String split[]=include.split("\\|");
      String url=split[0];
      String soptions=(split.length>1?split[1]:null);
      
      Options options=new Options(soptions);
      
      if (url.startsWith("method://")) {
         return includeMethodUrl(url, options);
      } else {
         // assume it is a url, or relative file/link
         URLPropertiesLoader upl=new URLPropertiesLoader();
         return upl.loadProperties(parent, url, options);
      }
   }
   
   /** */
   private static final Object includeMethodUrl(String url, Options options) {
      url=url.substring("method://".length());
      
      // java.lang.System.getenv()
      // java.land.System.getenv("foo")
      // java.lang.System.getProperties()
      // java.lang.System.getProperty("foo");
      
      // parse the classs name and method name.
      String sargs=url.substring(url.indexOf("(")+1, url.indexOf(")"));
      url=url.substring(0,url.length()-(sargs.length()+2));
      
      String className=url.substring(0, url.lastIndexOf("."));
      String methodName=url.substring(url.lastIndexOf(".")+1);
      
      Object methodArgs[]=null;
      
      
      if (sargs.length() > 0) {
         String aargs[]=sargs.split("\\,");
         
         for (int i=0; i<aargs.length; i++) {
            aargs[i]=aargs[i].trim();
            if (aargs[i].startsWith("\"") && aargs[i].endsWith("\"")) {
               aargs[i]=aargs[i].substring(1, aargs[i].length()-1);
            }
         }
         methodArgs=aargs;
      }
      
      log.info("Method name '"+methodName+"'");
      log.info("sargs: "+sargs);
      log.info(""+(methodArgs == null?"":Arrays.asList(methodArgs).toString()));
      
      try {
         Class clazz=Class.forName(className);
         
         Class parms[]=null;
         if (methodArgs != null) {
            parms=new Class[methodArgs.length];
            for (int i=0; i<methodArgs.length; i++) {
               parms[i]=methodArgs[i].getClass();
            }
         }
         
         Method method=clazz.getMethod(methodName, parms);
         
         Object obj=null;
         
         if (methodArgs == null) {
            obj=method.invoke(null);
         } else {
            obj=method.invoke(null, methodArgs);
         }
         
         if (obj == null) {
            log.error("Method include of "+className+"."+methodName+"("+sargs+") returned null.");
         } else if (obj instanceof Map) {
            JProperties p=new JProperties((Map)obj);
            
            log.debug("Loaded "+p.size()+" properties with MethodLoader.");
            
            return p;
         } else if (obj instanceof String) {
            return obj;
         } else {
            String m="Error including "+url+
                  ", don't know how to process result type "+obj.getClass().getName();
            if (options.failonerror)
               throw new JPRuntimeException(m);
            else
               log.warn(m);
         }
      } catch (Exception ex) {
         String m="Error processing "+url+" inclusion.";
         if (options.failonerror)
            throw new JPRuntimeException(m, ex);
         else
            log.warn(m);
      }
      return null;
   }
}
