package net.jmatrix.jproperties.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.jmatrix.jproperties.JPRuntimeException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.misc.BASE64Encoder;

/**
 * A set of URL utilities relevant to EProperites.
 * 
 */
public class URLUtil {
   public static Log log=LogFactory.getLog(URLUtil.class);
   
   public static boolean ALLOW_UNTRUSTED_SSL=true;
   
   /** 
    * Converts a classpath:/ style URL to a real file/jar URL like:
    * 
    * jar:file:/home/bemo/svnroot/eproperties/jars/testproperties.jar!/testjar.properties
    * Or: 
    * file:/bla
    * 
    * */
   public static final String convertClasspathURL(String surl) {
      String lcurl=surl.toLowerCase();
      
      if (lcurl.startsWith("classpath:/")) {
         log.debug ("Found classpath URL: "+surl);
         // First strip off the stuff that I created - 'classpath://'
         String resourcepath=surl.substring("classpath:/".length());
         log.debug ("Resource path: "+resourcepath);
         
         //URL localurl=ClasspathURLUtil.class.
         
         URL sysurl=ClassLoader.getSystemResource(resourcepath);
         log.debug ("System URL from Classloader.getSystemResource(): "+sysurl);
         
         if (sysurl == null) {
            sysurl=URLUtil.class.getClassLoader().getResource(resourcepath);
            log.debug ("System URL from URLUtil.class.getClassLoader().getResource(): "+sysurl);
         }
         
         if (sysurl == null) {
            throw new JPRuntimeException("Cannot find resource '"+resourcepath+"' in classpath for URL "+surl);
         }
         log.debug (surl+" -> "+sysurl.toExternalForm());
         surl=sysurl.toExternalForm(); // this will start with either file:// or jar:file:/
      }
      
      return surl;
   }
   
   /** */
   public static long lastMod(URL url) throws IOException {
      URLConnection con=getConnection(url);
      long lastmod=con.getLastModified();
      
      log.debug("Returning last modified: "+lastmod);
      return lastmod;
   }
   
   
   /**
    * This method wraps the URL.openConnection method to selectively 
    * disable https authentication on https URL connections.
    */
   public static URLConnection getConnection(URL url) throws IOException {
      URLConnection con=url.openConnection();
      
      log.trace("URL: \n"+debugURL(url));
      
      // Disable security features on https
      if (con instanceof HttpsURLConnection && ALLOW_UNTRUSTED_SSL) {
         HttpsURLConnection httpsCon=(HttpsURLConnection)con;
         
         log.warn("Creating un-trusted SSL connection to: "+url.toExternalForm());
         
         //log.warn("URL Info: \n"+debugURL(url));
         
         try {
            SSLContext sc=SSLContext.getInstance("SSL");
            TrustManager tmchain[]=new TrustManager[1];
            tmchain[0]=new TrustAll();
            sc.init(null, tmchain, new SecureRandom());

            httpsCon.setSSLSocketFactory(sc.getSocketFactory());
            httpsCon.setHostnameVerifier(new HostVerifyAll());
         } catch (GeneralSecurityException ex) {
            throw new IOException ("Error trying to establish SSL Connection", ex);
         } 
         
         con=httpsCon;
      }
      
      // Support for basic authentication.
      String userInfo=url.getUserInfo();
      if (userInfo != null) {
         log.info("Adding Auth Info: "+userInfo);
         // Assume basic auth.  Baae 64 encode, and add as request parm.
         String encodedAuth=(new BASE64Encoder()).encode(userInfo.getBytes());
         con.setRequestProperty("Authorization", "Basic "+encodedAuth);
      }
      
      return con;
   }
   
   /** */
   public static final String debugURL(URL url) {
      StringBuilder sb=new StringBuilder();
      
      sb.append("  URL: "+url.toExternalForm()+"\n");
      sb.append("      Authority: "+url.getAuthority()+"\n");
      sb.append("    DefaultPort: "+url.getDefaultPort()+"\n");
      sb.append("           File: "+url.getFile()+"\n");
      sb.append("           Host: "+url.getHost()+"\n");
      sb.append("           Path: "+url.getPath()+"\n");
      sb.append("           Port: "+url.getPort()+"\n");
      sb.append("       Protocol: "+url.getProtocol()+"\n");
      sb.append("          Query: "+url.getQuery()+"\n");
      sb.append("            Ref: "+url.getRef()+"\n");
      sb.append("       UserInfo: "+url.getUserInfo()+"\n");
      
      
      return sb.toString();
   }
   
   
   /** */
   private static class TrustAll implements X509TrustManager {
      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
         return null;
      }
   }
   
   /** */
   private static class HostVerifyAll implements HostnameVerifier {
      @Override
      public boolean verify(String hostname, SSLSession session) {
         // TODO Auto-generated method stub
         return  true;
      }
   }
}
