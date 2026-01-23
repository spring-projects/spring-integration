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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.router.RecipientListRouter.Recipient;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class RecipientListRouterTests {

	@Test
	@SuppressWarnings("unchecked")
	public void channelConfig() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channel1);
		channels.add(channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(channels);
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		ConcurrentLinkedQueue<Recipient> recipients = (ConcurrentLinkedQueue<Recipient>)
				new DirectFieldAccessor(router).getPropertyValue("recipients");
		assertThat(recipients.size()).isEqualTo(2);
		assertThat(new DirectFieldAccessor(recipients.poll()).getPropertyValue("channel")).isEqualTo(channel1);
		assertThat(new DirectFieldAccessor(recipients.poll()).getPropertyValue("channel")).isEqualTo(channel2);
	}

	@Test
	public void routeWithChannelList() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channel1);
		channels.add(channel2);
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(channels);
		router.setBeanFactory(mock(BeanFactory.class));
		router.afterPropertiesSet();
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1 = channel1.receive(25);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test");
		Message<?> result2 = channel2.receive(25);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("test");
	}

	@Test
	public void routeToSingleChannel() {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("channel");
		RecipientListRouter router = new RecipientListRouter();
		router.setChannels(Collections.singletonList((MessageChannel) channel));
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1 = channel.receive(25);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test");
		Message<?> result2 = channel.receive(5);
		assertThat(result2).isNull();
	}

	@Test
	public void sendFailureOnFirstRecipientTriggersExceptionByDefault() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		QueueChannel channelC = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		channelC.setBeanName("channelC");
		RecipientListRouter router = new RecipientListRouter();
		router.setSendTimeout(0);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		channels.add(channelC);
		router.setChannels(channels);
		channelA.send(new GenericMessage<String>("blocker"));
		Message<String> message = new GenericMessage<String>("test");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> router.handleMessage(message));
		Message<?> result1a = channelA.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("blocker");
		assertThat(channelB.receive(0)).isNull();
		assertThat(channelC.receive(0)).isNull();
	}

	@Test
	public void sendFailureOnMiddleRecipientTriggersExceptionByDefault() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		QueueChannel channelC = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		channelC.setBeanName("channelC");
		RecipientListRouter router = new RecipientListRouter();
		router.setSendTimeout(0);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		channels.add(channelC);
		router.setChannels(channels);
		channelB.send(new GenericMessage<String>("blocker"));
		Message<String> message = new GenericMessage<String>("test");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> router.handleMessage(message));
		Message<?> result1a = channelA.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("test");
		Message<?> result1b = channelB.receive(0);
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("blocker");
		assertThat(channelC.receive(0)).isNull();
	}

	@Test
	public void sendFailureOnLastRecipientTriggersExceptionByDefault() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		QueueChannel channelC = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		channelC.setBeanName("channelC");
		RecipientListRouter router = new RecipientListRouter();
		router.setSendTimeout(0);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		channels.add(channelC);
		router.setChannels(channels);
		channelC.send(new GenericMessage<String>("blocker"));
		Message<String> message = new GenericMessage<String>("test");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> router.handleMessage(message));
		Message<?> result1a = channelA.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("test");
		Message<?> result1b = channelB.receive(0);
		assertThat(result1b).isNotNull();
		assertThat(result1b.getPayload()).isEqualTo("test");
		Message<?> result1c = channelC.receive(0);
		assertThat(result1c).isNotNull();
		assertThat(result1c.getPayload()).isEqualTo("blocker");
	}

	@Test
	public void sendFailureOnFirstRecipientIgnored() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		QueueChannel channelC = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		channelC.setBeanName("channelC");
		RecipientListRouter router = new RecipientListRouter();
		router.setIgnoreSendFailures(true);
		router.setSendTimeout(0);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		channels.add(channelC);
		router.setChannels(channels);
		channelA.send(new GenericMessage<String>("blocker"));
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1a = channelA.receive(0);
		Message<?> result1b = channelB.receive(0);
		Message<?> result1c = channelC.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1b).isNotNull();
		assertThat(result1c).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("blocker");
		assertThat(result1b.getPayload()).isEqualTo("test");
		assertThat(result1c.getPayload()).isEqualTo("test");
	}

	@Test
	public void sendFailureOnMiddleRecipientIgnored() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		QueueChannel channelC = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		channelC.setBeanName("channelC");
		RecipientListRouter router = new RecipientListRouter();
		router.setIgnoreSendFailures(true);
		router.setSendTimeout(0);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		channels.add(channelC);
		router.setChannels(channels);
		channelB.send(new GenericMessage<String>("blocker"));
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1a = channelA.receive(0);
		Message<?> result1b = channelB.receive(0);
		Message<?> result1c = channelC.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1b).isNotNull();
		assertThat(result1c).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("test");
		assertThat(result1b.getPayload()).isEqualTo("blocker");
		assertThat(result1c.getPayload()).isEqualTo("test");
	}

	@Test
	public void sendFailureOnLastRecipientIgnored() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		QueueChannel channelC = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		channelC.setBeanName("channelC");
		RecipientListRouter router = new RecipientListRouter();
		router.setIgnoreSendFailures(true);
		router.setSendTimeout(0);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		channels.add(channelC);
		router.setChannels(channels);
		channelC.send(new GenericMessage<String>("blocker"));
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1a = channelA.receive(0);
		Message<?> result1b = channelB.receive(0);
		Message<?> result1c = channelC.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1b).isNotNull();
		assertThat(result1c).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("test");
		assertThat(result1b.getPayload()).isEqualTo("test");
		assertThat(result1c.getPayload()).isEqualTo("blocker");
	}

	@Test
	public void applySequenceNotEnabledByDefault() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		RecipientListRouter router = new RecipientListRouter();
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		router.setChannels(channels);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1a = channelA.receive(0);
		Message<?> result1b = channelB.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1b).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("test");
		assertThat(new IntegrationMessageHeaderAccessor(result1a).getSequenceNumber()).isEqualTo(0);
		assertThat(new IntegrationMessageHeaderAccessor(result1a).getSequenceSize()).isEqualTo(0);
		assertThat(new IntegrationMessageHeaderAccessor(result1a).getCorrelationId()).isNull();
		assertThat(result1b.getPayload()).isEqualTo("test");
		assertThat(new IntegrationMessageHeaderAccessor(result1b).getSequenceNumber()).isEqualTo(0);
		assertThat(new IntegrationMessageHeaderAccessor(result1b).getSequenceSize()).isEqualTo(0);
		assertThat(new IntegrationMessageHeaderAccessor(result1b).getCorrelationId()).isNull();
	}

	@Test
	public void applySequenceEnabled() {
		QueueChannel channelA = new QueueChannel(1);
		QueueChannel channelB = new QueueChannel(1);
		channelA.setBeanName("channelA");
		channelB.setBeanName("channelB");
		RecipientListRouter router = new RecipientListRouter();
		router.setApplySequence(true);
		List<MessageChannel> channels = new ArrayList<MessageChannel>();
		channels.add(channelA);
		channels.add(channelB);
		router.setChannels(channels);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1a = channelA.receive(0);
		Message<?> result1b = channelB.receive(0);
		assertThat(result1a).isNotNull();
		assertThat(result1b).isNotNull();
		assertThat(result1a.getPayload()).isEqualTo("test");
		assertThat(new IntegrationMessageHeaderAccessor(result1a).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(result1a).getSequenceSize()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(result1a).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
		assertThat(result1b.getPayload()).isEqualTo("test");
		assertThat(new IntegrationMessageHeaderAccessor(result1b).getSequenceNumber()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(result1b).getSequenceSize()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(result1b).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void nullChannelListRejected() {
		RecipientListRouter router = new RecipientListRouter();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> router.setChannels(null));
	}

	@Test
	public void emptyChannelListRejected() {
		RecipientListRouter router = new RecipientListRouter();
		List<MessageChannel> channels = new ArrayList<>();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> router.setChannels(channels));
	}

	@Test
	public void noChannelListPassInitialization() {
		RecipientListRouter router = new RecipientListRouter();
		router.setBeanFactory(mock(BeanFactory.class));
		QueueChannel defaultOutputChannel = new QueueChannel();
		router.setDefaultOutputChannel(defaultOutputChannel);
		router.afterPropertiesSet();
		router.handleMessage(new GenericMessage<String>("foo"));
		Message<?> receive = defaultOutputChannel.receive(1000);
		assertThat(receive).isNotNull();
	}

	@Test
	public void recipientsWithSelectors() {
		QueueChannel channel1 = new QueueChannel();
		QueueChannel channel2 = new QueueChannel();
		QueueChannel channel3 = new QueueChannel();
		QueueChannel channel4 = new QueueChannel();
		QueueChannel channel5 = new QueueChannel();
		List<Recipient> recipients = new ArrayList<Recipient>();
		recipients.add(new Recipient(channel1, new AlwaysTrueSelector()));
		recipients.add(new Recipient(channel2, new AlwaysFalseSelector()));
		recipients.add(new Recipient(channel3));
		recipients.add(new Recipient(channel4));
		recipients.add(new Recipient(channel5, new AlwaysFalseSelector()));
		RecipientListRouter router = new RecipientListRouter();
		router.setRecipients(recipients);
		Message<?> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> reply1 = channel1.receive(0);
		assertThat(reply1).isEqualTo(message);
		Message<?> reply2 = channel2.receive(0);
		assertThat(reply2).isNull();
		Message<?> reply3 = channel3.receive(0);
		assertThat(reply3).isEqualTo(message);
		Message<?> reply4 = channel4.receive(0);
		assertThat(reply4).isEqualTo(message);
		Message<?> reply5 = channel5.receive(0);
		assertThat(reply5).isNull();
	}

	@Test
	public void routeToDefaultChannelNoSelectorHits() {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("channel");
		QueueChannel defaultChannel = new QueueChannel();
		channel.setBeanName("default");
		List<Recipient> recipients = new ArrayList<Recipient>();
		recipients.add(new Recipient(channel, new AlwaysFalseSelector()));
		RecipientListRouter router = new RecipientListRouter();
		router.setRecipients(recipients);
		router.setDefaultOutputChannel(defaultChannel);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
		Message<?> result1 = defaultChannel.receive(25);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test");
		Message<?> result2 = channel.receive(5);
		assertThat(result2).isNull();
	}

	@Test
	public void testDefaultChannelResolutionFromName() {
		QueueChannel defaultChannel = new QueueChannel();
		List<Recipient> recipients = new ArrayList<Recipient>();
		recipients.add(new Recipient(new DirectChannel(), new AlwaysFalseSelector()));
		RecipientListRouter router = new RecipientListRouter();
		router.setRecipients(recipients);
		router.setDefaultOutputChannelName("defaultChannel");

		BeanFactory beanFactory = Mockito.mock(BeanFactory.class);
		when(beanFactory.getBean(Mockito.eq("defaultChannel"), Mockito.eq(MessageChannel.class)))
				.thenReturn(defaultChannel);
		router.setBeanFactory(beanFactory);
		router.afterPropertiesSet();

		router.handleMessage(new GenericMessage<String>("foo"));

		assertThat(TestUtils.<Object>getPropertyValue(router, "defaultOutputChannel"))
				.isSameAs(defaultChannel);
		Mockito.verify(beanFactory).getBean(Mockito.eq("defaultChannel"), Mockito.eq(MessageChannel.class));
	}

	private static class AlwaysTrueSelector implements MessageSelector {

		AlwaysTrueSelector() {
			super();
		}

		@Override
		public boolean accept(Message<?> message) {
			return true;
		}

	}

	private static class AlwaysFalseSelector implements MessageSelector {

		AlwaysFalseSelector() {
			super();
		}

		@Override
		public boolean accept(Message<?> message) {
			return false;
		}

	}

}
