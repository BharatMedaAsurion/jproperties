package net.jmatrix.jproperties.post;

import java.util.Arrays;

import net.jmatrix.jproperties.util.ClassLogFactory;

import org.apache.commons.logging.Log;

/* 
 * failonerror=[true,false]
 * parse=[true,false],
 * format=[properties,json]
 */
class Options {
   static final Log log=ClassLogFactory.getLog();

   static final String FAILONERROR="failonerror";
   static final String PARSE="parse";
   static final String TRUE="true";
   static final String FORMAT="format";

   static final String OPTIONS[]={FAILONERROR,PARSE,FORMAT};
   
   static enum Format{PROPERITES, JSON};
   
   public boolean failonerror=true;
   public boolean parse=true;
   public Format format=Format.JSON;
   
   
   public Options(String o) {
      if (o != null) {
         String list[]=o.split("\\,");
         
         for (String pair:list) {
            String kv[]=pair.split("\\=");
            if (kv.length == 2) {
               if (kv[0].equalsIgnoreCase(FAILONERROR)) {
                  failonerror=kv[1].equalsIgnoreCase(TRUE);
               } else if (kv[0].equalsIgnoreCase(PARSE)) {
                  parse=kv[1].equalsIgnoreCase(TRUE);
               } else if(kv[0].equalsIgnoreCase(FORMAT)) {
                  if (kv[1].equalsIgnoreCase("properties")) {
                     format=Format.PROPERITES;
                  } // else, default = json
               }else {
                  log.warn("don't understand option '"+kv[0]+"', valid options are: "+Arrays.asList(OPTIONS));
               }
            } else {
               log.warn("don't understand option pair '"+kv+"', syntax is key=value.");
            }
         }
      }
   }
}