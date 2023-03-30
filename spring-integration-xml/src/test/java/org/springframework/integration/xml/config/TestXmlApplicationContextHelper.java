/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.xml.config;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Chris Beams
 * @author Gary Russell
 * @author Artem Bilan
 */
public class TestXmlApplicationContextHelper {

	private static final String header =
			"""
					<?xml version='1.0' encoding='UTF-8'?>
					<beans xmlns='http://www.springframework.org/schema/beans'
							xmlns:si-xml='http://www.springframework.org/schema/integration/xml'
							xmlns:si='http://www.springframework.org/schema/integration'
							xmlns:util='http://www.springframework.org/schema/util'
							xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
							xmlns:context='http://www.springframework.org/schema/context'
							xsi:schemaLocation='http://www.springframework.org/schema/beans
								https://www.springframework.org/schema/beans/spring-beans.xsd
								http://www.springframework.org/schema/integration
								https://www.springframework.org/schema/integration/spring-integration.xsd
								http://www.springframework.org/schema/integration/xml
								https://www.springframework.org/schema/integration/xml/spring-integration-xml.xsd
								http://www.springframework.org/schema/util
								https://www.springframework.org/schema/util/spring-util.xsd
								http://www.springframework.org/schema/context
								https://www.springframework.org/schema/context/spring-context.xsd'>
						<context:annotation-config/>
					""";

	private static final String footer = "</beans>";

	private TestXmlApplicationContextHelper() {
		super();
	}

	public static TestXmlApplicationContext getTestAppContext(String xmlFragment) {
		return new TestXmlApplicationContext(header + xmlFragment + footer);
	}

}
