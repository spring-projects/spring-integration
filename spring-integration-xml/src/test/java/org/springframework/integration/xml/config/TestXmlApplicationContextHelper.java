/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.config;

public class TestXmlApplicationContextHelper {

	private TestXmlApplicationContextHelper() {
		super();
	}

	public static TestXmlApplicationContext getTestAppContext(String xmlFragment) {
		String xml = header + xmlFragment + footer;
		TestXmlApplicationContext ctx = new TestXmlApplicationContext(xml);
		return ctx;
	}

	private final static String header = "<?xml version='1.0' encoding='UTF-8'?>"
			+ "<beans xmlns='http://www.springframework.org/schema/beans' "
			+ "xmlns:si-xml='http://www.springframework.org/schema/integration/xml' "
			+ "xmlns:si='http://www.springframework.org/schema/integration' "
			+ "xmlns:util='http://www.springframework.org/schema/util' "
			+ "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
			+ "xmlns:context='http://www.springframework.org/schema/context' "
			+ "xsi:schemaLocation="
			+ "'http://www.springframework.org/schema/beans "
			+ "http://www.springframework.org/schema/beans/spring-beans.xsd "
			+ "http://www.springframework.org/schema/integration "
			+ "http://www.springframework.org/schema/integration/spring-integration.xsd "
			+ "http://www.springframework.org/schema/integration/xml "
			+ "http://www.springframework.org/schema/integration/xml/spring-integration-xml.xsd "
			+ "http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd " +
			  "http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd' >" +
			  "<context:annotation-config/>";

	private final static String footer = "</beans>";

}
