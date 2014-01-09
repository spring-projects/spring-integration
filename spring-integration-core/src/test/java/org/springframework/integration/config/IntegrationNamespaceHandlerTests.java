/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.config;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.config.xml.IntegrationNamespaceHandler;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class IntegrationNamespaceHandlerTests {

	@Test
	public void testRegisterBuiltInBeansOnlyOnce() {
		NamespaceHandler namespaceHandler = new IntegrationNamespaceHandler();

		Log log = Mockito.spy(TestUtils.getPropertyValue(namespaceHandler, "logger", Log.class));
		DirectFieldAccessor dfa = new DirectFieldAccessor(namespaceHandler);
		dfa.setPropertyValue("logger", log);

		GenericApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();

		XmlReaderContext readerContext = Mockito.mock(XmlReaderContext.class);
		BeanDefinitionReader beanDefinitionReader = Mockito.mock(XmlBeanDefinitionReader.class);
		DirectFieldAccessor bDefReaderDfa = new DirectFieldAccessor(beanDefinitionReader);
		bDefReaderDfa.setPropertyValue("registry", testApplicationContext);

		DirectFieldAccessor readerContextDfa = new DirectFieldAccessor(readerContext);
		readerContextDfa.setPropertyValue("reader", beanDefinitionReader);

		Element element = Mockito.mock(Element.class);
		Mockito.when(element.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation"))
				.thenReturn("http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration.xsd");
		Document document = Mockito.mock(Document.class);
		Mockito.when(element.getOwnerDocument()).thenReturn(document);
		Mockito.when(document.getDocumentElement()).thenReturn(element);


		for (int i = 0; i < 3; i++) {
			try {
				namespaceHandler.parse(element, new ParserContext(readerContext, null));
			}
			catch (NullPointerException e) {
				//ignore it as it is out of scope of this test.
			}
		}

		Mockito.verify(log).debug("SpEL function '#xpath' isn't registered: there is no spring-integration-xml.jar on the classpath.");
	}

}
