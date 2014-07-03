package net.jmatrix.jproperties.cli;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.WrappedProperties;
import net.jmatrix.jproperties.parser.Parser;
import net.jmatrix.jproperties.util.ArgParser;
import net.jmatrix.jproperties.util.JDK14LogConfig;
import net.jmatrix.jproperties.util.URLUtil;


/** 
 * A command line tool for migrating between java.util.Properties and
 * json properties, and back again.
 * 
 * This is the default main.class when using jproperties with dependencies
 * jar file.  So this class can be executed as: 
 *   java -jar jproperties-[version]-with-deps.jar [options] properties.f
 * 
 * See usage for further detail.
 */
public class JPropertiesCLI {
   static boolean debug=false;
   
   static final String usage=
"JPropertiesCLI [options] properties.file\n" + 
" \n" + 
"  With no options, just a properties file, the CLI will parse the properties \n"+
"  file as json properties, including processing incldues, and print the \n"+
"  result to System.out with all substitutions.\n"+
"\n"+
"Options: \n" + 
" \n" + 
"   -s <properties>\n" + 
"      Split.  Splits a (java.util.)Properties file into a json tree structure\n" + 
"      based on a delimeter.  \n" + 
"\n" + 
"   -f <jproperties>\n" + 
"      Flatten.  Takes a jproperties file and flattens it to a \n" + 
"      java.util.Properties format\n" + 
"      using the specified delimeter, or \".\" if none specified.\n" + 
"\n" + 
"      Note - flattening delimiter cannot be a component of any key \n" + 
"      within the JProperties tree.\n" + 
"      for instance, if you have a key foo.bar in JProperties, you \n" + 
"      cannot use '.' as the delimeter for flattening (or wrapping\n" + 
"      see WrappedProperties.java)\n" + 
"\n" + 
"   -d <delimeters>\n" + 
"      Defaults to \".\" - can substitute any other characters\n" + 
" \n" + 
"   -o <file>\n" + 
"      output file, if not, system.out";


   
   // JsonPropertiesCLI [options] properties.file
   // 
   // Options: 
   // 
   //   -s <properties>
   //      Split.  Splits a (java.util.)Properties file into a json tree structure
   //      based on a delimeter.  
   //
   //   -f <jproperties>
   //      Flatten.  Takes a jproperties file and flattens it to a 
   //      java.util.Properties format
   //      using the specified delimeter, or "." if none specified.
   //
   //      Note - flattening delimiter cannot be a component of any key 
   //      within the JProperties tree.
   //      for instance, if you have a key foo.bar in JProperties, you 
   //      cannot use '.' as the delimeter for flattening (or wrapping
   //      see WrappedProperties.java)
   //
   //   -d <delimeters>
   //      Defaults to "." - can substitute any other characters
   // 
   //   -o <file>
   //      output file, if not, system.out
   //
   //
   
   
   public static void main(String args[]) throws Exception {
      if (args.length == 0) {
         System.out.println (usage);
         System.exit(1);
      }
      
      
      ArgParser ap=new ArgParser(args);
      
      String delim=ap.getStringArg("-d");
      String outfilename=ap.getStringArg("-o");
      debug=ap.getBooleanArg("-v");
      
      if (debug) {
         JDK14LogConfig.startup();
      }
      
      String inputurlstring=ap.getLastArg();
      
      URL inputUrl=null;
      if (inputurlstring.startsWith("classpath:") || inputurlstring.startsWith("http:") ||
          inputurlstring.startsWith("jar:")) {
         inputUrl=new URL(URLUtil.convertClasspathURL(inputurlstring));
      } else {
         // assume is is a file
         File infile=new File(inputurlstring);
         inputUrl=infile.toURL();
      }
      
      if (debug) 
         System.err.println("URL:  "+inputUrl);
      
      InputStream is=null;
      try {
         is=inputUrl.openStream();
         if (is == null) {
            System.err.println ("Cannot read from input URL: "+inputUrl);
            System.exit(2);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         
         System.err.println ("Cannot read from input URL: "+inputUrl);
         System.exit(3);
      }
      
//      if (!infile.exists() || !infile.canRead()) {
//         throw new IOException ("Cannot read input file at "+infile.getAbsolutePath());
//      }
      
      if (ap.getBooleanArg("-f")) {
         JProperties jp=Parser.parse(inputUrl);
         
         // now flatten.
         Character d=null;
         if (delim != null) 
            d=delim.toCharArray()[0];
         
         WrappedProperties wp=new WrappedProperties(jp, d);
         
         Enumeration keys=wp.keys();
         
         while (keys.hasMoreElements()) {
            String key=keys.nextElement().toString();
            String value=wp.getProperty(key);
            
            System.err.println(key+"="+value);
         }
         
      } else if (ap.getBooleanArg("-s")) {
         Properties jup=new Properties();
         
         JProperties jp=new JProperties();
         
         jup.load(new InputStreamReader(inputUrl.openStream()));
         
         Enumeration keyset=jup.keys();
         while (keyset.hasMoreElements()) {
            String key=keyset.nextElement().toString();
            String val=jup.getProperty(key);
            
            // now, add to properties flat - or parse with delimeter
            
            if (delim != null) {
               String components[]=key.split("\\"+delim);
               
               if (debug) {
                  System.out.println("Key '"+key+"'  -->  "+Arrays.asList(components));
               }
               
               StringBuilder path=new StringBuilder();
               JProperties jpp=jp;
               for (int i=0; i<components.length-1; i++) {
                  String keyComponent=components[i];
                  path.append(keyComponent+delim);
                  Object v=jpp.get(keyComponent);
                  if (v == null) {
                     JProperties njp=new JProperties();
                     jpp.put(keyComponent, njp);
                     jpp=njp;
                  } else if (v instanceof JProperties) {
                     jpp=(JProperties)v;
                  } else {
                     // error - which is very possible.
                     throw new Exception("Property at path -"+path+"' is a "+v);
                  }
               }
               
               jpp.put(components[components.length-1], val);
            } else {
               jp.put(key, val);
            }
         }
         
         if (outfilename != null) {
            File outfile=new File(outfilename);
            System.out.println ("Writing to: "+outfile.getAbsolutePath());
            Parser.write(jp, new File(outfilename));
         } else {
            Parser.write(jp, new OutputStreamWriter(System.out));
            System.out.println("\n");
         }
      } else {
         
         // just parse and an output, with inclusions and substitutions
         JProperties jp=Parser.parse(inputUrl);
         
         if (outfilename != null) {
            File outfile=new File(outfilename);
            System.out.println ("Writing to: "+outfile.getAbsolutePath());
            Parser.write(jp, new File(outfilename));
         } else {
            //Parser.write(jp, new OutputStreamWriter(System.out));
            System.out.println(Parser.toJson(jp));
            System.out.println();
         }
      }
   }
}
