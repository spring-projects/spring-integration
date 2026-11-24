/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Matthias Thoma
 * @author Glenn Renfro
 *
 * @since 5.5.16
 *
 */
public class Mqttv5AdapterTests {

	@Test
	public void testStop() throws Exception {
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		Mqttv5PahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, true);

		adapter.start();
		adapter.connectComplete(false, null);
		adapter.stop();

		verify(client).connect(any(MqttConnectionOptions.class));
		verify(client).subscribe(any(MqttSubscription[].class), any(), any(), any(IMqttMessageListener.class), any());
		verify(client).unsubscribe(any(String[].class));
	}

	@Test
	public void testStopNotClean() throws Exception {
		final IMqttAsyncClient client = mock(IMqttAsyncClient.class);
		Mqttv5PahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, false);

		adapter.start();
		adapter.connectComplete(false, null);
		adapter.stop();

		verify(client).connect(any(MqttConnectionOptions.class));
		verify(client).subscribe(any(MqttSubscription[].class), any(), any(), any(IMqttMessageListener.class), any());
		verify(client, never()).unsubscribe(any(String[].class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void sharedSubscriptionsPopulation() throws Exception {
		IMqttAsyncClient client = mock();
		Mqttv5PahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, false);

		adapter.addTopic("$share/group/testTopic", "$SharedSubscription/myGroup/test/#", "some_other_topic",
				"$SYS/broker/#");

		Map<String, String> topicsWithoutSharedSubscriptionPrefix =
				(Map<String, String>) TestUtils.getPropertyValue(adapter, "topicsWithoutSharedSubscriptionPrefix");

		assertThat(topicsWithoutSharedSubscriptionPrefix)
				.containsEntry("$share/group/testTopic", "testTopic")
				.containsEntry("$SharedSubscription/myGroup/test/#", "test/#")
				.containsEntry("some_other_topic", "some_other_topic")
				.containsEntry("$SYS/broker/#", "$SYS/broker/#");
	}

	@Test
	void userPropertiesAreNotMappedToHeadersByDefault() throws Exception {
		IMqttAsyncClient client = mock();
		QueueChannel outputChannel = new QueueChannel();
		Mqttv5PahoMessageDrivenChannelAdapter adapter = buildAdapterIn(client, false);
		adapter.setOutputChannel(outputChannel);

		MqttMessage mqttMessage = new MqttMessage("test payload".getBytes(StandardCharsets.UTF_8));
		MqttProperties properties = new MqttProperties();
		properties.setUserProperties(
				Collections.singletonList(new UserProperty(MessageHeaders.ERROR_CHANNEL, "fakeChannel")));
		mqttMessage.setProperties(properties);

		adapter.messageArrived("testTopic", mqttMessage);

		Message<?> message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getHeaders()).doesNotContainKey(MessageHeaders.ERROR_CHANNEL);
	}

	private static Mqttv5PahoMessageDrivenChannelAdapter buildAdapterIn(final IMqttAsyncClient client,
			boolean cleanStart) throws MqttException {

		MqttConnectionOptions connectionOptions = new MqttConnectionOptions();
		connectionOptions.setServerURIs(new String[] {"tcp://localhost:1883"});
		connectionOptions.setCleanStart(cleanStart);

		given(client.isConnected()).willReturn(true);
		IMqttToken token = mock(IMqttToken.class);
		given(client.disconnect()).willReturn(token);
		given(client.connect(any(MqttConnectionOptions.class))).willReturn(token);
		given(client.subscribe(any(MqttSubscription[].class), any(), any(), any(IMqttMessageListener.class), any()))
				.willReturn(token);
		given(client.unsubscribe(any(String[].class))).willReturn(token);
		Mqttv5PahoMessageDrivenChannelAdapter adapter =
				new Mqttv5PahoMessageDrivenChannelAdapter(connectionOptions, "client", "foo");
		ReflectionTestUtils.setField(adapter, "mqttClient", client);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.setOutputChannel(new NullChannel());
		adapter.afterPropertiesSet();
		return adapter;
	}

}
