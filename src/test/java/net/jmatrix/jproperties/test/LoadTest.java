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
}
