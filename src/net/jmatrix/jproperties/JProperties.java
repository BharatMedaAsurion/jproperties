package net.jmatrix.jproperties;

import java.util.*;

import net.jmatrix.jproperties.substitution.SubstitutionProcessor;
import net.jmatrix.jproperties.util.ClassLogFactory;

import org.apache.commons.logging.Log;


/**
 * Values can be: 
 *   JProperties
 *   Boolean
 *   Number
 *   String
 *   
 */
@SuppressWarnings("serial")
public class JProperties implements Map<String, Object> {
   public static Log log=ClassLogFactory.getLog();
   
   // The LinkedHashMap preserves key order.
   Map<String, Object> data=new LinkedHashMap<String, Object>();
   
   /** Maintains a tree structure. */
   private JProperties parent=null;
   
   /** Each object should know whence it came. */
   private String url=null;
   
   /** */
   public JProperties() { }
   
   /** */
   public JProperties(Map<String, Object> map) {
      log.debug("map constructor.");
      if (map == null)
         throw new NullPointerException("Constructing JProperties from Null map.");
      
      for (String key:map.keySet()) {
         Object value=map.get(key);
         if (value == null) {
            
         } else if (value instanceof String || 
                    value instanceof Number ||
                    value instanceof Boolean) {
            put(key, value);
         } else if (Map.class.isAssignableFrom(value.getClass())) {
            JProperties np=new JProperties((Map)value);
            np.parent=this;
            put(key, np);
         } else if (List.class.isAssignableFrom(value.getClass())) {
            List l=(List)value;
            
            put(key, convertlist(l));
         } else {
            log.debug("Map Unknown object type: "+value.getClass().getName()+": "+value);
         }
      }
   }
   
   private static final List convertlist(List l) {
      List c=new ArrayList();
      for (Object o:l) {
         if (o == null) {}
         else if (o instanceof String ||
             o instanceof Number ||
             o instanceof Boolean) {
            c.add(o);
         } else if (Map.class.isAssignableFrom(o.getClass())) {
            c.add(new JProperties((Map)o));
         } else if (List.class.isAssignableFrom(o.getClass())) {
            c.add(convertlist((List)o));
         } else {
            log.debug("List Unknown object type: "+o.getClass().getName()+": "+o);
         }
      }
      return c;
   }
   
   
   /**
    * This method will search up a tree of EProoperties objects, looking for 
    * a match.  It will return the first match.
    * 
    * @param key
    * @return
    */
   public Object findValue(String key) {
      Object val=get(key);
      //log.debug("findValue(): Path='"+getPath()+"', "+s+"="+val);
            
      if (val != null)
         return val;
      else {
         if (parent != null) {
            return parent.findValue(key);
         } else {
            //log.debug("findValue(): parent is null at Path='"+getPath()+"'");
         }
      }
      
      return val;
   }
   
   public String findString(String key) {
      Object o=findValue(key);
      if (o != null)
         return o.toString();
      return null;
   }
   
   public void setParent(JProperties p) {
      parent=p;
   }
   
   public void setUrl(String s) {
      url=s;
   }
   
   public String getUrl() {
      return url;
   }
   
   public String findUrl() {
      if (url != null)
         return url;
      if (parent != null)
         return parent.findUrl();
      return null;
   }
   
   /** We no longer extend Properties, but some older systems may still
    * want Properties. */
   public Properties toProperties() {
      return null;
   }
   
   // overiding get to process complex keys and substitution.
   
   @Override
   public Object get(Object okey) {
      
      if (okey == null)
         return null;
      
      String key=okey.toString();
      
      String splitKey[]=key.split("\\-\\>");
      
      //log.debug("get("+okey+")");

      if (splitKey.length == 1) {
         // simple key
         Object val=data.get(key);
         
         if (val instanceof String) {
            // check substitutions
            String sval=(String)val;
            if (SubstitutionProcessor.containsTokens(sval)) {
               return SubstitutionProcessor.processSubstitution(sval, this);
            }
            return sval;
         } else {
            return val;
         }
      } else {
         String remainingKey=key.substring(splitKey[0].length()+2);
         
         Object val=get(splitKey[0]);
         if (val != null && val instanceof JProperties) {
            return ((JProperties)val).get(remainingKey);
         } else {
            log.warn("Unresolvable key '"+okey+"', "+splitKey[0]+
                  " does not return nested properties.");
            // syntax error - should be properties object.
            return null;
         }
      }
   }
   
   ///////////////////////////// Map interface //////////////////////////
   public void clear() {
      data.clear();
   }

   public boolean containsKey(Object key) {
      return data.containsKey(key);
   }

   public boolean containsValue(Object value) {
      return data.containsValue(value);
   }

   public Set<java.util.Map.Entry<String, Object>> entrySet() {
      Set<Map.Entry<String, Object>> oset=data.entrySet();
      
      Set<Map.Entry<String, Object>> nset=new LinkedHashSet<Map.Entry<String, Object>>();
      for (Map.Entry<String, Object> entry:oset) {
         nset.add(new JPropertiesEntry(entry));
      }
      return nset;
   }

//   public Object get(Object key) {
//      return data.get(key);
//   }

   public boolean isEmpty() {
      return data.isEmpty();
   }

   public Set<String> keySet() {
      return data.keySet();
   }

   public Object put(String key, Object value) {
      return data.put(key, value);
   }

   public void putAll(Map<? extends String, ? extends Object> m) {
      data.putAll(m);
   }

   public Object remove(Object key) {
      return data.remove(key);
   }

   public int size() {
      return data.size();
   }

   public Collection<Object> values() {
      throw new RuntimeException("values not supported.");
      //return data.values();
   }
   
   /** Required to process substitutions in EntrySets. */
   public class JPropertiesEntry implements Map.Entry<String, Object> {
      String key=null;
      
      public JPropertiesEntry(Map.Entry<String, Object> entry) {
         key=entry.getKey();
      }
      @Override
      public String getKey() {
         return key;
      }

      @Override
      public Object getValue() {
         return get(key);
      }

      @Override
      public Object setValue(Object value) {
         return put(key, value);
      }
   }
}
