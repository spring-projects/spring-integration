/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.zeromq.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ZeromqMessageDrivenChannelAdapterParserTests {

	@Autowired
	private org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter noTopicsAdapter;

	@Autowired
	private org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter noTopicsAdapterDefaultCF;

	@Autowired
	private org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter oneTopicAdapter;

	@Autowired
	private MessageChannel out;

	@Autowired
	private org.springframework.integration.zeromq.support.ZeromqMessageConverter converter;

	@Autowired
	private org.springframework.integration.zeromq.core.DefaultZeromqClientFactory clientFactory;

	@Autowired
	private MessageChannel errors;

	@Test
	public void testNoTopics() {
		assertThat("tcp://localhost:5559").isEqualTo(TestUtils.getPropertyValue(noTopicsAdapter, "url"));
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat("clientId1").isEqualTo(TestUtils.getPropertyValue(noTopicsAdapter, "clientId"));
		assertThat(out).isEqualTo(TestUtils.getPropertyValue(noTopicsAdapter, "outputChannel"));
		assertThat(clientFactory).isEqualTo(TestUtils.getPropertyValue(noTopicsAdapter, "clientFactory"));
		assertThat(5000).isEqualTo(TestUtils.getPropertyValue(this.noTopicsAdapter, "recoveryInterval"));
	}

	@Test
	public void testNoTopicsDefaultCF() {
		assertThat("tcp://localhost:5559").isEqualTo(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "url"));
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "autoStartup", Boolean.class)).isFalse();
		assertThat("clientId2").isEqualTo(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "clientId"));
		assertThat(out).isEqualTo(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "outputChannel"));
	}

	@Test
	public void testOneTopic() {
		assertThat("tcp://localhost:5559").isEqualTo(TestUtils.getPropertyValue(oneTopicAdapter, "url"));
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat("clientId4").isEqualTo(TestUtils.getPropertyValue(oneTopicAdapter, "clientId"));
		assertThat(converter).isEqualTo(TestUtils.getPropertyValue(oneTopicAdapter, "converter"));
		assertThat(out).isEqualTo(TestUtils.getPropertyValue(oneTopicAdapter, "outputChannel"));
		assertThat(clientFactory).isEqualTo(TestUtils.getPropertyValue(oneTopicAdapter, "clientFactory"));
		assertThat(errors).isEqualTo(TestUtils.getPropertyValue(oneTopicAdapter, "errorChannel"));
	}
}
