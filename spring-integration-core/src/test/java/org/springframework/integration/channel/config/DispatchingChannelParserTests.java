/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.channel.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DispatchingChannelParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Map<String, MessageChannel> channels;

	@Test
	public void taskExecutorOnly() {
		MessageChannel channel = channels.get("taskExecutorOnly");
		assertEquals(ExecutorChannel.class, channel.getClass());
		Object executor = getDispatcherProperty("executor", channel);
		assertEquals(ErrorHandlingTaskExecutor.class, executor.getClass());
		assertSame(context.getBean("taskExecutor"),
				new DirectFieldAccessor(executor).getPropertyValue("executor"));
		assertTrue((Boolean) getDispatcherProperty("failover", channel));
		assertEquals(RoundRobinLoadBalancingStrategy.class,
				getDispatcherProperty("loadBalancingStrategy", channel).getClass());
	}

	@Test
	public void failoverFalse() {
		MessageChannel channel = channels.get("failoverFalse");
		assertEquals(DirectChannel.class, channel.getClass());
		assertFalse((Boolean) getDispatcherProperty("failover", channel));
		assertEquals(RoundRobinLoadBalancingStrategy.class,
				getDispatcherProperty("loadBalancingStrategy", channel).getClass());
	}

	@Test
	public void failoverTrue() {
		MessageChannel channel = channels.get("failoverTrue");
		assertEquals(DirectChannel.class, channel.getClass());
		assertTrue((Boolean) getDispatcherProperty("failover", channel));
		assertEquals(RoundRobinLoadBalancingStrategy.class,
				getDispatcherProperty("loadBalancingStrategy", channel).getClass());
	}

	@Test
	public void loadBalancerDisabled() {
		MessageChannel channel = channels.get("loadBalancerDisabled");
		assertEquals(DirectChannel.class, channel.getClass());
		assertTrue((Boolean) getDispatcherProperty("failover", channel));
		assertNull(getDispatcherProperty("loadBalancingStrategy", channel));
	}

	@Test
	public void loadBalancerDisabledAndTaskExecutor() {
		MessageChannel channel = channels.get("loadBalancerDisabledAndTaskExecutor");
		assertEquals(ExecutorChannel.class, channel.getClass());
		assertTrue((Boolean) getDispatcherProperty("failover", channel));
		assertNull(getDispatcherProperty("loadBalancingStrategy", channel));
		Object executor = getDispatcherProperty("executor", channel);
		assertEquals(ErrorHandlingTaskExecutor.class, executor.getClass());
		assertSame(context.getBean("taskExecutor"),
				new DirectFieldAccessor(executor).getPropertyValue("executor"));
	}

	@Test
	public void roundRobinLoadBalancerAndTaskExecutor() {
		MessageChannel channel = channels.get("roundRobinLoadBalancerAndTaskExecutor");
		assertEquals(ExecutorChannel.class, channel.getClass());
		assertTrue((Boolean) getDispatcherProperty("failover", channel));
		assertEquals(RoundRobinLoadBalancingStrategy.class,
				getDispatcherProperty("loadBalancingStrategy", channel).getClass());
		Object executor = getDispatcherProperty("executor", channel);
		assertEquals(ErrorHandlingTaskExecutor.class, executor.getClass());
		assertSame(context.getBean("taskExecutor"),
				new DirectFieldAccessor(executor).getPropertyValue("executor"));
	}

	@Test
	public void loadBalancerRef() {
		MessageChannel channel = channels.get("lbRefChannel");
		LoadBalancingStrategy lbStrategy = TestUtils.getPropertyValue(channel, "dispatcher.loadBalancingStrategy", LoadBalancingStrategy.class);
		assertTrue(lbStrategy instanceof SampleLoadBalancingStrategy);
	}

	@Test
	public void loadBalancerRefFailWithLoadBalancer() {

		try {
			new ClassPathXmlApplicationContext("ChannelWithLoadBalancerRef-fail-config.xml", this.getClass());
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(), Matchers.containsString("'load-balancer' and 'load-balancer-ref' are mutually exclusive"));
		}

	}

	private static Object getDispatcherProperty(String propertyName, MessageChannel channel) {
		return new DirectFieldAccessor(
				new DirectFieldAccessor(channel).getPropertyValue("dispatcher"))
				.getPropertyValue(propertyName);
	}

	public static class SampleLoadBalancingStrategy implements LoadBalancingStrategy {

		@Override
		public Iterator<MessageHandler> getHandlerIterator(Message<?> message, Collection<MessageHandler> handlers) {
			return handlers.iterator();
		}
	}
}
