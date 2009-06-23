/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.integration.config.xml;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
/**
 * 
 * @author Oleg Zhurakousky
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InnerDefinitionAwareEndpointParserTests {

	@Autowired
	private Properties testConfigurations;

	@Test
	public void testInnerSplitterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("splitter-inner-success");
		this.testSplitterDefinitionSuccess(configProperty);
	}
	@Test
	public void testRefSplitterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("splitter-ref-success");
		this.testSplitterDefinitionSuccess(configProperty);
	}
	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerSplitterDefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("splitter-failure-refAndBean");
		ByteArrayInputStream stream = new ByteArrayInputStream(xmlConfig.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
	}
	
	@Test
	public void testInnerTransformerDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("transformer-inner-success");
		this.testTransformerDefinitionSuccess(configProperty);
	}
	@Test
	public void testRefTransformerDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("transformer-ref-success");
		this.testTransformerDefinitionSuccess(configProperty);
	}
	
	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerTransformerDefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("transformer-failure-refAndBean");
		ByteArrayInputStream stream = new ByteArrayInputStream(xmlConfig.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
	}
	
	private void testSplitterDefinitionSuccess(String configProperty){
		ByteArrayInputStream stream = new ByteArrayInputStream(configProperty.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		EventDrivenConsumer splitter = (EventDrivenConsumer) ac.getBean("testSplitter");
		Assert.assertNotNull(splitter);
		MessageBuilder inChannelMessageBuilder = MessageBuilder.withPayload(new String[]{"One","Two"});
		Message inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = (DirectChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel outChannel = (PollableChannel) ac.getBean("outChannel");
		Assert.assertTrue(outChannel.receive().getPayload() instanceof String);
		outChannel = (PollableChannel) ac.getBean("outChannel");
		Assert.assertTrue(outChannel.receive().getPayload() instanceof String);
	}
	
	private void testTransformerDefinitionSuccess(String configProperty){
		ByteArrayInputStream stream = new ByteArrayInputStream(configProperty.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		EventDrivenConsumer transformer = (EventDrivenConsumer) ac.getBean("testTransformer");
		Assert.assertNotNull(transformer);
		MessageBuilder inChannelMessageBuilder = MessageBuilder.withPayload(new String[]{"One","Two"});
		Message inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = (DirectChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel outChannel = (PollableChannel) ac.getBean("outChannel");
		String payload = (String) outChannel.receive().getPayload();
		Assert.assertTrue(payload.equals("One,Two"));
	}

	public static class TestSplitter{
		public Collection split(String[] payload){
			return CollectionUtils.arrayToList(payload);
		}
	}
	
	public static class TestTransformer{
		public String split(String[] payload){
			return StringUtils.arrayToDelimitedString(payload, ",");
		}
	}
		
}
