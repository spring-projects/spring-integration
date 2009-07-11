Instructions for running CafeDemo sample
-------------------------------------------------------------------------------
1. See README.txt of the parent directory

2. To run this sample as stand alone application simply execute HelloWorldDemo class
   located in org.springframework.integration.samples.cafe package

3. This sample is also configured to be a valid OSGi bundle. This means you can deploy generated JAR
   into OSGi environment providing that Spring Integration bundles as well as other prerequisites 
   (e.g., Spring Framework were installed).
   For example: To deploy it on SpringSource dmServer simply drop cafe-1.0.3.jar file, located 
   in the 'target' directory, to the 'pickup' directory of the dmServer. 
   You should see the following output in the trace file:
   
. . . . . . . . . . . . .   			                                                     
   			                                                     System.out I -----------------------
[2009-07-11 01:43:04.614] task-scheduler-3                       System.out I Order #1
[2009-07-11 01:43:04.614] task-scheduler-3                       System.out I Iced MOCHA, 3 shots.
[2009-07-11 01:43:04.614] task-scheduler-3                       System.out I Hot LATTE, 2 shots.
[2009-07-11 01:43:04.614] task-scheduler-3                       System.out I -----------------------
[2009-07-11 01:43:04.618] task-scheduler-2                       System.out I task-scheduler-2 prepared cold drink #5 for order #5: iced 3 shot MOCHA
[2009-07-11 01:43:05.618] task-scheduler-2                       System.out I task-scheduler-2 prepared cold drink #6 for order #6: iced 3 shot MOCHA
[2009-07-11 01:43:06.618] task-scheduler-2                       System.out I task-scheduler-2 prepared cold drink #7 for order #7: iced 3 shot MOCHA
[2009-07-11 01:43:07.619] task-scheduler-2                       System.out I task-scheduler-2 prepared cold drink #8 for order #8: iced 3 shot MOCHA
[2009-07-11 01:43:08.619] task-scheduler-2                       System.out I task-scheduler-2 prepared cold drink #9 for order #9: iced 3 shot MOCHA
[2009-07-11 01:43:09.614] task-scheduler-3                       System.out I task-scheduler-3 prepared hot drink #2 for order #2: hot 2 shot LATTE
[2009-07-11 01:43:09.615] task-scheduler-3                       System.out I -----------------------
[2009-07-11 01:43:09.615] task-scheduler-3                       System.out I Order #2
[2009-07-11 01:43:09.615] task-scheduler-3                       System.out I Iced MOCHA, 3 shots.
[2009-07-11 01:43:09.615] task-scheduler-3                       System.out I Hot LATTE, 2 shots.
[2009-07-11 01:43:09.615] task-scheduler-3                       System.out I -----------------------
[2009-07-11 01:43:09.620] task-scheduler-2                       System.out I task-scheduler-2 prepared cold 
. . . . . . . . . . . . .
   			
Happy integration :-)