Build samples using Maven (http://maven.apache.org)

1. Open command prompt and navigate to 'samples' directory in the root of the project distribution

	#> cd samples
	
2. Build samples 

	#> mvn install
	
You should see the following output:
. . . .
[INFO] Reactor Summary:
[INFO] ------------------------------------------------------------------------
[INFO] Spring Integration Samples ............................ SUCCESS [3.956s]
[INFO] Unnamed - cafe:cafe:jar:1.0.3 ......................... SUCCESS [6.704s]
[INFO] Unnamed - errorhandling:errorhandling:jar:1.0.3 ....... SUCCESS [0.467s]
[INFO] Unnamed - helloworld:helloworld:jar:1.0.3 ............. SUCCESS [0.442s]
[INFO] Unnamed - jms:jms:jar:1.0.3 ........................... SUCCESS [0.464s]
[INFO] Unnamed - oddeven:oddeven:jar:1.0.3 ................... SUCCESS [0.378s]
[INFO] Unnamed - quote:quote:jar:1.0.3 ....................... SUCCESS [0.381s]
[INFO] Unnamed - ws:ws:jar:1.0.3 ............................. SUCCESS [0.420s]
[INFO] Unnamed - xml:xml:jar:1.0.3 ........................... SUCCESS [0.388s]
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESSFUL
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 13 seconds
. . . .

You are ready to start working with samples

You can do so by doing one of two things
1. If your Java IDE is integrated with Maven (e.g., Eclipse/STS with Maven plug-in - highly recommended) then you can import 
individual projects into the workspace as Maven projects and start using them by following documentation in the 
javadoc of *Demo.java files.

For example:
File -> Import -> General -> Existing Project into Workspace and Browse to a specific project.

2. If you prefer to configure project on your own and simply need the right JARs in the class path, you can build samples 
using a pre-configured Maven profile which will copy all project dependencies (JARs) in the 'lib' directory of each project:

	#> mvn install -P classpath
	
Open any project and you will see the 'lib' directory with all the JARs required to run this demos
Now you can configure your IDE to point to this directory.

Happy integration :-)