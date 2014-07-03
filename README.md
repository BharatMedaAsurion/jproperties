jproperties
===========

JSON, tree based Properties for java.  Supports variable substitution and 
inclusion of other files.



Syntax
======

JProperties uses json syntax.  The json must be syntactically correct per the
Jackson json parser.  

The parser does allow comments per the following:

	/* this is a comment */
	// this is also a comment


Substitution
------------

JProperties supports variable substitution using familiar syntax: 

	{ "name":"Paul Bemowski",
	"email":"bemowski@yahoo.com",
	"email.address":"${name} <${email}>" }

Results in: 
	{
	  "name" : "Paul Bemowski",
	  "email" : "bemowski@yahoo.com",
	  "email.address" : "Paul Bemowski <bemowski@yahoo.com>"
	}


Inclusion
---------

Inclusion allows you to include one properties file in another using either
relateive or absolute URLs - supporting file, http, and classpath based urls.

The syntax for includsion is: 
	$[<include resource>]

If the included resource does not specify a protocol, it is assumed to be
relative to its parent's location.  For instance if we have 2 properties files
parent.jproperties, child.jproperties - with this relationship:

parent.jproperties:
	{"child":"$[child.properties"}


child.jproperties: 
	{"foo":"bar"}

Then - if we load the parent properties file from:
file:///tmp/jparent.properties 

Then the child will be loaded from:
file:///tmp/jchild.properties

But if we load Parent from: 
classpath:/resources/parent.jproperties

Then the child will be loaded from:
classpath:/resources/child.jproperties

* * *
Substitutions are respected within the include syntax.  So we may have 
multiple 



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


