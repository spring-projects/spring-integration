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

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.junit.Test;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;


/**
 * @author Gunnar Hillert
 * @since 2.2
 */
public class ChainElementsTests {

	@Test
	public void chainOutboundGateway() throws Exception {

		try {
			this.bootStrap("file-oubound-gateway");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			final String expectedMessage = "Configuration problem: The " +
					"'request-channel' attribute isn't allowed for a nested (e.g. " +
					"inside a <chain/>) endpoint element: 'int-file:outbound-gateway' " +
					"with id='myFileOutboundGateway'.";
			final String actualMessage = e.getMessage();
			assertTrue("Error message did not start with '" + expectedMessage +
					"' but instead returned: '" + actualMessage + "'", actualMessage.startsWith(expectedMessage));
		}

	}

	@Test
	public void chainOutboundGatewayWithInputChannel() throws Exception {

		try {
			this.bootStrap("file-oubound-gateway-input-channel");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int-file:outbound-gateway'.", e.getCause().getMessage());
		}

	}

	private ApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("org/springframework/integration/file/config/chain-elements-config.properties"));
		pfb.afterPropertiesSet();
		Properties prop = pfb.getObject();
		StringBuffer buffer = new StringBuffer();
		buffer.append(prop.getProperty("xmlheaders")).append(prop.getProperty(configProperty)).append(prop.getProperty("xmlfooter"));
		ByteArrayInputStream stream = new ByteArrayInputStream(buffer.toString().getBytes());

		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

}
