/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.channel.config;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

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

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(channel.getClass()).isEqualTo(ExecutorChannel.class);
		Object executor = getDispatcherProperty("executor", channel);
		assertThat(executor.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		assertThat(new DirectFieldAccessor(executor).getPropertyValue("executor"))
				.isSameAs(context.getBean("taskExecutor"));
		assertThat((Boolean) getDispatcherProperty("failover", channel)).isTrue();
		assertThat(getDispatcherProperty("loadBalancingStrategy", channel).getClass())
				.isEqualTo(RoundRobinLoadBalancingStrategy.class);
	}

	@Test
	public void failoverFalse() {
		MessageChannel channel = channels.get("failoverFalse");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		assertThat((Boolean) getDispatcherProperty("failover", channel)).isFalse();
		assertThat(getDispatcherProperty("loadBalancingStrategy", channel).getClass())
				.isEqualTo(RoundRobinLoadBalancingStrategy.class);
	}

	@Test
	public void failoverTrue() {
		MessageChannel channel = channels.get("failoverTrue");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		assertThat((Boolean) getDispatcherProperty("failover", channel)).isTrue();
		assertThat(getDispatcherProperty("loadBalancingStrategy", channel).getClass())
				.isEqualTo(RoundRobinLoadBalancingStrategy.class);
	}

	@Test
	public void loadBalancerDisabled() {
		MessageChannel channel = channels.get("loadBalancerDisabled");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		assertThat((Boolean) getDispatcherProperty("failover", channel)).isTrue();
		assertThat(getDispatcherProperty("loadBalancingStrategy", channel)).isNull();
	}

	@Test
	public void loadBalancerDisabledAndTaskExecutor() {
		MessageChannel channel = channels.get("loadBalancerDisabledAndTaskExecutor");
		assertThat(channel.getClass()).isEqualTo(ExecutorChannel.class);
		assertThat((Boolean) getDispatcherProperty("failover", channel)).isTrue();
		assertThat(getDispatcherProperty("loadBalancingStrategy", channel)).isNull();
		Object executor = getDispatcherProperty("executor", channel);
		assertThat(executor.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		assertThat(new DirectFieldAccessor(executor).getPropertyValue("executor"))
				.isSameAs(context.getBean("taskExecutor"));
	}

	@Test
	public void roundRobinLoadBalancerAndTaskExecutor() {
		MessageChannel channel = channels.get("roundRobinLoadBalancerAndTaskExecutor");
		assertThat(channel.getClass()).isEqualTo(ExecutorChannel.class);
		assertThat((Boolean) getDispatcherProperty("failover", channel)).isTrue();
		assertThat(getDispatcherProperty("loadBalancingStrategy", channel).getClass())
				.isEqualTo(RoundRobinLoadBalancingStrategy.class);
		Object executor = getDispatcherProperty("executor", channel);
		assertThat(executor.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		assertThat(new DirectFieldAccessor(executor).getPropertyValue("executor"))
				.isSameAs(context.getBean("taskExecutor"));
	}

	@Test
	public void loadBalancerRef() {
		MessageChannel channel = channels.get("lbRefChannel");
		LoadBalancingStrategy lbStrategy = TestUtils.getPropertyValue(channel, "dispatcher.loadBalancingStrategy",
				LoadBalancingStrategy.class);
		assertThat(lbStrategy instanceof SampleLoadBalancingStrategy).isTrue();
	}

	@Test
	public void loadBalancerRefFailWithLoadBalancer() {

		try {
			new ClassPathXmlApplicationContext("ChannelWithLoadBalancerRef-fail-config.xml", this.getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()).contains("'load-balancer' and 'load-balancer-ref' are mutually exclusive");
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
