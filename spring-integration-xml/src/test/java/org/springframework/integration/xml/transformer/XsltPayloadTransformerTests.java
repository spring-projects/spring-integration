/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

	private XsltPayloadTransformer testTransformer;

	private final String docAsString =
			"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	private final String outputAsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><bob>test</bob>";

	@TempDir
	public File temporaryFolder;

	@BeforeEach
	public void setUp() throws Exception {
		this.testTransformer = new XsltPayloadTransformer(getXslTemplates());
		this.testTransformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		this.testTransformer.setAlwaysUseResultFactory(false);
		this.testTransformer.afterPropertiesSet();
	}

	@Test
	public void testDocumentAsPayload() throws Exception {
		Message<?> message = new GenericMessage<>(XmlTestUtil.getDocumentForString(this.docAsString));
		Object transformed = this.testTransformer.doTransform(message);
		assertThat(transformed)
				.as("Wrong return type for document payload")
				.isInstanceOf(Document.class);
		assertThat(transformed).and(this.outputAsString).areSimilar();
	}

	@Test
	public void testSourceAsPayload() throws Exception {
		GenericMessage<?> message = new GenericMessage<>(new StringSource(this.docAsString));
		Object transformed = testTransformer.doTransform(message);

		assertThat(transformed)
				.as("Wrong return type for document payload")
				.isInstanceOf(DOMResult.class);

		DOMResult result = (DOMResult) transformed;
		assertThat(result.getNode())
				.as("Document incorrect after transformation")
				.and(this.outputAsString)
				.areSimilar();
	}

	@Test
	public void testStringAsPayload() {
		Object transformed = this.testTransformer.doTransform(new GenericMessage<>(this.docAsString));

		assertThat(transformed)
				.as("Wrong return type for document payload")
				.isInstanceOf(String.class);

		assertThat(transformed)
				.as("String incorrect after transform")
				.and(this.outputAsString)
				.areIdentical();
	}

	@Test
	public void testStringAsPayloadUseResultFactoryTrue() throws Exception {
		this.testTransformer.setAlwaysUseResultFactory(true);
		Object transformed = testTransformer.doTransform(new GenericMessage<>(this.docAsString));

		assertThat(transformed)
				.as("Wrong return type for useFactories true")
				.isInstanceOf(DOMResult.class);

		DOMResult result = (DOMResult) transformed;
		assertThat(result.getNode())
				.as("Document incorrect after transformation")
				.and(this.outputAsString)
				.areSimilar();
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
		XsltPayloadTransformer transformer =
				new XsltPayloadTransformer(getXslResourceThatOutputsText(), "foo.bar.Baz");
		transformer.setBeanFactory(Mockito.mock(BeanFactory.class));
		assertThatIllegalStateException()
				.isThrownBy(transformer::afterPropertiesSet)
				.withCauseExactlyInstanceOf(ClassNotFoundException.class);
	}

	@Test
	public void testNonXmlString() {
		assertThatExceptionOfType(IllegalStateException.class)
				.isThrownBy(() -> this.testTransformer.doTransform(new GenericMessage<>("test")))
				.withCauseInstanceOf(TransformerException.class);
	}

	@Test
	public void testUnsupportedPayloadType() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.testTransformer.doTransform(new GenericMessage<>(12)));
	}

	@Test
	public void testXsltWithImports() {
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

		this.temporaryFolder.mkdir();
		File xsltFile = File.createTempFile("test", null, this.temporaryFolder);
		FileCopyUtils.copy(xsl.getBytes(), xsltFile);
		return new FileSystemResource(xsltFile);
	}

	public static class StubResultTransformer implements ResultTransformer {

		private final Object objectToReturn;

		public StubResultTransformer(Object objectToReturn) {
			this.objectToReturn = objectToReturn;
		}

		@Override
		public Object transformResult(Result result) {
			return objectToReturn;
		}

	}

}
