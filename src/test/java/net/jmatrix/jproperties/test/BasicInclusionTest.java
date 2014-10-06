package net.jmatrix.jproperties.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;
import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.parser.Parser;

import org.junit.Test;

public class BasicInclusionTest extends AbstractTest {
   
   
   @Test
   public void testFiles() {
      //log(System.getProperty("java.class.path"));
      
      //System.out.println ("Classpath: "+System.getProperty("java.class.path"));
      
      // With test.files in src/main/resources - this method works to get
      // handles to test File URLs.
      URL url=this.getClass().getResource("/test.files/jproperties/test.include.json");
      
      Assert.assertNotNull("Cannot fine test files using java resources", url);
      
      log("Test include URL: "+url);
      
      File file=new File(url.getFile());
      
      log("File for URL: "+file.getAbsolutePath());
      Assert.assertTrue("Test file at "+file+" does not exist.", file.exists());
      
   }
   
   @Test
   public void testBasicInclude() throws Exception {
      URL url=getTestFileURL("/test.files/jproperties/include/basic/root.jproperties");
      
      log("Loading basic include root properties from "+url);
      
      JProperties jp=Parser.parse(url);
      
      JPValidate.validate(jp);
      Assert.assertNotNull("The Source URL should not be null", jp.getUrl());
      
      JProperties included=jp.getProperties("included");
      Assert.assertNotNull(included);
      Assert.assertNotNull("The Source URL should not be null", included.getUrl());
      
      Assert.assertNotSame("The included URL should be different than parent", 
            jp.getUrl(), included.getUrl());
      
      
      String value=included.getString("key");
      
      Assert.assertNotNull(value);
      
      log("included.key="+value);
      
      Assert.assertEquals(value, "value");
      
      value=jp.getString("included->key");
      log("included->key="+value);
      Assert.assertEquals(value, "value");
   }
   
   @Test
   public void testSystemProperties() throws IOException {
      String path="/test.files/jproperties/system.include.jproperties";
      
      URL url=getTestFileURL(path);
      
      Assert.assertNotNull("Cannot file URL for path "+path, url);
      
      JProperties jp=Parser.parse(url);
      JPValidate.validate(jp);
      
      Assert.assertNotNull(jp.getProperties("system"));
   }
   
   @Test
   public void testSystemProperty() throws IOException {
      String path="/test.files/jproperties/system.include.single.jproperties";
      
      URL url=getTestFileURL(path);
      
      Assert.assertNotNull("Cannot file URL for path "+path, url);
      
      JProperties jp=Parser.parse(url);
      JPValidate.validate(jp);
      
      Assert.assertNotNull(jp.getString("class.path"));
   }
}
