Instructions for building the samples using Maven (http://maven.apache.org)
NOTE: this requires Maven 2.1.0 or later
-------------------------------------------------------------------------------

1. Open a command prompt and navigate to the 'samples' directory within the
root of the project distribution:

	#> cd spring-integration-samples

2. Build the samples and install them into your local Maven repository:

	#> mvn install

You should see output similar to the following:
. . . .
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] ------------------------------------------------------------------------
[INFO] Spring Integration Samples ............................ SUCCESS [2.765s]
[INFO] Spring Integration Cafe Sample ........................ SUCCESS [19.351s]
[INFO] Spring Integration Error Handling Sample .............. SUCCESS [1.149s]
[INFO] Spring Integration File Copy Sample ................... SUCCESS [0.829s]
[INFO] Spring Integration Hello World Sample ................. SUCCESS [0.838s]
[INFO] Spring Integration JMS Sample ......................... SUCCESS [0.853s]
[INFO] Spring Integration Odd-Even Sample .................... SUCCESS [1.026s]
[INFO] Spring Integration Quote Sample ....................... SUCCESS [0.753s]
[INFO] Spring Integration WS Inbound Gateway Sample .......... SUCCESS [5.017s]
[INFO] Spring Integration WS Outbound Gateway Sample ......... SUCCESS [1.135s]
[INFO] Spring Integration XML Sample ......................... SUCCESS [0.823s]
[INFO] ------------------------------------------------------------------------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESSFUL
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 35 seconds
. . . .

Once you see BUILD SUCCESSFUL, you are ready to start working with the samples.

You can do so in one of two ways:

1. If your Java IDE is integrated with Maven (e.g., SpringSource Tool Suite or
Eclipse with a Maven plug-in is highly recommended), then you can import the
projects into a workspace as Maven projects and start using them by following
the documentation in the javadoc of the various *Demo.java files.

For example:
    File -> Import -> General -> Existing Projects into Workspace
    Browse to the 'samples' directory or a specific project's sub-directory

2. If you prefer to configure the projects on your own and simply need the
right JARs on the class path, you can build the samples using a pre-configured
Maven profile which will copy all project dependencies (JARs) in the 'lib'
directory of each project:

	#> mvn install -P classpath

Open any project and you will see the 'lib' directory with all of the JARs
required to run the demos. Now you can configure your IDE to point to those
directories.

Happy integration :-)
