/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class ChainElementsTests {

	private Locale localeBeforeTest;

	@Before
	public void setUp() {
		localeBeforeTest = Locale.getDefault();
		Locale.setDefault(new Locale("en", "US"));
	}

	@After
	public void tearDown() {
		Locale.setDefault(localeBeforeTest);
	}

	@Test
	public void chainOutboundGateway() throws Exception {
		try {
			bootStrap("file-outbound-gateway");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			final String expectedMessage = "Configuration problem: The " +
					"'request-channel' attribute isn't allowed for a nested (e.g. " +
					"inside a <chain/>) endpoint element: 'int-file:outbound-gateway' " +
					"with id='myFileOutboundGateway'.";
			final String actualMessage = e.getMessage();
			assertThat(actualMessage.startsWith(expectedMessage))
					.as("Error message did not start with '" + expectedMessage +
							"' but instead returned: '" + actualMessage + "'").isTrue();
		}
	}

	@Test
	public void chainOutboundGatewayWithInputChannel() throws Exception {
		try {
			bootStrap("file-outbound-gateway-input-channel");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int-file:outbound-gateway'.");
		}
	}

	private ConfigurableApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource(
				"org/springframework/integration/file/config/chain-elements-config.properties"));
		pfb.afterPropertiesSet();
		Properties prop = pfb.getObject();
		StringBuffer buffer = new StringBuffer();
		buffer.append(prop.getProperty("xmlheaders"))
				.append(prop.getProperty(configProperty))
				.append(prop.getProperty("xmlfooter"));
		ByteArrayInputStream stream = new ByteArrayInputStream(buffer.toString().getBytes());

		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

}
