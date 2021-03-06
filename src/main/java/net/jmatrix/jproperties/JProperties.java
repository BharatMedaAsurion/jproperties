package net.jmatrix.jproperties;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import net.jmatrix.jproperties.parser.Parser;
import net.jmatrix.jproperties.post.IncludeProcessor;
import net.jmatrix.jproperties.util.ClassLogFactory;


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
//@JsonDeserialize(as=JProperties.class)
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
   
   
   private static AtomicInteger atomicIdCounter=new AtomicInteger();
   
   int id=atomicIdCounter.getAndIncrement();
   
   // Used to prevent infinite recursion.
   public static int MAX_RECURSIVE_SUBSTITUTIONS=30;
   static ThreadLocal<AtomicInteger> depthCount=new ThreadLocal<AtomicInteger>() {
      @Override
      public AtomicInteger initialValue() {
         return new AtomicInteger(0);
      }
   };
   
   /**
    * Allows user to specify a set of global masking regular expresssions
    * used when printing out properties.  
    * 
    * Properties often contain passwords or other sensitive information
    * that we'd like to mask when printing to logs or stdout. 
    * 
    * maskingRegex is an ordered map of regular expressions, which are applied
    * with String.replaceAll(key, value), in order, before 
    * returning the toString() value of properties.
    * 
    * A common mask to mask most passords: 
    * "([pP]assword[ =\":]+)[^\",]*([,\"])", "$1******$2"
    */
   static Map<String, String> maskingRegex=new TreeMap<String, String>();
   
   /** */
   public JProperties() {
      log.trace("Empty constructor:"+id);
   }
   
   /** */
   public JProperties(Properties p) {
      this((Map)p);
      log.trace("Properties constructor:"+id);
   }
   
   /** */
   public JProperties(Map<String, Object> map) {
      log.trace("map constructor:"+id);
      if (map == null)
         throw new NullPointerException("Constructing JProperties from Null map.");
      
      addAll(map);
   }
   
   /** */
   public JProperties(JProperties parent, Map<String, Object> map) {
      log.trace("Map constructor with parent:"+id);
      //this.parent=parent;
      setParent(parent);
      if (map == null)
         throw new NullPointerException("Constructing JProperties from Null map.");
      addAll(map);
   }
   

   public void addAll(Map<String, Object> map) {
      log.trace("addAll(Map)");
      for (String key:map.keySet()) {
         Object value=map.get(key);
         if (value == null) {
            
         } else if (value instanceof String || 
                    value instanceof Number ||
                    value instanceof Boolean) {
            put(key, value);
         } else if (value instanceof JProperties) {
            JProperties jp=(JProperties)value;
            jp.parent=this;
            put(key, jp);
         } else if (Map.class.isAssignableFrom(value.getClass())) {
            JProperties np=new JProperties(this, (Map)value);
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
   
   public static void addMasking(String pattern, String replace) {
      maskingRegex.put(pattern, replace);
   }
   
   public static void clearMasking() {
      maskingRegex.clear();
   }
   
   public String toString() {
      String s=Parser.writeAsJson(this);
      
      if (maskingRegex != null && maskingRegex.size() > 0) {
         for (String pattern:maskingRegex.keySet()) {
            String replace=maskingRegex.get(pattern);
            s=s.replaceAll(pattern, replace);
         }
      }
      
      return s;
   }
   
   public int getId() {return id;}
   
   public String getDebugId() {
      StringBuilder sb=new StringBuilder();
      sb.append(id+"/"+getUrl());
      if (parent != null)
         sb.append("  parent: "+parent.id+"/"+parent.getUrl());
      return sb.toString();
   }
   
   /** Used when creating a JProperties tree from a generic String/Object Map. */
   private static final List convertlist(List l, JProperties parent) {
      log.trace("Convert list");
      List c=new ArrayList();
      for (Object o:l) {
         if (o == null) {}
         else if (o instanceof String) {
            String s=(String)o;
            if (IncludeProcessor.containsInclude(s)) {
               Object oo=IncludeProcessor.include(s, parent);
               c.add(oo);
            } else {
               c.add(s);
            }
         }
         else if (o instanceof Number ||
             o instanceof Boolean) {
            c.add(o);
         } else if (Map.class.isAssignableFrom(o.getClass())) {
            JProperties p=new JProperties();
            
            p.setParent(parent);
            p.addAll((Map)o);
            c.add(p);
         } else if (List.class.isAssignableFrom(o.getClass())) {
            c.add(convertlist((List)o, parent));
         } else {
            log.debug("List Unknown object type: "+o.getClass().getName()+": "+o);
         }
      }
      return c;
   }
   
   
   public void load(String surl) throws IOException {
      Parser.parseInto(this, surl);
      resetDepth();
   }
   
   public void load(File f) throws MalformedURLException, IOException {
      load(f.toURI().toURL().toString());
   }
   
   public void load(URL url) throws IOException {
      load(url.toString());
   }
   
   public void loadFromString(String json) throws IOException {
      Parser.parseInto(this, new StringReader(json), null);
   }
   
   /**
    * This method will search up a tree of EProoperties objects, looking for 
    * a match.  It will return the first match.
    * 
    * @param key
    * @return The value found by walking up the tree.
    */
   public Object findValue(String key) {
      log.trace("findValue("+key+") setting depthCount=0");
      depthCount.get().set(0);
      return internalFindValue(key);
   }
   
   public void resetDepth() {
      log.trace("Reset depth - setting depth=0");
      depthCount.get().set(0);
   }
   
   /** Should not be called publically.  declared public for SubstitutionProcessor.*/
   Object internalFindValue(String key) {
      Integer i=depthCount.get().getAndIncrement();
      log.trace("  internalFindValue("+key+") "+i);
      if (i > MAX_RECURSIVE_SUBSTITUTIONS) {
         resetDepth();
         throw new JPRuntimeException("Error: recursive replacement limit on '"+
               key+"' id:"+id+" url:"+findUrl()+" "+i+" exceeds max "+MAX_RECURSIVE_SUBSTITUTIONS+
               ".  Parent: "+(parent == null?"null":parent.id+"/"+parent.findUrl()));
      }
      
      Object val=internalGet(key);
      //log.debug("findValue(): Path='"+getPath()+"', "+s+"="+val);
            
      if (val != null)
         return val;
      else {
         if (parent != null) {
            //log.debug("Searching parent root="+isRoot()+" with url : "+parent.url);
            return parent.internalFindValue(key);
         } else {
            //log.debug("findValue(): parent is null at Path='"+getPath()+"'");
         }
      }
      
      return val;
   }
   
   boolean isRoot() {
      return parent==null;
   }
   
   public String findString(String key) {
      System.out.println ("findString("+key+") setting depthCount=0");
      depthCount.get().set(0);
      return internalFindString(key);
   }
   
   String internalFindString(String key) {
      Object o=internalFindValue(key);
      if (o != null)
         return o.toString();
      return null;
   }
   
   public void setParent(JProperties p) {
      log.debug("Setting parent of "+id+" to "+(p == null?"null":p.id));
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
      WrappedProperties p= new WrappedProperties(this, null);
      p.setAllowKeysWithNullValues(false);
      return p;
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
   
   public void deepMerge(JProperties jp) {
      log.debug("Merging "+jp.getUrl()+" -> "+this.getUrl());
      
      Set<String> allkeys=new LinkedHashSet<String>();
      allkeys.addAll(keySet());
      allkeys.addAll(jp.keySet());
      
      for (String key:allkeys) {
         Object newval=jp.get(key);
         Object oldval=get(key);
         
         if (newval == null) {
            // do nothing.
         } else if (oldval == null && newval != null) {
            // overwrite.
            if (newval instanceof JProperties) {
               ((JProperties) newval).parent=this;
            }
            this.put(key, newval);
         } else {  // newval != null && oldval != null
            // merge.
            if (oldval instanceof JProperties && newval instanceof JProperties) {
               ((JProperties)oldval).deepMerge((JProperties)newval);
            } else {
               // overwrite
               this.put(key, newval);
            }
         }
      }
   }
   
   ///////////////////////////////////////////////////////////////////////////
   ///////////////////////////// Map interface ///////////////////////////////
   // overiding get to process complex keys and substitution.
   @Override
   public Object get(Object okey) {
      log.trace("get("+okey+") setting depthCount=0");
      depthCount.get().set(0);
      return internalGet(okey);
   }
   
   /** Should never be called by external callers. Public for access from SubstitutionProcessor */
   Object internalGet(Object okey) {
      Integer depth=depthCount.get().getAndIncrement();
      log.trace("  internalGet("+okey+") "+depth);
      if (depth > MAX_RECURSIVE_SUBSTITUTIONS) {
         resetDepth();
         throw new JPRuntimeException("Error: recursive replacement limit on '"+
               okey+"' id:"+id+" url:"+findUrl()+" "+depth+" exceeds max "+MAX_RECURSIVE_SUBSTITUTIONS+
               ".  Parent: "+(parent == null?"null":parent.id+"/"+parent.findUrl()));
      }
      
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
         
         Object val=internalGet(splitKey[0]);
         if (val == null) {
            return null;
         } else if (val instanceof JProperties) {
            return ((JProperties)val).internalGet(remainingKey);
         } else {
            log.warn("Unresolvable key '"+okey+"', at component '"+splitKey[0]+
                  "' does not return nested properties, rather "+
                  (val == null?"null":val.getClass().getName()));
            // syntax error - should be properties object.
            return null;
         }
      }
   }
   
   /**
    * Puts value into properties.
    * 
    * Keys: 
    *   Keys should be strings
    *   A complex key will contain the '-&gt;' syntax, putting the object deeper 
    *   into the tree.
    *   
    * Values:
    *    A value can be one of: 
    *      1) A primitive - String or Number
    *      2) A list - which can contain primitives or JProperties maps
    *      3) A nested JProperties object.
    */
   @JsonAnySetter
   @Override
   public Object put(String key, Object value) {
      String splitKey[]=key.split("\\-\\>");
      
      if (splitKey.length == 1) {
         if (value == null) {
            return data.remove(key);
         }
         else if (value instanceof String &&
             IncludeProcessor.containsInclude(value)) {
            Object result=IncludeProcessor.include((String)value, this);
            
            if (result == null) {
               // hmm... do nothing.
               log.debug("IncludeProcessor got null value processing '"+(String)value+"'. ignoring");
               return get(key);
            }
            else if (result instanceof String ||
                result instanceof List) {
               // overwrite
               return data.put(key, result);
            } else if (result instanceof JProperties) {
               Object currentValue=get(key);
               if (currentValue == null){
                  return data.put(key, result);
               } else if (currentValue instanceof String ||
                   currentValue instanceof List) {
                  log.debug("Overwriting key '"+key+"' with included properties.");
                  return data.put(key, result);
               } else if (currentValue instanceof JProperties) {
                  // current value is JProperites
                  // new value included is JProperties
                  // merge.
                  ((JProperties)currentValue).deepMerge((JProperties)result);
                  return put(key, currentValue);
               } else {
                  throw new RuntimeException("Current value has unknown "
                        + "data type '"+currentValue.getClass().getName()+"'");
               }
            } else {
               throw new RuntimeException("Include Processor returned value "
                     + "of unknown type '"+result.getClass().getName()+"'");
            }
         } else if (value instanceof Map && !(value instanceof JProperties)) {
            JProperties jp=new JProperties(this, (Map)value);
            JProperties existing=getProperties(key);
            if (existing != null) {
               existing.deepMerge(jp);
               return existing;
            } else
               return data.put(key, jp);
         } else if (value instanceof JProperties) {
            JProperties jp=(JProperties)value;
            JProperties existing=getProperties(key);
            if (existing != null) {
               existing.deepMerge(jp);
               return existing;
            } else
               return data.put(key, jp);
         } else if (value instanceof List) {
            return data.put(key, convertlist((List)value, this));
         } else {
            return data.put(key, value);
         }
      } else {
         // trim key, and recursively put.
         
         String remainingKey=key.substring(splitKey[0].length()+2);
         
         JProperties next=null;
         
         Object val=get(splitKey[0]);
         if (val == null) {
            next=new JProperties();
            next.setParent(this);
            put(splitKey[0], next);
         } else if (val instanceof JProperties) {
            // ok, do nothing
            next=(JProperties)val;
         } else {
            // syntax error - should be properties object.
            throw new JPRuntimeException ("Unresolvable put key '"+key+
                  "', at component '"+splitKey[0]+
                  "' does not return nested properties, rather "+
                  val.getClass().getName());
         }
         return next.put(remainingKey, value);
      }
   }
   
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
      throw new JPRuntimeException("values not supported.");
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
