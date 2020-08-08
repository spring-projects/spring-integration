/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.stomp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.integration.stomp.inbound.StompInboundChannelAdapter;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;

/**
 * @author Artem Bilan
 *
 * @since 4.2
 */
@SpringJUnitConfig
@DirtiesContext
public class StompAdaptersParserTests {

	@Autowired
	private StompSessionManager stompSessionManager;

	@Autowired
	private HeaderMapper<?> headerMapper;

	@Autowired
	@Qualifier("defaultInboundAdapter")
	private MessageChannel defaultInboundAdapterChannel;

	@Autowired
	private MessageChannel errorChannel;

	@Autowired
	private MessageChannel inboundChannel;

	@Autowired
	@Qualifier("defaultInboundAdapter.adapter")
	private StompInboundChannelAdapter defaultInboundAdapter;

	@Autowired
	private StompInboundChannelAdapter customInboundAdapter;

	@Autowired
	@Qualifier("defaultOutboundAdapter")
	private MessageChannel defaultOutboundAdapterChannel;

	@Autowired
	@Qualifier("defaultOutboundAdapter.handler")
	private MessageHandler defaultOutboundAdapterHandler;

	@Autowired
	@Qualifier("defaultOutboundAdapter.adapter")
	private AbstractEndpoint defaultOutboundAdapter;

	@Autowired
	private MessageChannel outboundChannel;

	@Autowired
	@Qualifier("customOutboundAdapter.handler")
	private MessageHandler customOutboundAdapterHandler;

	@Autowired
	@Qualifier("customOutboundAdapter")
	private AbstractEndpoint customOutboundAdapter;

	@Autowired
	private SmartLifecycleRoleController roleController;

	@Test
	public void testParsers() {
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "outputChannel"))
				.isSameAs(this.defaultInboundAdapterChannel);
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "stompSessionManager"))
				.isSameAs(this.stompSessionManager);
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "errorChannel")).isNull();
		Object headerMapper = TestUtils.getPropertyValue(this.defaultInboundAdapter, "headerMapper");
		assertThat(headerMapper).isNotNull();
		assertThat(headerMapper).isNotSameAs(this.headerMapper);
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "payloadType", Class.class))
				.isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.defaultInboundAdapter, "autoStartup", Boolean.class)).isTrue();

		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "outputChannel"))
				.isSameAs(this.inboundChannel);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "stompSessionManager"))
				.isSameAs(this.stompSessionManager);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "errorChannel")).isSameAs(this.errorChannel);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "destinations"))
				.isEqualTo(Collections.singleton("foo"));
		headerMapper = TestUtils.getPropertyValue(this.customInboundAdapter, "headerMapper");
		assertThat(headerMapper).isNotNull();
		assertThat(headerMapper).isNotSameAs(this.headerMapper);
		assertThat(TestUtils.getPropertyValue(headerMapper, "inboundHeaderNames", String[].class))
				.isEqualTo(new String[]{"bar", "foo"});
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "payloadType", Class.class))
				.isEqualTo(Integer.class);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "phase")).isEqualTo(200);
		assertThat(TestUtils.getPropertyValue(this.customInboundAdapter, "messagingTemplate.sendTimeout"))
				.isEqualTo(2000L);

		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapterHandler, "stompSessionManager"))
				.isSameAs(this.stompSessionManager);
		headerMapper = TestUtils.getPropertyValue(this.defaultOutboundAdapterHandler, "headerMapper");
		assertThat(headerMapper).isNotNull();
		assertThat(headerMapper).isNotSameAs(this.headerMapper);
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapterHandler, "destinationExpression")).isNull();
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "handler"))
				.isSameAs(this.defaultOutboundAdapterHandler);
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "inputChannel"))
				.isSameAs(this.defaultOutboundAdapterChannel);
		assertThat(TestUtils.getPropertyValue(this.defaultOutboundAdapter, "autoStartup", Boolean.class)).isTrue();

		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapterHandler, "stompSessionManager"))
				.isSameAs(this.stompSessionManager);
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapterHandler, "headerMapper"))
				.isSameAs(this.headerMapper);
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapterHandler, "destinationExpression.literalValue"))
				.isEqualTo("baz");
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "handler"))
				.isSameAs(this.customOutboundAdapterHandler);
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "inputChannel"))
				.isSameAs(this.outboundChannel);
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.customOutboundAdapter, "phase")).isEqualTo(100);

		@SuppressWarnings("unchecked")
		MultiValueMap<String, SmartLifecycle> lifecycles = (MultiValueMap<String, SmartLifecycle>)
				TestUtils.getPropertyValue(this.roleController, "lifecycles", MultiValueMap.class);
		assertThat(lifecycles.containsKey("bar")).isTrue();
		List<SmartLifecycle> bars = lifecycles.get("bar");
		bars.contains(this.customInboundAdapter);
		assertThat(lifecycles.containsKey("foo")).isTrue();
		bars.contains(this.customOutboundAdapter);
	}

}
