package net.jmatrix.jproperties;

import java.util.*;

import org.apache.commons.logging.*;


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
   public static Log log=LogFactory.getLog(JProperties.class);
   
   Map<String, Object> data=new HashMap<String, Object>();
   
   private JProperties parent=null;
   
   public JProperties() { }
   
   
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
            log.debug("Unknown object type: "+value.getClass().getName()+": "+value);
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
         } else if (o.getClass().isAssignableFrom(Map.class)) {
            c.add(new JProperties((Map)o));
         } else if (o.getClass().isAssignableFrom(List.class)) {
            c.add(convertlist((List)o));
         } else {
            log.debug("Unknown object type: "+o.getClass().getName()+": "+o);
         }
      }
      return c;
   }
   
   
   /**
    * This method will search up a tree of EProoperties objects, looking for 
    * a match.  It will return the first match.
    * 
    * @param s
    * @return
    */
   public Object findValue(String s) {
      Object val=get(s);
      //log.debug("findValue(): Path='"+getPath()+"', "+s+"="+val);
            
      if (val != null)
         return val;
      else {
         if (parent != null) {
            return parent.findValue(s);
         } else {
            //log.debug("findValue(): parent is null at Path='"+getPath()+"'");
         }
      }
      
      return val;
   }
   
   // overiding get to process complex keys and substitution.
   
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
      return data.entrySet();
   }

   public Object get(Object key) {
      return data.get(key);
   }

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
      return data.values();
   }
}
