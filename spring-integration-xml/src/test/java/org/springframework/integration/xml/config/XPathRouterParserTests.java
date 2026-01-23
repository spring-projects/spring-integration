/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.xml.config;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@ContextConfiguration
@DirtiesContext
public class XPathRouterParserTests {

	String channelConfig = """
			<si:channel id='test-input'/>

			<si:channel id='outputOne'>
				<si:queue capacity='10'/>
			</si:channel>

			<si:channel id='defaultOutput'>
				<si:queue capacity='10'/>
			</si:channel>
			""";

	@Autowired @Qualifier("test-input")
	MessageChannel inputChannel;

	@Autowired @Qualifier("outputOne")
	QueueChannel outputChannel;

	@Autowired @Qualifier("defaultOutput")
	QueueChannel defaultOutput;

	ConfigurableApplicationContext appContext;

	public EventDrivenConsumer buildContext(String routerDef) {
		appContext = TestXmlApplicationContextHelper.getTestAppContext(channelConfig + routerDef);
		appContext.getAutowireCapableBeanFactory()
				.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		EventDrivenConsumer consumer = (EventDrivenConsumer) appContext.getBean("router");
		consumer.start();
		return consumer;
	}

	@AfterEach
	public void tearDown() {
		if (appContext != null) {
			appContext.close();
		}
	}

	@Test
	public void testParse() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("XPathRouterTests-context.xml", this.getClass());
		EventDrivenConsumer consumer = (EventDrivenConsumer) context.getBean("parseOnly");
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "handler.order")).isEqualTo(2);
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "phase")).isEqualTo(-1);
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "autoStartup")).isFalse();
		SmartLifecycleRoleController roleController = context.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list =
				(List<SmartLifecycle>) TestUtils.<MultiValueMap<?, ?>>getPropertyValue(roleController, "lifecycles")
						.get("foo");
		assertThat(list).containsExactly(consumer);
		context.close();
	}

	@Test
	public void testSimpleStringExpression() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		buildContext("""
				<si-xml:xpath-router id='router' input-channel='test-input'>
					<si-xml:xpath-expression expression='/name'/>
				</si-xml:xpath-router>
				""");
		inputChannel.send(docMessage);
		assertThat(outputChannel.getQueueSize()).as("Wrong number of messages").isEqualTo(1);
	}

	@Test
	public void testNamespacedStringExpression() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		buildContext("""
				<si-xml:xpath-router id='router' input-channel='test-input'>
					<si-xml:xpath-expression expression='/ns2:name' ns-prefix='ns2' ns-uri='www.example.org' />
				</si-xml:xpath-router>
				""");
		inputChannel.send(docMessage);
		assertThat(outputChannel.getQueueSize()).as("Wrong number of messages").isEqualTo(1);
	}

	@Test
	public void testStringExpressionWithNestedNamespaceMap() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("""
				<ns1:name xmlns:ns1='www.example.org' xmlns:ns2='www.example.org2'>
					<ns2:type>outputOne</ns2:type>
				</ns1:name>
				""");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		buildContext("""
				<si-xml:xpath-router id='router' input-channel='test-input'>
					<si-xml:xpath-expression expression='/ns1:name/ns2:type'>
						<map>
							<entry key='ns1' value='www.example.org' />
							<entry key='ns2' value='www.example.org2'/>
						</map>
					</si-xml:xpath-expression>
				</si-xml:xpath-router>
				""");
		inputChannel.send(docMessage);
		assertThat(outputChannel.getQueueSize()).as("Wrong number of messages").isEqualTo(1);
	}

	@Test
	public void testStringExpressionWithReferenceToNamespaceMap() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("""
				<ns1:name xmlns:ns1='www.example.org' xmlns:ns2='www.example.org2'>
					<ns2:type>outputOne</ns2:type>
				</ns1:name>
				""");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		String buffer = """
				<si-xml:xpath-router id='router' input-channel='test-input'>
					<si-xml:xpath-expression expression='/ns1:name/ns2:type' namespace-map='nsMap'/>
				</si-xml:xpath-router>

				<util:map id='nsMap'>
					<entry key='ns1' value='www.example.org' />
					<entry key='ns2' value='www.example.org2' />
				</util:map>
				""";

		buildContext(buffer);
		inputChannel.send(docMessage);
		assertThat(outputChannel.getQueueSize()).as("Wrong number of messages").isEqualTo(1);
	}

	@Test
	public void testSetResolutionRequiredFalse() {
		EventDrivenConsumer consumer = buildContext("""
				<si-xml:xpath-router id='router' resolution-required='false' input-channel='test-input'>
					<si-xml:xpath-expression expression='/name'/>
				</si-xml:xpath-router>
				""");

		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);
		Object resolutionRequired = accessor.getPropertyValue("resolutionRequired");
		assertThat(resolutionRequired).as("Resolution required not set to false ").isEqualTo(false);
	}

	@Test
	public void testSetResolutionRequiredTrue() {
		EventDrivenConsumer consumer = buildContext("""
				<si-xml:xpath-router id='router' resolution-required='true' input-channel='test-input'>
					<si-xml:xpath-expression expression='/name'/>
				</si-xml:xpath-router>
				""");

		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);
		Object resolutionRequired = accessor.getPropertyValue("resolutionRequired");
		assertThat(resolutionRequired).as("Resolution required not set to true ").isEqualTo(true);
	}

	@Test
	public void testSetDefaultOutputChannel() {
		EventDrivenConsumer consumer = buildContext("""
				<si-xml:xpath-router id='router' default-output-channel='defaultOutput' input-channel='test-input'>
					<si-xml:xpath-expression expression='/name'/>
				</si-xml:xpath-router>
				""");

		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);
		Object defaultOutputChannelValue = accessor.getPropertyValue("defaultOutputChannel");
		assertThat(defaultOutputChannelValue).as("Default output channel not correctly set ").isEqualTo(defaultOutput);
		inputChannel.send(MessageBuilder.withPayload("<unrelated/>").build());
		assertThat(defaultOutput.getQueueSize()).as("Wrong count of messages on default output channel").isEqualTo(1);
	}

	@Test
	public void testWithDynamicChanges() throws Exception {
		var ac = new ClassPathXmlApplicationContext("XPathRouterTests-context.xml", this.getClass());

		MessageChannel inputChannel = ac.getBean("xpathRouterEmptyChannel", MessageChannel.class);
		PollableChannel channelA = ac.getBean("channelA", PollableChannel.class);
		PollableChannel channelB = ac.getBean("channelB", PollableChannel.class);
		Document doc = XmlTestUtil.getDocumentForString("<name>channelA</name>");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		inputChannel.send(docMessage);
		assertThat(channelA.receive(10)).isNotNull();
		assertThat(channelB.receive(10)).isNull();

		EventDrivenConsumer routerEndpoint = ac.getBean("xpathRouterEmpty", EventDrivenConsumer.class);
		var xpathRouter = TestUtils.<AbstractMappingMessageRouter>getPropertyValue(routerEndpoint, "handler");
		xpathRouter.setChannelMapping("channelA", "channelB");
		inputChannel.send(docMessage);
		assertThat(channelB.receive(10)).isNotNull();
		assertThat(channelA.receive(10)).isNull();
		ac.close();
	}

	@Test
	public void testWithDynamicChangesWithExistingMappings() throws Exception {
		var ac = new ClassPathXmlApplicationContext("XPathRouterTests-context.xml", this.getClass());

		MessageChannel inputChannel = ac.getBean("xpathRouterWithMappingChannel", MessageChannel.class);
		PollableChannel channelA = ac.getBean("channelA", PollableChannel.class);
		PollableChannel channelB = ac.getBean("channelB", PollableChannel.class);
		Document doc = XmlTestUtil.getDocumentForString("<name>channelA</name>");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		inputChannel.send(docMessage);
		assertThat(channelA.receive(10)).isNull();
		assertThat(channelB.receive(10)).isNotNull();

		EventDrivenConsumer routerEndpoint = ac.getBean("xpathRouterWithMapping", EventDrivenConsumer.class);
		var xpathRouter = TestUtils.<AbstractMappingMessageRouter>getPropertyValue(routerEndpoint, "handler");
		xpathRouter.removeChannelMapping("channelA");
		inputChannel.send(docMessage);
		assertThat(channelA.receive(10)).isNotNull();
		assertThat(channelB.receive(10)).isNull();
		ac.close();
	}

	@Test
	public void testWithDynamicChangesWithExistingMappingsAndMultiChannel() throws Exception {
		var ac = new ClassPathXmlApplicationContext("XPathRouterTests-context.xml", this.getClass());

		MessageChannel inputChannel = ac.getBean("multiChannelRouterChannel", MessageChannel.class);
		PollableChannel channelA = ac.getBean("channelA", PollableChannel.class);
		PollableChannel channelB = ac.getBean("channelB", PollableChannel.class);
		Document doc = XmlTestUtil.getDocumentForString("""
				<root>
					<name>channelA</name>
					<name>channelB</name>
				</root>
				""");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		inputChannel.send(docMessage);
		assertThat(channelA.receive(10)).isNotNull();
		assertThat(channelA.receive(10)).isNotNull();
		assertThat(channelB.receive(10)).isNull();

		EventDrivenConsumer routerEndpoint = ac.getBean("xpathRouterWithMappingMultiChannel", EventDrivenConsumer.class);
		var xpathRouter = TestUtils.<AbstractMappingMessageRouter>getPropertyValue(routerEndpoint, "handler");
		xpathRouter.removeChannelMapping("channelA");
		xpathRouter.removeChannelMapping("channelB");
		inputChannel.send(docMessage);
		assertThat(channelA.receive(10)).isNotNull();
		assertThat(channelB.receive(10)).isNotNull();
		ac.close();
	}

	@Test
	public void testWithStringEvaluationType() throws Exception {
		var ac = new ClassPathXmlApplicationContext("XPathRouterTests-context.xml", this.getClass());
		MessageChannel inputChannel = ac.getBean("xpathStringChannel", MessageChannel.class);
		PollableChannel channelA = ac.getBean("channelA", PollableChannel.class);
		Document doc = XmlTestUtil.getDocumentForString("<channelA/>");
		GenericMessage<Document> docMessage = new GenericMessage<>(doc);
		inputChannel.send(docMessage);
		assertThat(channelA.receive(10)).isNotNull();
		ac.close();
	}

	@Test
	public void testWithCustomXmlPayloadConverter() {
		var ac = new ClassPathXmlApplicationContext("XPathRouterTests-context.xml", this.getClass());
		MessageChannel inputChannel = ac.getBean("customConverterChannel", MessageChannel.class);
		PollableChannel channelZ = ac.getBean("channelZ", PollableChannel.class);
		GenericMessage<String> message = new GenericMessage<>("<name>channelA</name>");
		inputChannel.send(message);
		Message<?> result = channelZ.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("<name>channelA</name>");
		ac.close();
	}

	@SuppressWarnings("unused")
	private static class TestXmlPayloadConverter extends DefaultXmlPayloadConverter {

		@Override
		public Document convertToDocument(Object object) {
			if (object instanceof String) {
				object = ((String) object).replace("A", "Z");
			}
			return super.convertToDocument(object);
		}

	}

}
