/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.transformer;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;


/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Mike Bazos
 */
public class XsltPayloadTransformerTests {

	private XsltPayloadTransformer transformer;

	private final String docAsString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	private final String outputAsString = "<bob>test</bob>";

	@Before
	public void setUp() throws Exception {
		transformer = new XsltPayloadTransformer(getXslResource());
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
	}

	@Test
	public void testDocumentAsPayload() throws Exception {
		Object transformed = transformer.doTransform(buildMessage(XmlTestUtil
				.getDocumentForString(docAsString)));
		assertTrue("Wrong return type for document payload", Document.class
				.isAssignableFrom(transformed.getClass()));
		Document transformedDocument = (Document) transformed;
		assertXMLEqual(outputAsString, XmlTestUtil
				.docToString(transformedDocument));
	}

	@Test
	public void testSourceAsPayload() throws Exception {
		Object transformed = transformer
				.doTransform(buildMessage(new StringSource(docAsString)));
		assertEquals("Wrong return type for source payload", DOMResult.class,
				transformed.getClass());
		DOMResult result = (DOMResult) transformed;
		assertXMLEqual("Document incorrect after transformation", XmlTestUtil
				.getDocumentForString(outputAsString), (Document) result
				.getNode());
	}

	@Test
	public void testStringAsPayload() throws Exception {
		Object transformed = transformer.doTransform(buildMessage(docAsString));
		assertEquals("Wrong return type for string payload", String.class,
				transformed.getClass());
		String transformedString = (String) transformed;
		assertXMLEqual("String incorrect after transform", outputAsString,
				transformedString);
	}

	@Test
	public void testStringAsPayloadUseResultFactoryTrue() throws Exception {
		transformer.setAlwaysUseResultFactory(true);
		Object transformed = transformer.doTransform(buildMessage(docAsString));
		assertEquals("Wrong return type for useFactories true",
				DOMResult.class, transformed.getClass());
		DOMResult result = (DOMResult) transformed;
		assertXMLEqual("Document incorrect after transformation", XmlTestUtil
				.getDocumentForString(outputAsString), (Document) result
				.getNode());
	}

	@Test
	public void testSourceWithResultTransformer() throws Exception {
		Integer returnValue = new Integer(13);
		transformer = new XsltPayloadTransformer(getXslResource(),
				new StubResultTransformer(returnValue));
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object transformed = transformer
				.doTransform(buildMessage(new StringSource(docAsString)));
		assertEquals("Wrong value from result conversion", returnValue,
				transformed);
	}

	@Test
	public void testXsltPayloadWithTransformerFactoryClassname() throws Exception {
		Integer returnValue = new Integer(13);
		transformer = new XsltPayloadTransformer(getXslResource(), new StubResultTransformer(returnValue),
				"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object transformed = transformer
				.doTransform(buildMessage(new StringSource(docAsString)));
		assertEquals("Wrong value from result conversion", returnValue,
				transformed);
	}

	@Test(expected = TransformerFactoryConfigurationError.class)
	public void testXsltPayloadWithBadTransformerFactoryClassname() throws Exception {
		transformer = new XsltPayloadTransformer(getXslResource(), "foo.bar.Baz");
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		transformer.doTransform(buildMessage(new StringSource(docAsString)));
	}

	@Test(expected = TransformerException.class)
	public void testNonXmlString() throws Exception {
		transformer.doTransform(buildMessage("test"));
	}

	@Test(expected = MessagingException.class)
	public void testUnsupportedPayloadType() throws Exception {
		transformer.doTransform(buildMessage(new Long(12)));
	}

	@Test
	public void testXsltWithImports() throws Exception {
		Resource resource = new ClassPathResource("transform-with-import.xsl",
				this.getClass());
		transformer = new XsltPayloadTransformer(resource);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		assertEquals(transformer.doTransform(buildMessage(docAsString)),
				outputAsString);
	}


	@Test
	public void documentInStringResultOut() throws Exception {
		Resource resource = new ClassPathResource("transform-with-import.xsl",
				this.getClass());
		transformer = new XsltPayloadTransformer(resource);
		transformer.setResultFactory(new StringResultFactory());
		transformer.setAlwaysUseResultFactory(true);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object returned = transformer.doTransform(buildMessage(XmlTestUtil.getDocumentForString(docAsString)));
		assertEquals("Wrong type of return ", StringResult.class, returned.getClass());
	}


	@Test
	public void stringInDomResultOut() throws Exception {
		Resource resource = new ClassPathResource("transform-with-import.xsl",
				this.getClass());
		transformer = new XsltPayloadTransformer(resource);
		transformer.setResultFactory(new StringResultFactory());
		transformer.setAlwaysUseResultFactory(true);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object returned = transformer.doTransform(buildMessage(XmlTestUtil.getDocumentForString(docAsString)));
		assertEquals("Wrong type of return ", StringResult.class, returned.getClass());
	}

	@Test
	public void docInStringOut() throws Exception {
		transformer = new XsltPayloadTransformer(getXslResourceThatOutputsText());
		transformer.setResultFactory(new StringResultFactory());
		transformer.setAlwaysUseResultFactory(true);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object returned = transformer.doTransform(buildMessage(XmlTestUtil.getDocumentForString(docAsString)));
		assertEquals("Wrong type of return ", StringResult.class, returned.getClass());
		assertEquals("Wrong content in string", "hello world", returned.toString());
	}

	protected Message<?> buildMessage(Object payload) {
		return MessageBuilder.withPayload(payload).build();
	}

	private Resource getXslResource() throws Exception {
		String xsl = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
				"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
				"   <xsl:template match=\"order\">" +
				"     <bob>test</bob>" +
				"   </xsl:template>" +
				"</xsl:stylesheet>";
		return new ByteArrayResource(xsl.getBytes("UTF-8"));
	}

	private Resource getXslResourceThatOutputsText() throws Exception {
		String xsl = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
				"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
				"   <xsl:output method=\"text\" encoding=\"UTF-8\" />" +
				"   <xsl:template match=\"order\">hello world</xsl:template>" +
				"</xsl:stylesheet>";
		return new ByteArrayResource(xsl.getBytes("UTF-8"));
	}

	public static class StubResultTransformer implements ResultTransformer {

		private final Object objectToReturn;

		public StubResultTransformer(Object objectToReturn) {
			this.objectToReturn = objectToReturn;
		}

		public Object transformResult(Result result) {
			return objectToReturn;
		}
	}

}
