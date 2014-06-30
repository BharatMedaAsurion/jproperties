package net.jmatrix.jproperties.cli;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import net.jmatrix.jproperties.JProperties;
import net.jmatrix.jproperties.parser.Parser;
import net.jmatrix.jproperties.util.ArgParser;


/** 
 * 
 */
public class JsonPropertiesCLI {
   static boolean debug=false;
   
   
   // -s <properties>
   //   Split a properties file based on delimeter
   //
   // -d <delimeters>
   //   Defaults to "." - can substitute any other characters
   // 
   // -o <file>
   //   output file, if not, system.out
   //
   //  JsonPropertiesCLI [options] properties.file
   //
   
   
   public static void main(String args[]) throws Exception {
      ArgParser ap=new ArgParser(args);
      
      String delim=ap.getStringArg("-d");
      String outfilename=ap.getStringArg("-o");
      debug=ap.getBooleanArg("-v");
      
      if (ap.getBooleanArg("-s")) {
         
         String infilename=ap.getLastArg();
         File infile=new File(infilename);
         
         if (!infile.exists() || !infile.canRead()) {
            throw new IOException ("Cannot read input file at "+infile.getAbsolutePath());
         }
         
         Properties jup=new Properties();
         
         JProperties jp=new JProperties();
         
         jup.load(new FileReader(infile));
         
         Enumeration keyset=jup.keys();
         while (keyset.hasMoreElements()) {
            String key=keyset.nextElement().toString();
            String val=jup.getProperty(key);
            
            // now, add to properties flat - or parse with delimeter
            
            if (delim != null) {
               String components[]=key.split("\\"+delim);
               
               if (debug) {
                  System.out.print("Key '"+key+"'  -->  "+Arrays.asList(components));
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
         }
      } else {
         // just parse ??
         System.out.println ("not -s, now what?");
      }
      
      
   }
   
   
}
