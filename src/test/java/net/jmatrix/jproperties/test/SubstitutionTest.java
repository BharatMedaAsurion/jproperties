package net.jmatrix.jproperties.test;

import net.jmatrix.jproperties.JProperties;

import org.junit.Assert;
import org.junit.Test;

public class SubstitutionTest extends AbstractTest {
   
   @Test
   public void testPartialSubstitution() {
      JProperties jp=new JProperties();
      
      jp.put("name", "${firstName} ${lastName}");
      jp.put("firstName", "Daffy");
      jp.put("lastName", "Duck");
      
      log(jp.toString());
      
      Assert.assertEquals(jp.getString("name"), "Daffy Duck");
   }
   
   @Test
   public void testCompleteSubstitution() {
      JProperties jp=new JProperties();
      
      jp.put("name", "${daffy.duck}");
      jp.put("daffy.duck", "Daffy Duck");
      
      log(jp.toString());
      
      Assert.assertEquals(jp.getString("name"), "Daffy Duck");
   }
   
   @Test
   public void testNestedSubstituion() {
      JProperties jp=new JProperties();
      
      jp.put("env", "dev");
      jp.put("current.env", "${env}");
      jp.put("dev->mode", "test");
      jp.put("prod->mode", "live");
      
      jp.put("mode", "${${current.env}->mode}");
      
      log(jp.toString());
      
      Assert.assertEquals(jp.getString("mode"), "test");
      
      jp.put("env", "prod");
      Assert.assertEquals(jp.getString("mode"), "live");
   }
}
