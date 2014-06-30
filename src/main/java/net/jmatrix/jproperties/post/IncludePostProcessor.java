package net.jmatrix.jproperties.post;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class IncludePostProcessor implements PostProcessor {
   static final Log log=ClassLogFactory.getLog();
   
   static String INCLUDE_REGEX="^\\$\\[(.*)\\]$";
   
   static Pattern INCLUDE_PATTERN=Pattern.compile(INCLUDE_REGEX);
   
   @Override
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
   
   ///////////////////////////////////////////////////////////////////////////
   
   /** Top entry into include, delegates to various types of includers */
   private static final Object include(String value, JProperties parent) {
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
         RuntimeException("Unresolvable Substitution in Include directive '"+
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
   private static final JProperties includeMethodUrl(String url, Options options) {
      url=url.substring("method://".length());
      
      // java.lang.System.getenv()
      
      // parse the classs name and method name.
      String className=url.substring(0, url.lastIndexOf("."));
      String methodName=url.substring(url.lastIndexOf(".")+1);
      
      if (methodName.endsWith("()"))
         methodName=methodName.substring(0, methodName.length()-2);
      
      try {
         Class clazz=Class.forName(className);
         
         Method method=clazz.getMethod(methodName);
         
         Object obj=method.invoke(null);
         
         if (obj == null) {
            log.error("Method include of "+className+"."+methodName+" returned null.");
         }
         
         if (obj instanceof Map) {
            JProperties p=new JProperties((Map)obj);
            
            log.debug("Loaded "+p.size()+" properties with MethodLoader.");
            
            return p;
         } else {
            String m="Error including "+url+
                  ", don't know how to process result type "+obj.getClass().getName();
            if (options.failonerror)
               throw new RuntimeException(m);
            else
               log.warn(m);
         }
      } catch (Exception ex) {
         String m="Error processing "+url+" inclusion.";
         if (options.failonerror)
            throw new RuntimeException(m, ex);
         else
            log.warn(m);
      }
      return null;
   }
}
