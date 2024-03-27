/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.mqtt;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.test.util.ReflectionTestUtils;

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
		verify(client).subscribe(any(MqttSubscription[].class), any(), any(), any(IMqttMessageListener[].class), any());
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
		verify(client).subscribe(any(MqttSubscription[].class), any(), any(), any(IMqttMessageListener[].class), any());
		verify(client, never()).unsubscribe(any(String[].class));
	}

	private static Mqttv5PahoMessageDrivenChannelAdapter buildAdapterIn(final IMqttAsyncClient client, boolean cleanStart) throws MqttException {

		MqttConnectionOptions connectionOptions = new MqttConnectionOptions();
		connectionOptions.setServerURIs(new String[] {"tcp://localhost:1883"});
		connectionOptions.setCleanStart(cleanStart);

		given(client.isConnected()).willReturn(true);
		IMqttToken token = mock(IMqttToken.class);
		given(client.disconnect()).willReturn(token);
		given(client.connect(any(MqttConnectionOptions.class))).willReturn(token);
		given(client.subscribe(any(MqttSubscription[].class), any(), any(), any(IMqttMessageListener[].class), any())).willReturn(token);
		given(client.unsubscribe(any(String[].class))).willReturn(token);
		Mqttv5PahoMessageDrivenChannelAdapter adapter = new Mqttv5PahoMessageDrivenChannelAdapter(connectionOptions, "client", "foo");
		ReflectionTestUtils.setField(adapter, "mqttClient", client);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		adapter.setOutputChannel(new NullChannel());
		adapter.afterPropertiesSet();
		return adapter;
	}

}
