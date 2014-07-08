package net.jmatrix.jproperties;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * WrappedProperties wraps a JProperties instance, and presents a
 * flattened view of the data as a java.util.Properties object.
 * 
 * Useful for backward compatability with some systems that require 
 * java.util.Properties.
 */
public class WrappedProperties extends Properties {
   static final Log log=LogFactory.getLog(WrappedProperties.class);

   
   JProperties jproperties;
   Character delimiter=null;
   
   public boolean debug=false;
   
   boolean allowKeysWithNullValues=true;
   
   /** */
   public WrappedProperties(JProperties jp) {
      this(jp, '.');
   }
   
   /** */
   public WrappedProperties(JProperties jp, Character del) {
      jproperties=jp;
      delimiter=del;
   }
   

   public boolean isAllowKeysWithNullValues() {
      return allowKeysWithNullValues;
   }

   public void setAllowKeysWithNullValues(boolean allowKeysWithNullValues) {
      this.allowKeysWithNullValues = allowKeysWithNullValues;
   }
   
   ///////////////////////////////////////////////////////////////////////////
   
   @Override
   public String getProperty(String key, String def) {
      if (key == null)
         throw new NullPointerException("Key cannot be null.");
      
      String ikey=null;
      if (delimiter != null) {
         String components[]=key.split("\\"+delimiter);
         if (components.length > 1) {
            StringBuilder sb=new StringBuilder();
            for (int i=0; i<components.length-1; i++) {
               sb.append(components[i]+"->");
            }
            sb.append(components[components.length-1]);
            ikey=sb.toString();
         } else {
            ikey=key;
         }
      } else {
         ikey=key;
      }
      
      String value=jproperties.getString(ikey);
      
      if (value == null && def == null) {
         log.info("Value is null for property key '"+key+"'");
      }
      
      return value == null ? def:value;
   }
   
   @Override
   public String getProperty(String key) {
      return getProperty(key, null);
   }
   
   @Override
   public Object get(Object key) {
      if (key == null)
         throw new NullPointerException("Key cannot be null.");
      return getProperty(key.toString());
   }
   
   @Override
   public Enumeration keys() {
      return new KeyEnumeration();
   }
   
   @Override
   public Enumeration propertyNames() {
      return keys();
   }
   
   ///////////////////////////////////////////////////////////////////////////
   class KeyEnumeration implements Enumeration {
      
      Iterator<String> iterator=null;
      
      public KeyEnumeration() {
         log.debug("Creating KeyEnumeration, allowKeysWithNullValues="+allowKeysWithNullValues);

         Set<String> keys=new LinkedHashSet<String>();
         
         if (debug)
            System.err.println ("Building flat key enumeration, delim="+delimiter);
         
         Map<String, Iterator<String>> iterMap=new HashMap<String, Iterator<String>>();
         Map<String, JProperties> propMap=new HashMap<String, JProperties>();
         
         Iterator<String> currentIterator=jproperties.keySet().iterator();
         String currentPath="";
         JProperties currentProps=jproperties;
         
         
         // build the set of keys
         while (currentIterator != null && currentPath != null) {
            if (!currentIterator.hasNext()) {
               
               // back up.
               String parentPath=null;
               if (currentPath.indexOf(""+delimiter) != -1) 
                  parentPath=currentPath.substring(0, currentPath.lastIndexOf(""+delimiter));
               else if (currentPath == "")
                  parentPath=null;
               else 
                  parentPath="";
               
               if (debug)
                  System.err.println ("  Backing up to '"+parentPath+"'");
               
               if (parentPath != null) {
                  currentIterator=iterMap.get(parentPath);
                  currentProps=propMap.get(parentPath);
               }
               currentPath=parentPath;
            } else {
               String key=currentIterator.next();
               
               if (key.contains(""+delimiter)) {
                  throw new JPRuntimeException("Key '"+key+"' at path:"+currentPath+
                        " contains the delimter '"+delimiter+"'.  When using WrappedProperties "+
                        "around a JProperties tree - the delimeter cannot be part of a key.");
               }
               
               Object value=currentProps.get(key);
               
               if (value instanceof JProperties) {
                  JProperties children=(JProperties)value;
                  //currentPath=currentPath+"."+key;
                  iterMap.put(currentPath, currentIterator);
                  propMap.put(currentPath, currentProps);
                  
                  if (currentPath.length() > 0) {
                     currentPath=currentPath+delimiter+key;
                  } else {
                     currentPath=key;
                  }
                  
                  if (debug)
                     System.err.println ("  Descending into "+currentPath);
                  
                  currentProps=children;
                  currentIterator=currentProps.keySet().iterator();
               } else {
                  String flatkey=null;
                  if (currentPath.length() > 0) 
                     flatkey=currentPath+delimiter+key;
                  else
                     flatkey=key;
                  
                  if (value != null || allowKeysWithNullValues) {
                     if (debug)
                        System.err.println ("     Adding key "+flatkey);
                     
                     keys.add(flatkey);
                  } else {
                     if (debug)
                        System.out.println("      Ignoring key w/ null value "+flatkey);
                  }
               }
            }
         }
         
         if (debug) {
            System.err.println("Keys: "+keys);
         }
         iterator=keys.iterator();
      }

      @Override
      public boolean hasMoreElements() {
         return iterator.hasNext();
      }

      @Override
      public Object nextElement() {
         return iterator.next();
      }
   }

}
