readme

khemka-webserver is a HTTP webserver which can be used to host your website. 
The server can be configured using properties.xml. This config file location should not be changed.




=== Plugin Name ===
Contributors: khemka@adobe.com
Tags: webserver,HTTP
Tested up to: 1.0
Stable tag: 1.0


khemka-webserver is a HTTP file webserver which can be used to host your website. 

== Description ==

This is the long description.  No limit, and you can use Markdown (as well as in the following sections).

== Installation ==


1. unzip the zip and put the folder in some suitable location
2. go the khemka-webserver/conf/properties.xml and set the properties according to your need
3. do not change the folder structure or file name
4. in properties.xml - you should set parameters as described below - 
	a. port	- if you don't set the port value the server will run on port 80 by default
	b. www_root_path - this is the folder which you want to expose to external world. if you don't set this folder , the system will expose khemka-webserver/webapps
	c. www_upload_path - this is relative to www_root_path. this is the folder where you want to upload files when upload location was not 
						specified by the request. By defult this location is khemka-webserver/webapps/root/upload
						 if you specify a path which is not present, server tries to create the folder structure.
	d. thread_pool_size - this is the number of threads which you want to create to serve the request from client. by default value is 50
	e. hostname - the ip address on which you want your server to listen. In case you dont specify any value, server will listen to any IP associated with the system
	

	
== starting server ==
go to bin folder and execute run.bat
If you want to stop the server just enter 'e'

== A brief Markdown Example ==

1. this webserver supports GET,POST request
2. this supports multipart post request
3. keep-alive is not supported
4. If-Modified-Since is supported only for GET. where if file was modified since the given time then only it is returned



== Testing ==
there are several unit test cases written. these test cases require src/test/resource which contains files used in testing the webserver. 
for testing you just need to run these test cases . (mvn test)

==platforms tested == 
1. Windows 7
	Browsers  -
	1. IE - 9.0.18
	2. Firefox - 22.0
	3. Google Chrome - 28.0.1500.72 m


testing read me
2nd change