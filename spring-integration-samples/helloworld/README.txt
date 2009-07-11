Instructions for running HelloWorldDemo sample
-------------------------------------------------------------------------------
1. See README.txt of the parent directory

2. To run this sample as stand alone application simply execute HelloWorldDemo class
   located in org.springframework.integration.samples.helloworld package

3. This sample is also configured to be a valid OSGi bundle. This means you can deploy generated JAR
   into OSGi environment providing that Spring Integration bundles as well as other prerequisites 
   (e.g., Spring Framework were installed).
   For example: To deploy it on SpringSource dmServer simply drop helloworld-1.0.3.jar file, located 
   in the 'target' directory, to the 'pickup' directory of the dmServer. 
   You should see the following output in the trace file:
   
   			System.out I ==> HelloWorldDemo: Hello World
   			
Happy integration :-)