package net.jmatrix.jproperties.test;

import java.net.URL;

import junit.framework.Assert;
import net.jmatrix.jproperties.JProperties;

import org.junit.Test;

public class LoadTest extends AbstractTest {
   
   
   @Test
   public void test() throws Exception {
      String path="/test.files/jproperties/test.json";
      URL url=getTestFileURL(path);
      Assert.assertNotNull("Cannot find test file at path "+path, url);
      
      JProperties jp=new JProperties();
      
      jp.load(url);
      
      
      String bar=jp.getString("foo");
      
      Assert.assertNotNull(bar);
      Assert.assertTrue("", bar.equals("bar"));
   }
   
   @Test
   public void testLoadFromString() throws Exception {
      String json=
            "{\"key\":\"aval\",\"sub\":\"val of key: ${key}\"}";
      
      JProperties jp=new JProperties();
      jp.loadFromString(json);
      
      Assert.assertTrue("Should be size=2", jp.size()==2);
      Assert.assertTrue(jp.getString("sub").contains("aval"));
   }
}
