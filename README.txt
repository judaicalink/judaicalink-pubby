Original Pubby documentation: http://wifo5-03.informatik.uni-mannheim.de/pubby/

This is the DM2E (http://dm2e.eu) fork of Pubby. We will implement several modifications and adaptions to suit the DM2E data model.

Contact: Kai Eckert (kai@informatik.uni-mannheim.de)


Requirements for using wisslab/pubby:

It is necessary to have an up to date version of Java Developement Kit
	-  https://www.oracle.com/java/technologies/javase-downloads.html


Download and install a servlet container such as jetty or tomcat
In the servlet container under webapps you will find a distribution of pubby. You'll primarily work there. Copy the changes from 
judaicalink/pubby into the servlet webapp/pubby directory to replace the standart distribution files.


A software tool for automating software build such as apache ant is necessary for the creation of a .war file
	-  https://ant.apache.org/
	- include ant or similar software tool in your path variable
If using ant, open shell and use the command 'ant' to start the application. Change into your pubby directory and use the command 'ant war'.
This creates a .war file vital for using pubby
Copy the crated file pubby.war into the webapps folder of your servlet container.

To use judaicalink-pubby locally, you have to change config.ttl. Alternatively a config.ttl file with the changes described in the following
paragraph can also be found under judaicalink-pubby\WEB-INF under the name config-local.ttl. 
To use this config.ttl file instead of changing the standard config.ttl, simply rename it to config.ttl and replace the old file.

Change config.ttl in the WEB-INF folder of the ubby distribution in your servlet container to function locally:
	
	change line conf:web to:				
		conf:webBase <http://localhost:8080/pubby/>
	change line conf_defaultEndpoint to: 			
		conf:defaultEndpoint <http://data.judaicalink.org/sparql/query>
	
To make sure the changes are registered in tomcat, open and edit web.xml and save it after deleting the changes. 


To start judaicalink-pubby, change to tomcat/bin or jetty/bin directory in the shell and use the command startup.

If you are using the changed config.ttl, you  should be able to acces pubby under localhost:8080/pubby