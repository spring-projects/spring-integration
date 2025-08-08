/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
