/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Jonas Partner
 */
public class DefaultTransformerConfigurerTests {
	
	StubTransformer transformer;
	
	DefaultTransformerConfigurer transformerConfigurer;
	
	@Before
	public void setUp(){
		this.transformer = new StubTransformer();
		this.transformerConfigurer = new DefaultTransformerConfigurer();
	}
	
	@Test
	public void testSettingParametersAndOutputProperties(){
		Message<String> testMessage = MessageBuilder.withPayload("test")
				.setHeader("xslt_parameter_headerOne",1)
				.setHeader("xslt_parameter_headerTwo", "string")
				.setHeader("xslt_output_property_outOne","1")
				.setHeader("xslt_output_property_outTwo","2")
				.build();
		transformerConfigurer.configureTransformer(testMessage, transformer);

		Object paramOne = transformer.getParameter("headerOne");
		assertEquals("Wrong value for headerOne parameter",1, paramOne);
		
		Object paramTwo = transformer.getParameter("headerTwo");
		assertEquals("Wrong value for headerTwo parameter","string", paramTwo);

		String outPropertyOne = transformer.getOutputProperty("outOne");
		assertEquals("Wrong value for headerOne parameter","1", outPropertyOne);
		
		String outPropertyTwo = transformer.getOutputProperty("outTwo");
		assertEquals("Wrong value for headerTwo parameter","2", outPropertyTwo);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNonStringOutputPropertyHeader() {
		Message<String> testMessage = MessageBuilder.withPayload("test")
				.setHeader("xslt_output_property_outOne",12)
				.build();
		transformerConfigurer.configureTransformer(testMessage, transformer);
	}


	private static class StubTransformer extends Transformer{

		Map<String,Object> parameterMap = new HashMap<String, Object>();
		
		Map<String, String> outputProperties = new HashMap<String, String>();
		
		@Override
		public void clearParameters() {
			parameterMap.clear();
		}

		@Override
		public ErrorListener getErrorListener() {
			return null;
		}

		@Override
		public Properties getOutputProperties() {
			return null;
		}

		@Override
		public String getOutputProperty(String name) throws IllegalArgumentException {
			return outputProperties.get(name);
		}

		@Override
		public Object getParameter(String name) {
			return parameterMap.get(name);
		}

		@Override
		public URIResolver getURIResolver() {
			return null;
		}

		@Override
		public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
		}

		@Override
		public void setOutputProperties(Properties oformat) {
		}

		@Override
		public void setOutputProperty(String name, String value) throws IllegalArgumentException {
			outputProperties.put(name, value);
		}

		@Override
		public void setParameter(String name, Object value) {
			parameterMap.put(name, value);	
		}

		@Override
		public void setURIResolver(URIResolver resolver) {
		}

		@Override
		public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
		}
	}

}
