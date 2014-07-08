package net.jmatrix.jproperties.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import net.jmatrix.jproperties.JPRuntimeException;
import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.WrappedProperties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;



public class ConversionTest extends AbstractTest {
   @Rule
   public ExpectedException exception = ExpectedException.none();
   
   @BeforeClass
   public static void beforeClass() {
      
   }
   
   @AfterClass
   public static void afterClass() {
      
   }
   
   @Test
   public void testFlattenWithDelimitedKey() {
      JProperties jp=new JProperties();
      
      jp.put("foo", "bar");
      jp.put("nested->baz", "biz");
      jp.put("nested->foo.bar", "${foo}.bar");
      jp.put("nested->baz.bar", "${baz}.bar");
      
      log(jp.toString());
      
      WrappedProperties wp=new WrappedProperties(jp, '/');
      wp.debug=true;
      
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      wp.list(new PrintStream(baos));
      log(baos.toString());
      
   }
   
   @Test
   public void testDelimiterInKeyFailure() {
      JProperties jp=new JProperties();
      
      jp.put("foo", "bar");
      jp.put("foo.bar", "foobar");
      jp.put("nested->baz", "biz");
      jp.put("nested->foo.bar", "${foo}.bar");
      jp.put("nested->baz.bar", "${baz}.bar");
      log(jp.toString());
      
      WrappedProperties wp=new WrappedProperties(jp, '.');
      wp.debug=true;
      
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      exception.expect(JPRuntimeException.class);
      wp.list(new PrintStream(baos));
      log(baos.toString());
   }
   
   
//   @Test
//   public void testSuccess() {
//      // nothing
//      log("success");
//   }
//   
//   @Test
//   public void testFail() {
//      log("Assertion failure");
//      Assert.fail();
//   }
//   
//   @Test
//   public void testException() {
//      log("Exception fail");
//      throw new RuntimeException();
//   }
}
