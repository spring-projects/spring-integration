/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.amqp.channel;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.amqp.config.AmqpChannelFactoryBean;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0.8
 *
 */
public class ChannelTests {


	@Test
	public void testAmqpChannelFactoryBean() throws Exception {
		ConnectionFactory factory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		when(factory.createConnection()).thenReturn(connection);
		Channel mockChannel = mock(Channel.class);
		when(connection.createChannel(false)).thenReturn(mockChannel);
		when(mockChannel.queueDeclare()).thenReturn(new AMQImpl.Queue.DeclareOk("foo", 0, 0));

		AmqpChannelFactoryBean channelFactoryBean = new AmqpChannelFactoryBean();
		channelFactoryBean.setBeanFactory(mock(BeanFactory.class));
		channelFactoryBean.setConnectionFactory(factory);
		channelFactoryBean.setBeanName("testChannel");
		channelFactoryBean.afterPropertiesSet();
		AbstractAmqpChannel channel = channelFactoryBean.getObject();
		assertThat(channel, instanceOf(PointToPointSubscribableAmqpChannel.class));

		channelFactoryBean = new AmqpChannelFactoryBean();
		channelFactoryBean.setBeanFactory(mock(BeanFactory.class));
		channelFactoryBean.setConnectionFactory(factory);
		channelFactoryBean.setBeanName("testChannel");
		channelFactoryBean.setPubSub(true);
		channelFactoryBean.afterPropertiesSet();
		channel = channelFactoryBean.getObject();
		assertThat(channel, instanceOf(PublishSubscribeAmqpChannel.class));
	}

}
