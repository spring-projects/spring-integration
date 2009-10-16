Instructions for running the HelloWorldDemo sample
-------------------------------------------------------------------------------
1. See README.txt in the parent directory.

2. To run this sample as a standalone application simply execute the
   HelloWorldDemo class that is located within the
   org.springframework.integration.samples.helloworld package.

3. This sample is also configured to be a valid OSGi bundle. This means you can
   deploy the generated JAR into an OSGi environment providing that the Spring
   Integration bundles as well as other prerequisites (e.g., Spring Framework)
   were installed. For example: To deploy it on SpringSource dm Server simply
   drop the helloworld-[version].jar file, located  in the 'target' directory,
   to the 'pickup' directory of the dm Server instance. You should see output
   similar to the following in the trace file:
   
   			System.out I ==> HelloWorldDemo: Hello World
   			
Happy integration :-)