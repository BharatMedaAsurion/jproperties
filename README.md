jproperties
===========

JSON, tree based Properties for java


Syntax
======

JProperties uses json syntax.  The json must be syntactically correct per the
Jackson json parser.  

The parser does allow comments per the following:

/* this is a comment */
// this is also a comment


Substitution
------------


Inclusion
---------



JProperiesCLI
=============

The command line interface is useful especially when converting large, complex
properties files to json properties.  It allows the user to split properties 
into a tree based structure using a delimiter. 

Usage
-----


	JPropertiesCLI [options] properties.file
	 
	  With no options, just a properties file, the CLI will parse the properties 
	  file as json properties, including processing incldues, and print the 
	  result to System.out with all substitutions.

	Options: 
	 
	   -s <properties>
	      Split.  Splits a (java.util.)Properties file into a json tree structure
	      based on a delimeter.  

	   -f <jproperties>
	      Flatten.  Takes a jproperties file and flattens it to a 
	      java.util.Properties format
	      using the specified delimeter, or "." if none specified.

	      Note - flattening delimiter cannot be a component of any key 
	      within the JProperties tree.
	      for instance, if you have a key foo.bar in JProperties, you 
	      cannot use '.' as the delimeter for flattening (or wrapping
	      see WrappedProperties.java)

	   -d <delimeters>
	      Defaults to "." - can substitute any other characters
	 
	   -o <file>
	      output file, if not, system.out



Spring Integration
==================

work in progress


