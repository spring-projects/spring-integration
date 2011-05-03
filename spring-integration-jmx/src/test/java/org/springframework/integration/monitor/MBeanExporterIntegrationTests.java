/*
 * Copyright 2009-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;

public class MBeanExporterIntegrationTests {

	private IntegrationMBeanExporter messageChannelsMonitor;
	
	private GenericXmlApplicationContext context;
	
	@After
	public void close() {
		if (context!=null) {
			context.close();
		}
	}

	@Test
	public void testCircularReferenceNoChannel() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "oref-nonchannel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
	}

	@Test
	public void testCircularReferenceNoChannelInFactoryBean() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "oref-factory-nonchannel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
	}

	@Test
	public void testCircularReferenceWithChannel() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "oref-channel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
	}

	@Test
	public void testCircularReferenceWithChannelInFactoryBean() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "oref-factory-channel.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
		assertTrue(Arrays.asList(messageChannelsMonitor.getChannelNames()).contains("anonymous"));
		MBeanServer server = context.getBean(MBeanServer.class);
		assertEquals(1, server.queryNames(ObjectName.getInstance("com.foo:*"), null).size());
		assertEquals(1, server.queryNames(ObjectName.getInstance("org.springframework.integration:name=anonymous,*"), null).size());
	}

	@Test
	public void testCircularReferenceWithChannelInFactoryBeanAutodetected() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "oref-factory-channel-autodetect.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
	}

	@Test
	public void testLifecycleInEndpointWithMessageSource() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "lifecycle-source.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names = server.queryNames(ObjectName.getInstance("org.springframework.integration:type=ManagedEndpoint,*"), null);
		assertEquals(0, names.size());
		names = server.queryNames(ObjectName.getInstance("org.springframework.integration:name=explicit,*"), null);
		assertEquals(1, names.size());
		MBeanOperationInfo[] operations = server.getMBeanInfo(names.iterator().next()).getOperations();
		String startName = null;
		for (MBeanOperationInfo info : operations) {
			String name = info.getName();
			if (name.startsWith("start")) {
				startName = name;
			}
		}
		// Lifecycle method name
		assertEquals("start", startName);
	}

	@Test
	public void testLifecycleInEndpointWithoutMessageSource() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "lifecycle-no-source.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names = server.queryNames(ObjectName.getInstance("org.springframework.integration:type=ManagedEndpoint,*"), null);
		assertEquals(1, names.size());
		names = server.queryNames(ObjectName.getInstance("org.springframework.integration:name=gateway,*"), null);
		assertEquals(1, names.size());
		MBeanOperationInfo[] operations = server.getMBeanInfo(names.iterator().next()).getOperations();
		String startName = null;
		for (MBeanOperationInfo info : operations) {
			String name = info.getName();
			if (name.startsWith("start")) {
				startName = name;
			}
		}
		// Lifecycle method name
		assertEquals("start", startName);
	}

	@Test
	public void testComponentNames() throws Exception {		
		context = new GenericXmlApplicationContext(getClass(), "excluded-components.xml");
		messageChannelsMonitor = context.getBean(IntegrationMBeanExporter.class);
		assertNotNull(messageChannelsMonitor);
		MBeanServer server = context.getBean(MBeanServer.class);
		Set<ObjectName> names = server.queryNames(ObjectName.getInstance("org.springframework.integration:type=MessageChannel,*"), null);
		// Only one registered (out of >2 available)
		assertEquals(1, names.size());
		names = server.queryNames(ObjectName.getInstance("org.springframework.integration:type=MessageHandler,*"), null);
		assertEquals(0, names.size());
	}

	public static class DateFactoryBean implements FactoryBean<Date> {

		private Date date;

		public void setDate(Date date) {
			this.date = date;
		}

		// This has the potential to blow up if called before setDate().
		// Depends on bean instantiation order and IntegrationMBeanExporter
		// can influence that by aggressively instantiating other MBeanExporters
		public Date getObject() throws Exception {
			Assert.state(date != null, "A date must be provided");
			return date;
		}

		public Class<?> getObjectType() {
			return Date.class;
		}

		public boolean isSingleton() {
			return true;
		}

	}

	public static class DateHolder implements InitializingBean {

		private Date date;

		public void setDate(Date date) {
			this.date = date;
		}

		public void afterPropertiesSet() throws Exception {
			Assert.state(date != null, "A date must be provided");
		}

	}

	public static class MetricFactoryBean implements FactoryBean<Metric> {

		private MessageChannel channel;
		private Metric date = new Metric();

		public void setChannel(MessageChannel channel) {
			this.channel = channel;
		}

		public Metric getObject() throws Exception {
			Assert.state(channel != null, "A channel must be provided");
			return date;
		}

		public Class<?> getObjectType() {
			return Metric.class;
		}

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

		public void afterPropertiesSet() throws Exception {
			Assert.state(channel != null, "A channel must be provided");
		}

	}

	public static interface Service {
		String execute() throws Exception;
		int getCounter();
	}
	
	public static class SimpleService implements Service {
		private int counter;

		public String execute() throws Exception {
			Thread.sleep(10L); // make the duration non-zero
			counter++;
			return "count="+counter;
		}

		public int getCounter() {
			return counter;
		}
	}

}
