package net.jmatrix.jproperties.test;

import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;
import net.jmatrix.jproperties.JProperties;

import org.junit.Test;

/** 
 * 
 */
public class IncludeWithSubstitutionTest extends AbstractTest {
   
   
   @Test
   public void test() throws IOException {
      String path="/test.files/jproperties/include.parent.sub/root.jproperties";
      
      
      URL url=getTestFileURL(path);
      
      log("Losding "+url);
      
      JProperties jp=new JProperties();
      jp.load(url);
      
      log("Loaded: "+jp);
      
      Assert.assertTrue("Included sub properties no loaded.", 
            jp.getString("first->deeper->foo-properties->success").equals("true"));
   }
}
