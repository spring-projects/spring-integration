/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.config;

import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.0.5
 *
 */
public class InnerPollerParserTests {

	@Test
	public void testRefGood() {
		// Just load the context to test the parse of a 'good' inner parser
		new ClassPathXmlApplicationContext("InnerPollerParserTests-context.xml", InnerPollerParserTests.class).close();
	}

	@Test
	public void testRefExtraAttribute() {
		try {
			// Load context from a String to avoid IDEs reporting the invalid configuration
			String badContext =
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
							"<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
							"		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
							"		xmlns:int=\"http://www.springframework.org/schema/integration\"" +
							"		xmlns:int-jdbc=\"http://www.springframework.org/schema/integration/jdbc\"" +
							"		xsi:schemaLocation=\"http://www.springframework.org/schema/integration https://www.springframework.org/schema/integration/spring-integration.xsd" +
							"			http://www.springframework.org/schema/integration/jdbc https://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc.xsd" +
							"			http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd\">" +
							"" +
							"	<int:poller id=\"outer\" fixed-rate=\"5000\"/>" +
							"" +
							"	<int:channel id=\"someChannel\"/>" +
							"" +
							"	<int-jdbc:inbound-channel-adapter channel=\"someChannel\" jdbc-operations=\"ops\"" +
							"			query=\"select 1\">" +
							"		<int:poller ref=\"outer\" fixed-rate=\"1000\"/>" + // <<<<< fixed-rate not allowed here
							"	</int-jdbc:inbound-channel-adapter>" +
							"" +
							"	<bean id=\"ops\" class=\"org.mockito.Mockito\" factory-method=\"mock\">" +
							"		<constructor-arg value=\"org.springframework.jdbc.core.JdbcOperations\"/>" +
							"	</bean>" +
							"</beans>";

			Resource resource = new ByteArrayResource(badContext.getBytes());
			new GenericXmlApplicationContext(resource).close();
			fail("Expected Failure to load ApplicationContext");
		}
		catch (BeanDefinitionParsingException bdpe) {
			assertThat(bdpe.getMessage()
					.startsWith("Configuration problem: A 'poller' element that provides a 'ref' must have no other " +
							"attributes."))
					.isTrue();
		}
	}

	@Test
	public void testRefDefaultTrue() {
		try {
			// Load context from a String to avoid IDEs reporting the invalid configuration
			String badContext =
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
							"<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
							"		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
							"		xmlns:int=\"http://www.springframework.org/schema/integration\"" +
							"		xmlns:int-jdbc=\"http://www.springframework.org/schema/integration/jdbc\"" +
							"		xsi:schemaLocation=\"http://www.springframework.org/schema/integration https://www.springframework.org/schema/integration/spring-integration.xsd" +
							"			http://www.springframework.org/schema/integration/jdbc https://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc.xsd" +
							"			http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd\">" +
							"" +
							"	<int:poller id=\"outer\" fixed-rate=\"5000\"/>" +
							"" +
							"	<int:channel id=\"someChannel\"/>" +
							"" +
							"	<int-jdbc:inbound-channel-adapter channel=\"someChannel\" jdbc-operations=\"ops\"" +
							"			query=\"select 1\">" +
							"		<int:poller ref=\"outer\" default=\"true\"/>" + // <<<<< default true not allowed here
							"	</int-jdbc:inbound-channel-adapter>" +
							"" +
							"	<bean id=\"ops\" class=\"org.mockito.Mockito\" factory-method=\"mock\">" +
							"		<constructor-arg value=\"org.springframework.jdbc.core.JdbcOperations\"/>" +
							"	</bean>" +
							"</beans>";

			Resource resource = new ByteArrayResource(badContext.getBytes());
			new GenericXmlApplicationContext(resource).close();
			fail("Expected Failure to load ApplicationContext");
		}
		catch (BeanDefinitionParsingException bdpe) {
			assertThat(bdpe.getMessage()
					.startsWith("Configuration problem: A 'poller' element that provides a 'ref' must have no other attributes."))
					.isTrue();
		}
	}

	@Test
	public void testRefExtraAttributeAndDefaultFalse() {
		try {
			// Load context from a String to avoid IDEs reporting the invalid configuration
			String badContext =
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
							"<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
							"		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
							"		xmlns:int=\"http://www.springframework.org/schema/integration\"" +
							"		xmlns:int-jdbc=\"http://www.springframework.org/schema/integration/jdbc\"" +
							"		xsi:schemaLocation=\"http://www.springframework.org/schema/integration https://www.springframework.org/schema/integration/spring-integration.xsd" +
							"			http://www.springframework.org/schema/integration/jdbc https://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc.xsd" +
							"			http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd\">" +
							"" +
							"	<int:poller id=\"outer\" fixed-rate=\"5000\"/>" +
							"" +
							"	<int:channel id=\"someChannel\"/>" +
							"" +
							"	<int-jdbc:inbound-channel-adapter channel=\"someChannel\" jdbc-operations=\"ops\"" +
							"			query=\"select 1\">" +
							"		<int:poller ref=\"outer\" default=\"false\" fixed-rate=\"1000\"/>" + // <<<<< fixed-rate not allowed here
							"	</int-jdbc:inbound-channel-adapter>" +
							"" +
							"	<bean id=\"ops\" class=\"org.mockito.Mockito\" factory-method=\"mock\">" +
							"		<constructor-arg value=\"org.springframework.jdbc.core.JdbcOperations\"/>" +
							"	</bean>" +
							"</beans>";

			Resource resource = new ByteArrayResource(badContext.getBytes());
			new GenericXmlApplicationContext(resource).close();
			fail("Expected Failure to load ApplicationContext");
		}
		catch (BeanDefinitionParsingException bdpe) {
			assertThat(bdpe.getMessage()
					.startsWith("Configuration problem: A 'poller' element that provides a 'ref' must have no other attributes."))
					.isTrue();
		}
	}

}
