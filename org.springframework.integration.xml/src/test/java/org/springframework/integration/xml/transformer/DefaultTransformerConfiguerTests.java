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

public class DefaultTransformerConfiguerTests {
	
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
		transformerConfigurer.configureTransfomer(testMessage, transformer);
		
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
	public void testNonStringOutputPropertyHeader(){
		Message<String> testMessage = MessageBuilder.withPayload("test")
		.setHeader("xslt_output_property_outOne",12)
		.build();
		transformerConfigurer.configureTransfomer(testMessage, transformer);
	}
	
	private static class StubTransformer extends Transformer{

		Map<String,Object> paramterMap = new HashMap<String, Object>();
		
		Map<String, String> outputProperties = new HashMap<String, String>();
		
		@Override
		public void clearParameters() {
			paramterMap.clear();
			
		}

		@Override
		public ErrorListener getErrorListener() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Properties getOutputProperties() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getOutputProperty(String name)
				throws IllegalArgumentException {
			return outputProperties.get(name);
		}

		@Override
		public Object getParameter(String name) {
			return paramterMap.get(name);
		}

		@Override
		public URIResolver getURIResolver() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setErrorListener(ErrorListener listener)
				throws IllegalArgumentException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setOutputProperties(Properties oformat) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setOutputProperty(String name, String value)
				throws IllegalArgumentException {
			outputProperties.put(name, value);
		}

		@Override
		public void setParameter(String name, Object value) {
			paramterMap.put(name, value);	
		}

		@Override
		public void setURIResolver(URIResolver resolver) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void transform(Source xmlSource, Result outputTarget)
				throws TransformerException {
			// TODO Auto-generated method stub
			
		}
		
		
	}

}
