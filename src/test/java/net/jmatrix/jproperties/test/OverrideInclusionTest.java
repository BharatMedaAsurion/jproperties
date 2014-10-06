package net.jmatrix.jproperties.test;

import java.io.IOException;
import java.net.URL;

import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.parser.Parser;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class OverrideInclusionTest extends AbstractTest {
   @BeforeClass
   public static void beforeClass() {
      
   }
   
   @AfterClass
   public static void afterClass() {
      
   }
   
   @Test
   public void testOverride() throws IOException {
      log("Override inclusion test.");
      
      URL url=getTestFileURL("/test.files/jproperties/override/root.jproperties");
      
      log("Loading basic include root properties from "+url);
      
      JProperties jp=Parser.parse(url);
      
      JPValidate.validate(jp);
      Assert.assertNotNull("The Source URL should not be null", jp.getUrl());
      
      log(jp.toString());
      
      Assert.assertEquals("Overridden root url is wrong", jp.getString("env->root.url"), "http://localhost");
      Assert.assertNotNull("Overide property not mixed in", jp.getString("env->not.in.initial"));
   }
}
