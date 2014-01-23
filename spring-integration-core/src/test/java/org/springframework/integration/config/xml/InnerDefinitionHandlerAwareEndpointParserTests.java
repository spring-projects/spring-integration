/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
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
	public void testInnerSplitterDefinitionSuccessWithPoller(){
		String configProperty = testConfigurations.getProperty("splitter-inner-success-with-poller");
		this.bootStrap(configProperty);
	}

	@Test
	public void testInnerSplitterDefinitionSuccessWithPollerReversedOrder(){
		String configProperty = testConfigurations.getProperty("splitter-inner-success-with-poller-reversed-order");
		this.bootStrap(configProperty);
	}

	@Test
	public void testRefSplitterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("splitter-ref-success");
		this.testSplitterDefinitionSuccess(configProperty);
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerSplitterDefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("splitter-failure-refAndBean");
		this.bootStrap(xmlConfig);
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
		this.bootStrap(xmlConfig);
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
		this.bootStrap(xmlConfig);
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
		this.bootStrap(xmlConfig);
	}

	@Test
	public void testInnerAggregatorDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("aggregator-inner-success");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerConcurrentAggregatorDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("aggregator-inner-concurrent-success");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerConcurrentAggregatorDefinitionSuccessReorderBeanPoller(){
		String configProperty = testConfigurations.getProperty("aggregator-inner-concurrent-success-reorder-bean-poller");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testRefAggregatorDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("aggregator-ref-success");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerAggregatorDefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("aggregator-failure-refAndBean");
		this.bootStrap(xmlConfig);
	}

	@Test
	public void testInnerFilterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("filter-inner-success");
		this.testFilterDefinitionSuccess(configProperty);
	}

	@Test
	public void testRefFilterDefinitionSuccess(){
		String configProperty = testConfigurations.getProperty("filter-ref-success");
		this.testFilterDefinitionSuccess(configProperty);
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testInnerFilterDefinitionFailureRefAndInner(){
		String xmlConfig = testConfigurations.getProperty("filter-failure-refAndBean");
		this.bootStrap(xmlConfig);
	}

	private void testSplitterDefinitionSuccess(String configProperty){
		ApplicationContext ac = this.bootStrap(configProperty);
		EventDrivenConsumer splitter = (EventDrivenConsumer) ac.getBean("testSplitter");
		Assert.assertNotNull(splitter);
		MessageBuilder<String[]> inChannelMessageBuilder = MessageBuilder.withPayload(new String[]{"One","Two"});
		Message<String[]> inMessage = inChannelMessageBuilder.build();
		MessageChannel inChannel = (MessageChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel outChannel = (PollableChannel) ac.getBean("outChannel");
		Assert.assertTrue(outChannel.receive().getPayload() instanceof String);
		outChannel = (PollableChannel) ac.getBean("outChannel");
		Assert.assertTrue(outChannel.receive().getPayload() instanceof String);
	}

	private void testTransformerDefinitionSuccess(String configProperty){
		ApplicationContext ac = this.bootStrap(configProperty);
		EventDrivenConsumer transformer = (EventDrivenConsumer) ac.getBean("testTransformer");
		Assert.assertNotNull(transformer);
		MessageBuilder<String[]> inChannelMessageBuilder = MessageBuilder.withPayload(new String[]{"One","Two"});
		Message<String[]> inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = (DirectChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel outChannel = (PollableChannel) ac.getBean("outChannel");
		String payload = (String) outChannel.receive().getPayload();
		Assert.assertTrue(payload.equals("One,Two"));
	}

	private void testRouterDefinitionSuccess(String configProperty){
		ApplicationContext ac = this.bootStrap(configProperty);
		EventDrivenConsumer splitter = (EventDrivenConsumer) ac.getBean("testRouter");
		Assert.assertNotNull(splitter);
		MessageBuilder<String> inChannelMessageBuilder = MessageBuilder.withPayload("1");
		Message<String> inMessage = inChannelMessageBuilder.build();
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
		ApplicationContext ac = this.bootStrap(configProperty);
		EventDrivenConsumer splitter = (EventDrivenConsumer) ac.getBean("testServiceActivator");
		Assert.assertNotNull(splitter);
		MessageBuilder<String> inChannelMessageBuilder = MessageBuilder.withPayload("1");
		Message<String> inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = (DirectChannel) ac.getBean("inChannel");
		inChannel.send(inMessage);
		PollableChannel channel1 = (PollableChannel) ac.getBean("outChannel");
		Assert.assertTrue(channel1.receive().getPayload().equals("1"));
	}

	private void testAggregatorDefinitionSuccess(String configProperty){
		ApplicationContext ac = this.bootStrap(configProperty);
		MessageChannel inChannel = (MessageChannel) ac.getBean("inChannel");
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			Message<Integer> message = MessageBuilder.withPayload(i).copyHeaders(headers).build();
			inChannel.send(message);
		}
		PollableChannel output = (PollableChannel) ac.getBean("outChannel");
		Message<?> receivedMessage = output.receive(10000);
		assertNotNull(receivedMessage);
		assertEquals(0 + 1 + 2 + 3 + 4, receivedMessage.getPayload());
	}

	private void testFilterDefinitionSuccess(String configProperty){
		ApplicationContext ac = this.bootStrap(configProperty);
		MessageChannel input = (MessageChannel) ac.getBean("inChannel");
		PollableChannel output = (PollableChannel) ac.getBean("outChannel");
		input.send(new GenericMessage<String>("foo"));
		Message<?> reply = output.receive(0);
		assertEquals("foo", reply.getPayload());
	}

	private ApplicationContext bootStrap(String configProperty){
		ByteArrayInputStream stream = new ByteArrayInputStream(configProperty.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correllationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correllationId);
		return headers;
	}

	@SuppressWarnings("unchecked")
	public static class TestSplitter{
		public Collection<String> split(String[] payload) {
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

	public static class TestAggregator{
		public Integer sum(List<Integer> numbers) {
			int result = 0;
			for (Integer number : numbers) {
				result += number;
			}
			return result;
		}
	}

	public static class TestMessageFilter{
		public boolean filter(String value) {
			return value.equals("foo");
		}
	}

}
