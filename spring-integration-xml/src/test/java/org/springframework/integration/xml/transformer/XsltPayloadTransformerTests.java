/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.w3c.dom.Document;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileCopyUtils;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;


/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Mike Bazos
 * @author Artem Bilan
 */
public class XsltPayloadTransformerTests {

	private XsltPayloadTransformer transformer;

	private final String docAsString =
			"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	private final String outputAsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><bob>test</bob>";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		this.transformer = new XsltPayloadTransformer(getXslTemplates());
		this.transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		this.transformer.setAlwaysUseResultFactory(false);
		this.transformer.afterPropertiesSet();
	}

	@Test
	public void testDocumentAsPayload() throws Exception {
		Message<?> message = new GenericMessage<>(XmlTestUtil.getDocumentForString(this.docAsString));
		Object transformed = this.transformer.doTransform(message);
		assertThat(transformed)
				.as("Wrong return type for document payload")
				.isInstanceOf(Document.class);
		Document transformedDocument = (Document) transformed;
		assertThat(XmlTestUtil.docToString(transformedDocument)).isXmlEqualTo(this.outputAsString);
	}

	@Test
	public void testSourceAsPayload() throws Exception {
		GenericMessage<?> message = new GenericMessage<>(new StringSource(this.docAsString));
		Object transformed = transformer.doTransform(message);

		assertThat(transformed)
				.as("Wrong return type for document payload")
				.isInstanceOf(DOMResult.class);

		DOMResult result = (DOMResult) transformed;
		assertThat(XmlTestUtil.docToString((Document) result.getNode()))
				.as("Document incorrect after transformation")
				.isXmlEqualTo(this.outputAsString);
	}

	@Test
	public void testStringAsPayload() throws Exception {
		Object transformed = this.transformer.doTransform(new GenericMessage<>(this.docAsString));

		assertThat(transformed)
				.as("Wrong return type for document payload")
				.isInstanceOf(String.class);

		String transformedString = (String) transformed;
		assertThat(transformedString)
				.as("String incorrect after transform")
				.isXmlEqualTo(this.outputAsString);
	}

	@Test
	public void testStringAsPayloadUseResultFactoryTrue() throws Exception {
		this.transformer.setAlwaysUseResultFactory(true);
		Object transformed = transformer.doTransform(new GenericMessage<>(this.docAsString));

		assertThat(transformed)
				.as("Wrong return type for useFactories true")
				.isInstanceOf(DOMResult.class);

		DOMResult result = (DOMResult) transformed;
		assertThat(XmlTestUtil.docToString((Document) result.getNode()))
				.as("Document incorrect after transformation")
				.isXmlEqualTo(this.outputAsString);
	}

	@Test
	public void testSourceWithResultTransformer() throws Exception {
		Integer returnValue = 13;
		XsltPayloadTransformer transformer =
				new XsltPayloadTransformer(getXslTemplates(), new StubResultTransformer(returnValue));
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object transformed = transformer
				.doTransform(new GenericMessage<>(new StringSource(docAsString)));
		assertThat(transformed).isEqualTo(returnValue);
	}

	@Test
	public void testXsltPayloadWithTransformerFactoryClassName() throws Exception {
		Integer returnValue = 13;
		XsltPayloadTransformer transformer =
				new XsltPayloadTransformer(getXslResourceThatOutputsText(), new StubResultTransformer(returnValue),
						"com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object transformed = transformer.doTransform(new GenericMessage<>(new StringSource(this.docAsString)));
		assertThat(transformed)
				.as("Wrong value from result conversion")
				.isEqualTo(returnValue);
	}

	@Test
	public void testXsltPayloadWithBadTransformerFactoryClassName() throws IOException {
		XsltPayloadTransformer transformer = new XsltPayloadTransformer(getXslResourceThatOutputsText(), "foo.bar.Baz");
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		assertThatThrownBy(transformer::afterPropertiesSet)
				.isExactlyInstanceOf(TransformerFactoryConfigurationError.class);
	}

	@Test
	public void testNonXmlString() {
		assertThatThrownBy(() -> this.transformer.doTransform(new GenericMessage<>("test")))
				.isExactlyInstanceOf(TransformerException.class);
	}

	@Test
	public void testUnsupportedPayloadType() {
		assertThatThrownBy(() -> this.transformer.doTransform(new GenericMessage<>(12)))
				.isExactlyInstanceOf(MessagingException.class);
	}

	@Test
	public void testXsltWithImports() throws Exception {
		Resource resource = new ClassPathResource("transform-with-import.xsl", getClass());
		XsltPayloadTransformer transformer = new XsltPayloadTransformer(resource);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		Object transformed = transformer.doTransform(new GenericMessage<>(this.docAsString));
		assertThat(transformed).isEqualTo(this.outputAsString);
	}


	@Test
	public void documentInStringResultOut() throws Exception {
		Resource resource = new ClassPathResource("transform-with-import.xsl", getClass());
		XsltPayloadTransformer transformer = new XsltPayloadTransformer(resource);
		transformer.setResultFactory(new StringResultFactory());
		transformer.setAlwaysUseResultFactory(true);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		GenericMessage<Document> message = new GenericMessage<>(XmlTestUtil.getDocumentForString(this.docAsString));
		Object transformed = transformer.doTransform(message);
		assertThat(transformed)
				.as("Wrong type of return")
				.isInstanceOf(StringResult.class);
	}


	@Test
	public void stringInDomResultOut() throws Exception {
		Resource resource = new ClassPathResource("transform-with-import.xsl", getClass());
		XsltPayloadTransformer transformer = new XsltPayloadTransformer(resource);
		transformer.setResultFactory(new DomResultFactory());
		transformer.setAlwaysUseResultFactory(true);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		GenericMessage<Document> message = new GenericMessage<>(XmlTestUtil.getDocumentForString(this.docAsString));
		Object transformed = transformer.doTransform(message);
		assertThat(transformed)
				.as("Wrong type of return")
				.isInstanceOf(DOMResult.class);
	}

	@Test
	public void docInStringOut() throws Exception {
		XsltPayloadTransformer transformer = new XsltPayloadTransformer(getXslResourceThatOutputsText());
		transformer.setResultFactory(new StringResultFactory());
		transformer.setAlwaysUseResultFactory(true);
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		transformer.afterPropertiesSet();
		GenericMessage<Document> message = new GenericMessage<>(XmlTestUtil.getDocumentForString(this.docAsString));
		Object transformed = transformer.doTransform(message);
		assertThat(transformed)
				.as("Wrong type of return")
				.isInstanceOf(StringResult.class);
		assertThat(transformed.toString()).isEqualTo("hello world");
	}

	private Templates getXslTemplates() throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();

		String xsl = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
				"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
				"   <xsl:template match=\"order\">" +
				"     <bob>test</bob>" +
				"   </xsl:template>" +
				"</xsl:stylesheet>";

		return transformerFactory.newTemplates(new StringSource(xsl));
	}

	private Resource getXslResourceThatOutputsText() throws IOException {
		String xsl = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
				"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
				"   <xsl:output method=\"text\" encoding=\"UTF-8\" />" +
				"   <xsl:template match=\"order\">hello world</xsl:template>" +
				"</xsl:stylesheet>";

		File xsltFile = this.temporaryFolder.newFile();
		FileCopyUtils.copy(xsl.getBytes(), xsltFile);
		return new FileSystemResource(xsltFile);
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
