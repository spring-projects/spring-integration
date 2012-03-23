Spring Integration JPA Adapter
==============================

#Overview

The Spring Integration JPA Adapter contains the following components:

* The Inbound Channel Adapter
* The Outbound Channel Adapter
* The Outbound Gateway for JPA in the project

The provided JPA components are using the Java Persistence API (JPA). They allow you to execute not only queries using the Java Persistence Query Language (JPQL) but also native SQL queries or named JPQL/SQL queries. Furthermore, entities can be retrieved/persisted by solely specifying the targeted entity class.

# Configuration

## JpaOperations

The actual execution of Jpa queries is done through the *JpaOperations* interface. The user has the following 2 choices:

* Use the *DefaultJpaOperations* implementation, which is instantiated by default (recommended)
* Provide their own implementation of the *JpaOperations* interface.

## JpaExecutor

All adapters will delegate to the JpaExecutor. The JpaExecutor will then execute the JpaOperations. This strategy enables maximum reuse of of code through the various JPA components. 

## Passing Parameters

### ParameterSourceFactory

	<jpa:outbound-channel-adapter entity-manager="dataSource" channel="input"
	    query="update Customer set name = :newName where name = :oldName"
	    jpa-parameter-source-factory="spelSource"/>
        
	<bean id="spelSource" 
	      class="o.s.integration.jpa.ExpressionEvaluatingJpaParameterSourceFactory">
	    <property name="parameterExpressions">
	        <map>
	            <entry key="id"          value="headers['id'].toString()"/>
	            <entry key="createdDate" value="new java.util.Date()"/>
	            <entry key="payload"     value="payload"/>
	        </map>
	    </property>
	</bean>

### Parameter Sub Elements

	<jpa:outbound-channel-adapter entity-manager="dataSource" channel="input"
		                          query="update Customer set name = :newName  where name = :oldName">
		   <int-jpa:parameter name="oldName" type="java.lang.String" value="some Old Name"/>
		   <int-jpa:parameter name="newName" expression="payload.newName"/>		
	</jpa:outbound-channel-adapter>

	<jpa:outbound-gateway entity-manager="dataSource" channel="input"
		                  query="update Customer set name = :newName  where name = :oldName">
		   <int-jpa:parameter name="oldName" type="java.lang.String" value="some Old Name"/>
		   <int-jpa:parameter name="newName" expression="payload.newName"/>		
	</jpa:outbound-channel-adapter>

#### Names Parameters

	<jpa:polling-channel-adapter entity-manager="dataSource"
		                         query="from Customer s where s.name = :name and s.industry = :industry">
		   <int-jpa:parameter name="industry" type="java.lang.String" value="Manufacturing"/>
		   <int-jpa:parameter name="name"                             expression="payload.name"/>		
	</jpa:outbound-channel-adapter>

#### Positional Parameters

	<jpa:polling-channel-adapter entity-manager="dataSource"
		                         query="from Customer s where s.name = ? and s.industry = ?">
		   <int-jpa:parameter type="java.lang.String" value="Manufacturing"/>
		   <int-jpa:parameter                         expression="payload.name"/>		
	</jpa:outbound-channel-adapter>

## Common Configuration Attributes

## Common Configuration Sub-Elements

#### Passing in the EntityManager

entityManagerFactoryRef
sharedentitymanagerbean


### Native Queries

The Spring Integration JPA Adapter also support native queries. 

When doing native queries, you can also pass in the **entityClass** property. It will be used to return the results as List<entityClass>. If the *entityClass* property is not specified, the returned list will return Object[] arrays, containing the values for each column. 
	
**WARNING** At this point specifying **resultMappings** is not supported, yet. Furthermore, you cannot pass in collections of parameters for native queries.  

####Named Native Queries

When using Hibernate as your JPA provider and you want to execute named native queries using the **@NamedNativeQuery** annotation, you must declare the ‘resultClass‘ to let Hibernate know what is the return type, failed to do it will caused the exception:

    org.hibernate.cfg.NotYetImplementedException: Pure native scalar queries are not yet supported


## Operation using the EntityClass

### Persist/Merge/Delete

### Batch Operations

**Not yet supported.** 

JPA has not direct support for batch inserts. However, a user can pass in a payload that contains a collection of entity. 


### Inbound Channel Adapter

### Outbound Channel Adapter

In the simplest use-case, a user provides a message containing a single entity that needs to be persisted in the database. Using a JPA Outbound Channel Adapter, the relevant configuration would be:

	<int-jpa:outbound-channel-adapter id="jpaOutboundChannelAdapter" 
                                      entity-manager="entity-manager"
                                      entity-class="org.springframework.integration.jpa.test.entity.Student"                        
                                      channel="target">                         
    </int-jpa:outbound-channel-adapter>

### Outbound Gateway

	<int-jpa:outbound-gateway id="jpaOutboundChannelAdapter" 
	                                  entity-manager="entity-manager"
	                                  entity-class="org.springframework.integration.jpa.test.entity.Student"                        
	                                  request-channel="in"
	                                  reply-channel="out">                         
	</int-jpa:outbound-gateway>

The persisted entity will be send to the reply-channel with the ID property being populated.

### Complete Configuration Options 

	<?xml version="1.0" encoding="UTF-8"?>
	<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xmlns:jdbc="http://www.springframework.org/schema/jdbc"
		xmlns:int="http://www.springframework.org/schema/integration"
		xmlns:int-jpa="http://www.springframework.org/schema/integration/jpa"
		xsi:schemaLocation="http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc.xsd
			http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration.xsd
			http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
			http://www.springframework.org/schema/integration/jpa http://www.springframework.org/schema/integration/jpa/spring-integration-jpa-2.2">

	<int:channel id="target"/>

	    <jdbc:embedded-database id="dataSource" type="H2"/>
    
	    <bean id="entityManager"
	        class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean">
	        <property name="dataSource" ref="dataSource" />
	    </bean>

	    <bean id="em"
	        class="org.springframework.orm.jpa.support.SharedEntityManagerBean">
	        <property name="entityManagerFactory" ref="lc" />
	    </bean>

	    <int-jpa:outbound-channel-adapter id="jpaOutboundChannelAdapter" 
	                                      entity-manager="entity-manager"
                                      
	                                      auto-startup="true"
	                                      entity-class="org.springframework.integration.jpa.test.entity.Student"
	                                      query="from Student"
	                                      native-query="false"
	                                      named-query="myNamedQery"

	                                      persist-mode="MERGE"
	                                      parameter-source-factory=""
	                                      use-payload-as-parameter-source="false"
	                                      order="1"
                                                                         
	                                      channel="target">
                                      
	        <int-jpa:parameter  name="firstName"   value="kenny"  type="java.lang.String"/>
	        <int-jpa:parameter  name="firstaName"  value="cartman"/>     
	        <int-jpa:parameter  name="updatedDateTime" expression="new java.util.Date()"/>           
                
	    </int-jpa:outbound-channel-adapter>


	    <int-jpa:inbound-channel-adapter id="inbound-channel-adapter"
	                                     entity-manager="entity-manager"
	                                     entity-manager-factory="entity-manager-factory"
	                                     jpa-operations="custom"
	                                     auto-startup="true"
	                                     entity-class="org.springframework.integration.jpa.test.entity.Student"
	                                     query="from Student"
	                                     native-query="true"
	                                     named-query="myNamedQery"                                     
                                     
	                                     delete-after-poll="true"
	                                     delete-per-row="true"                                     
                                     
	                                     channel=""
	                                     send-timeout ="">
	         <int:poller/>
         
	     </int-jpa:inbound-channel-adapter>   
             
	 	<int-jpa:outbound-channel-adapter id="jpaOutboundChannelAdapter" 
	                                      entity-manager="entity-manager"
	                                      entity-manager-factory="entity-manager-factory"
	                                      jpa-operations="custom"
	                                      auto-startup="true"
	                                      entity-class="org.springframework.integration.jpa.test.entity.Student"
	                                      query="from Student"
	                                      native-query="true"
	                                      named-query="myNamedQery"

	                                      persist-mode="MERGE"
	                                      parameter-source-factory=""
	                                      use-payload-as-parameter-source="false"
	                                      order="1"
                                                                         
		                                  channel="target">
	                                  
		    <int-jpa:parameter  name="firstName"   value="kenny"  type="java.lang.String"/>
	        <int-jpa:parameter  name="firstaName"  value="cartman"/>     
	        <int-jpa:parameter  name="updatedDateTime" expression="new java.util.Date()"/>           
                
	    </int-jpa:outbound-channel-adapter> 
                           	
	     <int-jpa:outbound-gateway id="outbound-gateway"
	                              entity-manager="entity-manager"
	                              entity-manager-factory="entity-manager-factory"
	                              jpa-operations="custom"
	                              auto-startup="true"
	                              entity-class="org.springframework.integration.jpa.test.entity.Student"
	                              query=""
	                              native-query="true"
	                              named-query="myNamedQery"
                              
	                              persist-mode="MERGE"
	                              parameter-source-factory=""
	                              use-payload-as-parameter-source="false"
	                              order="1"
                              
	                              delete-after-poll="true"
	                              delete-per-row="true"
                              
	                              gateway-type="UPDATING"
	                              max-rows="-1"
	                              request-channel="outbound-gateway-channel"
	                              reply-channel=""
	                              reply-timeout="">
	                <int:poller></int:poller>
	                <int-jpa:parameter expression="payload"/>             
	    </int-jpa:outbound-gateway>                                
                                   
	</beans>
