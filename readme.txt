============================== Spring Integration =============================
To check out the project and build from source, do the following:

 git clone git://git.springsource.org/spring-integration/spring-integration.git
 cd spring-integration
 ./gradlew build

-------------------------------------------------------------------------------
To generate Eclipse metadata (.classpath and .project files), do the following:

 ./gradlew eclipse

Once complete, you may then import the projects into Eclipse as usual:

 File -> Import -> Existing projects into workspace

Browse to the 'spring-integration' root directory. All projects should import
free of errors.

-------------------------------------------------------------------------------
To generate IDEA metadata (.iml and .ipr files), do the following:

 ./gradlew idea

-------------------------------------------------------------------------------
To build the JavaDoc, do the following from within the root directory:

 ./gradlew :docs:api

The result will be available in 'docs/build/api'.

###### OSGI Notes ######

1. Dependency on Third Party Bundles
  	 	Some adapters depend on third party libraries (bundles). 
   		Spring hosts Enterprise Bundle Repository (EBR) https://ebr.springsource.com/repository/app/ 
   			where you can download many third party JARs as valid OSGi bundles.
   		If a particular bundle is not available in Spring's EBR, there are tools that can convert regular 
   		   JAR to a bundle JAR. One of them is Bundlor http://www.springsource.org/bundlor which can auto-generate 
   		   MANIFEST as part of standard project lifecycle or simply convert non-bundle JAR to a bundle JAR.
2. Boot delegation
		Some adapters depend on extension packages that are available to the boot class loader.
		For example; Feed Adapter depends on com.sun.syndication.feed. Since by default OSGi only
		loads java.* from the boot class loader, other packages that must be loaded from the boot class loader 
		can therefore be specified with the 'org.osgi.framework.bootdelegation' System property.
		For example:
			org.osgi.framework.bootdelegation=com.sun.*,org.w3c.*. . . .
   		  
===============================================================================