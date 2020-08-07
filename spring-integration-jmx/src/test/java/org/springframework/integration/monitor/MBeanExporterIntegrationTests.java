/*
 * Copyright 2009-2020 the original author or authors.
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

package org.springframework.integration.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class MBeanExporterIntegrationTests {

	private IntegrationMBeanExporter messageChannelsMonitor;

	private GenericXmlApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testCircularReferenceNoChannel() {
		context = new GenericXmlApplicationContext(getClass(), "oref-nonchannel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
	}

	@Test
	public void testCircularReferenceNoChannelInFactoryBean() {
		context = new GenericXmlApplicationContext(getClass(), "oref-factory-nonchannel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
	}

	@Test
	public void testCircularReferenceWithChannel() {
		context = new GenericXmlApplicationContext(getClass(), "oref-channel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
	}

	@Test
	public void testCircularReferenceWithChannelInFactoryBean() throws Exception {
		context = new GenericXmlApplicationContext(getClass(), "oref-factory-channel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
		assertThat(Arrays.asList(messageChannelsMonitor.getChannelNames()).contains("anonymous")).isTrue();
		MBeanServer server = context.getBean(MBeanServer.class);
		assertThat(server.queryNames(ObjectName.getInstance("com.foo:*"), null).size()).isEqualTo(1);
		assertThat(server.queryNames(ObjectName.getInstance("org.springframework.integration:name=anonymous,*"), null)
				.size()).isEqualTo(1);
	}

	@Test
	public void testCircularReferenceWithChannelInFactoryBeanAutodetected() {
		context = new GenericXmlApplicationContext(getClass(), "oref-factory-channel-autodetect.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
	}

	@Test
	public void testLifecycleInEndpointWithMessageSource() throws Exception {
		context = new GenericXmlApplicationContext(getClass(), "lifecycle-source.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names =
				server.queryNames(
						ObjectName.getInstance("org.springframework.integration:type=ManagedEndpoint,*"), null);
		assertThat(names.size()).isEqualTo(3);
		names = server.queryNames(ObjectName.getInstance("org.springframework.integration:name=explicit.adapter,*"), null);
		assertThat(names.size()).isEqualTo(1);
		MBeanOperationInfo[] operations = server.getMBeanInfo(names.iterator().next()).getOperations();
		String startName = null;
		for (MBeanOperationInfo info : operations) {
			String name = info.getName();
			if (name.startsWith("start")) {
				startName = name;
			}
		}
		// Lifecycle method name
		assertThat(startName).isEqualTo("start");
		assertThat((Boolean) server.invoke(names.iterator().next(), "isRunning", null, null)).isTrue();
		messageChannelsMonitor.stopActiveComponents(100);
		assertThat((Boolean) server.invoke(names.iterator().next(), "isRunning", null, null)).isFalse();
		ActiveChannel activeChannel = context.getBean("activeChannel", ActiveChannel.class);
		assertThat(activeChannel.isStopCalled()).isTrue();
		OtherActiveComponent otherActiveComponent = context.getBean(OtherActiveComponent.class);
		assertThat(otherActiveComponent.isBeforeCalled()).isTrue();
		assertThat(otherActiveComponent.isAfterCalled()).isTrue();
		assertThat(otherActiveComponent.isRunning()).isTrue();
		assertThat(context.getBean(AMessageProducer.class).isRunning()).isFalse();

		// check pollers are still running
		QueueChannel input = (QueueChannel) extractTarget(context.getBean("input"));
		QueueChannel input2 = (QueueChannel) extractTarget(context.getBean("input2"));
		input.purge(null);
		input2.purge(null);
		input.send(new GenericMessage<>("foo"));
		assertThat(input2.receive(10000)).isNotNull();
	}

	private Object extractTarget(Object bean) {
		if (!(bean instanceof Advised)) {
			return bean;
		}
		Advised advised = (Advised) bean;
		if (advised.getTargetSource() == null) {
			return null;
		}
		try {
			return extractTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			return null;
		}
	}

	@Test
	public void testSelfDestruction() throws Exception {
		context = new GenericXmlApplicationContext(getClass(), "self-destruction-context.xml");
		SourcePollingChannelAdapter adapter = context.getBean(SourcePollingChannelAdapter.class);
		adapter.start();
		int n = 0;
		while (adapter.isRunning()) {
			n += 10;
			if (n > 10000) {
				fail("Adapter failed to stop");
			}
			Thread.sleep(10);
		}
	}

	@Test
	public void testLifecycleInEndpointWithoutMessageSource() throws Exception {
		context = new GenericXmlApplicationContext(getClass(), "lifecycle-no-source.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names =
				server.queryNames(
						ObjectName.getInstance("org.springframework.integration:type=ManagedEndpoint,*"), null);
		assertThat(names.size()).isEqualTo(1);
		names = server.queryNames(ObjectName.getInstance("org.springframework.integration:name=gateway,*"), null);
		assertThat(names.size()).isEqualTo(1);
		MBeanOperationInfo[] operations = server.getMBeanInfo(names.iterator().next()).getOperations();
		String startName = null;
		for (MBeanOperationInfo info : operations) {
			String name = info.getName();
			if (name.startsWith("start")) {
				startName = name;
			}
		}
		// Lifecycle method name
		assertThat(startName).isEqualTo("start");

		context.close();

		context = new GenericXmlApplicationContext(getClass(), "lifecycle-no-source.xml");
		server = context.getBean(MBeanServer.class);
		names = server.queryNames(
				ObjectName.getInstance("org.springframework.integration:type=ManagedEndpoint,*"), null);
		assertThat(names.size()).isEqualTo(1);
		names = server.queryNames(ObjectName.getInstance("org.springframework.integration:name=gateway,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@Test
	public void testComponentNames() throws Exception {
		context = new GenericXmlApplicationContext(getClass(), "excluded-components.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names =
				server.queryNames(
						ObjectName.getInstance("org.springframework.integration:type=MessageChannel,*"), null);
		// Only one registered (out of >2 available)
		assertThat(names.size()).isEqualTo(1);
		names = server.queryNames(
				ObjectName.getInstance("org.springframework.integration:type=MessageHandler,*"), null);
		assertThat(names.size()).isEqualTo(0);
	}

	@Test
	public void testDuplicateComponentNames() throws Exception {
		context = new GenericXmlApplicationContext(getClass(), "duplicate-components.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertThat(messageChannelsMonitor).isNotNull();
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names =
				server.queryNames(
						ObjectName.getInstance("org.springframework.integration:type=ManagedEndpoint,*"), null);
		assertThat(names.size()).isEqualTo(2);
	}

	@Test
	public void testSingleMBeanServer() {
		AnnotationConfigApplicationContext ctx1 = new AnnotationConfigApplicationContext(Config1.class);
		AnnotationConfigApplicationContext ctx2 = new AnnotationConfigApplicationContext(Config2.class);
		assertThat(TestUtils.getPropertyValue(ctx2.getBean(IntegrationMBeanExporter.class), "server"))
				.isSameAs(TestUtils.getPropertyValue(ctx1.getBean(IntegrationMBeanExporter.class), "server"));
		ctx1.close();
		ctx2.close();
	}

	@EnableIntegration
	@EnableIntegrationMBeanExport(defaultDomain = "config1")
	public static class Config1 {

		@Bean
		public MBeanServerFactoryBean fb() {
			MBeanServerFactoryBean fb = new MBeanServerFactoryBean();
			fb.setLocateExistingServerIfPossible(true);
			return fb;
		}

	}

	@EnableIntegration
	@EnableIntegrationMBeanExport(defaultDomain = "config2")
	public static class Config2 {

		@Bean
		public MBeanServerFactoryBean fb() {
			MBeanServerFactoryBean fb = new MBeanServerFactoryBean();
			fb.setLocateExistingServerIfPossible(true);
			return fb;
		}

	}

	public static class BogusEndpoint extends AbstractEndpoint {

		@SuppressWarnings("unused")
		private IntegrationObjectSupport parent;

		public void setParent(IntegrationObjectSupport parent) {
			this.parent = parent;
			setComponentName(parent.getComponentName());
		}

		@Override
		protected void doStart() {
		}

		@Override
		protected void doStop() {
		}

	}

	public static class DateFactoryBean implements FactoryBean<Date> {

		private Date date;

		public void setDate(Date date) {
			this.date = date;
		}

		// This has the potential to blow up if called before setDate().
		// Depends on bean instantiation order and IntegrationMBeanExporter
		// can influence that by aggressively instantiating other MBeanExporters
		@Override
		public Date getObject() throws Exception {
			Assert.state(date != null, "A date must be provided");
			return date;
		}

		@Override
		public Class<?> getObjectType() {
			return Date.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	public static class DateHolder implements InitializingBean {

		private Date date;

		public void setDate(Date date) {
			this.date = date;
		}

		@Override
		public void afterPropertiesSet() {
			Assert.state(date != null, "A date must be provided");
		}

	}

	public static class MetricFactoryBean implements FactoryBean<Metric> {

		private MessageChannel channel;

		private final Metric date = new Metric();

		public void setChannel(MessageChannel channel) {
			this.channel = channel;
		}

		@Override
		public Metric getObject() {
			Assert.state(channel != null, "A channel must be provided");
			return date;
		}

		@Override
		public Class<?> getObjectType() {
			return Metric.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	@ManagedResource
	public static class Metric {

	}

	public static class MetricHolder implements InitializingBean {

		private MessageChannel channel;

		public void setChannel(MessageChannel channel) {
			this.channel = channel;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			Assert.state(channel != null, "A channel must be provided");
		}

	}

	public interface Service {

		String execute() throws Exception;

		int getCounter();

	}

	public static class SimpleService implements Service {

		private int counter;

		@Override
		public String execute() throws Exception {
			Thread.sleep(10L); // make the duration non-zero
			counter++;
			return "count=" + counter;
		}

		@Override
		public int getCounter() {
			return counter;
		}

	}

	public interface ActiveChannel {

		boolean isStopCalled();

	}

	public static class ActiveChannelImpl extends AbstractMessageChannel implements Lifecycle, ActiveChannel {

		private boolean stopCalled;

		@Override
		protected boolean doSend(Message<?> message, long timeout) {
			return false;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
			this.stopCalled = true;
		}

		@Override
		public boolean isRunning() {
			return false;
		}

		@Override
		public boolean isStopCalled() {
			return this.stopCalled;
		}

	}

	public static class OtherActiveComponent extends MessageProducerSupport
			implements OrderlyShutdownCapable {

		private boolean beforeCalled;

		private boolean afterCalled;

		public boolean isBeforeCalled() {
			return this.beforeCalled;
		}

		protected boolean isAfterCalled() {
			return afterCalled;
		}

		@Override
		public int beforeShutdown() {
			this.beforeCalled = true;
			return 0;
		}

		@Override
		public int afterShutdown() {
			this.afterCalled = true;
			return 0;
		}

	}

	public static class AMessageProducer extends MessageProducerSupport {

	}

}
