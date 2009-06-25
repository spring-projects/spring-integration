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
public class InnerDefinitionHandlerAwareEndpointParserTests {

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
	
	@Test
	public void testInnerRouterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("router-inner-success");
		this.testRouterDefinitionSuccess(configProperty);
	}
	@Test
	public void testRefRouterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("router-ref-success");
		this.testRouterDefinitionSuccess(configProperty);
	}
	
	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerRouterDefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("router-failure-refAndBean");
		ByteArrayInputStream stream = new ByteArrayInputStream(xmlConfig.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
	}
	
	@Test
	public void testInnerSADefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("sa-inner-success");
		this.testSADefinitionSuccess(configProperty);
	}
	@Test
	public void testRefSADefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("sa-ref-success");
		this.testSADefinitionSuccess(configProperty);
	}
	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerSADefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("sa-failure-refAndBean");
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
	private void testRouterDefinitionSuccess(String configProperty){
		ByteArrayInputStream stream = new ByteArrayInputStream(configProperty.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		EventDrivenConsumer splitter = (EventDrivenConsumer) ac.getBean("testRouter");
		Assert.assertNotNull(splitter);
		MessageBuilder inChannelMessageBuilder = MessageBuilder.withPayload("1");
		Message inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = (DirectChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel channel1 = (PollableChannel) ac.getBean("channel1");
		Assert.assertTrue(channel1.receive().getPayload().equals("1"));
		inChannelMessageBuilder = MessageBuilder.withPayload("2");
		inMessage = inChannelMessageBuilder.build();
		inChannel.send(inMessage);
		PollableChannel channel2 = (PollableChannel) ac.getBean("channel2");
		Assert.assertTrue(channel2.receive().getPayload().equals("2"));
	}
	private void testSADefinitionSuccess(String configProperty){
		ByteArrayInputStream stream = new ByteArrayInputStream(configProperty.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		EventDrivenConsumer splitter = (EventDrivenConsumer) ac.getBean("testServiceActivator");
		Assert.assertNotNull(splitter);
		MessageBuilder inChannelMessageBuilder = MessageBuilder.withPayload("1");
		Message inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = (DirectChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel channel1 = (PollableChannel) ac.getBean("outChannel");
		Assert.assertTrue(channel1.receive().getPayload().equals("1"));
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
	
	public static class TestRouter{
		public String route(String value) {
			return (value.equals("1")) ? "channel1" : "channel2";
		}
	}
	public static class TestServiceActivator{
		public String foo(String value) {
			return value;
		}
	}
		
}
