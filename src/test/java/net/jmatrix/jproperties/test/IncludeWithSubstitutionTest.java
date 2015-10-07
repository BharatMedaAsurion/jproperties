package net.jmatrix.jproperties.test;

import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;
import net.jmatrix.jproperties.JProperties;

import org.junit.Ignore;
import org.junit.Test;

/** 
 * 
 */
public class IncludeWithSubstitutionTest extends AbstractTest {
   
   
   @Test //@Ignore
   public void test() throws IOException {
      String path="/test.files/jproperties/include.parent.sub/root.jproperties";
      
      
      URL url=getTestFileURL(path);
      
      log("Loading "+url);
      
      JProperties jp=new JProperties();
      jp.load(url);
      
      log("Loaded: "+jp);
      
      Assert.assertTrue("Included sub properties not loaded.", 
            jp.getString("first->deeper->foo-properties->success").equals("true"));
   }
   
   @Test
   public void testMultiInclusionRecursionProblem() throws IOException {
      try {
         //JProperties.MAX_RECURSIVE_SUBSTITUTIONS=10;
         String path="/test.files/jproperties/include/many/root.jproperties";
         
         
         URL url=getTestFileURL(path);
         
        
         log("Loading "+url);
         
         JProperties jp=new JProperties();
         jp.load(url);
         
         log("Loaded: "+jp);
         
         Assert.assertTrue("Included sub properties not loaded.", 
               jp.size() > 0);
      } finally {
         //JProperties.MAX_RECURSIVE_SUBSTITUTIONS=30;
      }

   }
}
