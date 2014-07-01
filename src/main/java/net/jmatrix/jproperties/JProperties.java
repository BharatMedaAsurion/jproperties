package net.jmatrix.jproperties;

import java.util.*;

import net.jmatrix.jproperties.substitution.SubstitutionProcessor;
import net.jmatrix.jproperties.util.ClassLogFactory;

import org.apache.commons.logging.Log;


/**
 * JProperties is a replacment for the classic java.util.Properties
 * object that supports JSON based syntax, inclusion and substitution.
 * 
 * This allows any value in a property set to be any of:
 *   JProperties
 *   List
 *   Boolean
 *   Number
 *   String
 * 
 * Further, complex properties sets can be broken into multiple files 
 * using "includes" syntax to load additional properites from external
 * locations based on URLs or method inclusion (very useful for system properties).
 * 
 *  
 */
@SuppressWarnings("serial")
public class JProperties implements Map<String, Object> {
   public static long DEFAULT_LONG=-1;
   public static int DEFAULT_INT=-1;
   public static boolean DEFAULT_BOOLEAN=false;
   
   public static Log log=ClassLogFactory.getLog();
   
   // The LinkedHashMap preserves key order.
   Map<String, Object> data=new LinkedHashMap<String, Object>();
   
   /** Maintains a tree structure. */
   private JProperties parent=null;
   
   /** Each object should know whence it came. */
   private String url=null;
   
   private boolean processSubstitutions=true;
   private boolean processInclusions=true;
   
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
            
            put(key, convertlist(l, this));
         } else {
            log.debug("Map Unknown object type: "+value.getClass().getName()+": "+value);
         }
      }
   }
   
   private static final List convertlist(List l, JProperties parent) {
      List c=new ArrayList();
      for (Object o:l) {
         if (o == null) {}
         else if (o instanceof String ||
             o instanceof Number ||
             o instanceof Boolean) {
            c.add(o);
         } else if (Map.class.isAssignableFrom(o.getClass())) {
            JProperties p=new JProperties((Map)o);
            p.setParent(parent);
            c.add(p);
         } else if (List.class.isAssignableFrom(o.getClass())) {
            c.add(convertlist((List)o, parent));
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
      processInclusions=p.processInclusions;
      processSubstitutions=p.processSubstitutions;
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
   
   public List<String> getKeys() {
      List<String> l=new ArrayList<String>();
      l.addAll(keySet());
      return l;
   }
   

   public boolean isProcessSubstitutions() {
      return processSubstitutions;
   }

   public void setProcessSubstitutions(boolean processSubstitutions) {
      this.processSubstitutions = processSubstitutions;
   }

   public boolean isProcessInclusions() {
      return processInclusions;
   }

   public void setProcessInclusions(boolean processInclusions) {
      this.processInclusions = processInclusions;
   }
   
   ///////////////////////////////////////////////////////////////////////////
   ///////////////////////////// Map interface ///////////////////////////////
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
         
         if (!processSubstitutions) 
            return val;
         
         if (val instanceof String) {
            // check substitutions
            String sval=(String)val;
            if (SubstitutionProcessor.containsTokens(sval)) {
               return SubstitutionProcessor.processSubstitution(sval, this, Object.class);
            }
            
            if (SubstitutionProcessor.containsTokens(sval)) {
               log.warn("Value for key "+key+" contains unresolvable substitution.");
            }
            return sval;
         } else if (val instanceof List) {
            List l=(List)val;
            int size=l.size();
            for (int i=0; i<size; i++) {
               Object lo=l.get(i);
               if (lo instanceof String) {
                  String ls=(String)lo;
                  if (SubstitutionProcessor.containsTokens(ls)) {
                     Object newobj=SubstitutionProcessor.processSubstitution(ls, this, Object.class);
                     l.set(i, newobj); // replace the string.
                  }
               } else if (lo instanceof JProperties) {
                  // do i do anything here?
               } else if (lo instanceof List) {
                  // FIXME: this is an issue.  Needs to be called recursively
                  // list of lists not currently supported.
               } else {
                  // what else could there be?
               }
            }
            return l;
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

// public Object get(Object key) {
//    return data.get(key);
// }

   @Override
   public void clear() {
      data.clear();
   }

   @Override
   public boolean containsKey(Object key) {
      return data.containsKey(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return data.containsValue(value);
   }

   @Override
   public Set<java.util.Map.Entry<String, Object>> entrySet() {
      Set<Map.Entry<String, Object>> oset=data.entrySet();
      
      Set<Map.Entry<String, Object>> nset=new LinkedHashSet<Map.Entry<String, Object>>();
      for (Map.Entry<String, Object> entry:oset) {
         nset.add(new JPropertiesEntry(entry));
      }
      return nset;
   }
   
   @Override
   public boolean isEmpty() {
      return data.isEmpty();
   }
   
   @Override
   public Set<String> keySet() {
      return data.keySet();
   }
   
   @Override
   public Object put(String key, Object value) {
      return data.put(key, value);
   }
   
   @Override
   public void putAll(Map<? extends String, ? extends Object> m) {
      data.putAll(m);
   }
   
   @Override
   public Object remove(Object key) {
      return data.remove(key);
   }

   @Override
   public int size() {
      return data.size();
   }

   @Override
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
   
   //////////////////////////  convience methods  //////////////////////////
   public String getString(String key) {
      return getString(key, null);
   }
   
   public String getString(String key, String def) {
      Object o=get(key);
      if (o == null)
         return def;
      return o.toString();
   }
   
   public boolean getBoolean(String key) {
      return getBoolean(key, DEFAULT_BOOLEAN);
   }
   
   public boolean getBoolean(String key, boolean def) {
      Object o=get(key);
      if (o == null)
         return def;
      if (o instanceof Boolean) {
         return ((Boolean)o).booleanValue();
      }
      if (o instanceof String) {
         String s=(String)o;
         if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") ||
             s.equalsIgnoreCase("yes")) {
            return true;
         }
         return false;
      }
      return false;
   }
   
   public int getInt(String key) {
      return getInt(key, DEFAULT_INT);
   }
   
   public int getInt(String key, int def) {
      return (int)getLong(key, def);
   }
   
   public long getLong(String key) {
      return getLong(key, DEFAULT_LONG);
   }
   
   public long getLong(String key, long def) {
      Object o=get(key);
      if (o == null)
         return def;
      if (o instanceof Number) {
         return ((Number)o).longValue();
      }
      if (o instanceof String) {
         try {
            return Long.parseLong(((String)o).trim());
         } catch (NumberFormatException ex) {
            log.debug("Cannot parse number from '"+o+"'");
         }
      }
      return def;
   }
   
   public JProperties getProperties(String key) {
      Object o=get(key);
      if (o instanceof JProperties) {
         return (JProperties)o;
      }
      return null;
   }
   
   public List getList(String key) {
      Object o=get(key);
      if (o instanceof List) {
         return (List)o;
      }
      return null;
   }

}
