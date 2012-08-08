package org.springframework.integration.config.xml;
/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.junit.Test;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;


/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class ChainElementsFailureTests {

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainServiceActivator() throws Exception {
		this.bootStrap("service-activator");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainAggregator() throws Exception {
		this.bootStrap("aggregator");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainChain() throws Exception {
		this.bootStrap("chain");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainDelayer() throws Exception {
		this.bootStrap("delayer");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainFilter() throws Exception {
		this.bootStrap("filter");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainGateway() throws Exception {
		this.bootStrap("gateway");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainHeaderEnricher() throws Exception {
		this.bootStrap("header-enricher");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainHeaderFilter() throws Exception {
		this.bootStrap("header-filter");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainHeaderValueRouter() throws Exception {
		this.bootStrap("header-value-router");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainTransformer() throws Exception {
		this.bootStrap("transformer");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainRouter() throws Exception {
		this.bootStrap("router");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainSplitter() throws Exception {
		this.bootStrap("splitter");
	}

	@Test(expected=XmlBeanDefinitionStoreException.class)
	public void chainResequencer() throws Exception {
		this.bootStrap("resequencer");
	}


	private ApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("org/springframework/integration/config/xml/chain-elements-config.properties"));
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

	public static class Sampleservice {
		public String echo(String value){
			return value;
		}
	}
}
