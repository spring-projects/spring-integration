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

package org.springframework.integration.config.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public abstract class DispatcherMaxSubscribersTests {

	@Autowired
	private MessageChannel autoCreateChannel;

	@Autowired
	private MessageChannel defaultChannel;

	@Autowired
	private MessageChannel defaultChannel2;

	@Autowired
	private MessageChannel explicitChannel;

	@Autowired
	private MessageChannel executorChannel;

	@Autowired
	private MessageChannel explicitExecutorChannel;

	@Autowired
	private MessageChannel pubSubDefaultChannel;

	@Autowired
	private MessageChannel pubSubExplicitChannel;

	public DispatcherMaxSubscribersTests() {
		super();
	}

	protected void doTestUnicast(int val1, int val2, int val3, int val4, int val5) {
		Integer autoCreateMax =
				TestUtils.getPropertyValue(autoCreateChannel, "dispatcher.maxSubscribers", Integer.class);
		assertThat(autoCreateMax).isEqualTo(val1);
		Integer defaultMax = TestUtils.getPropertyValue(defaultChannel, "dispatcher.maxSubscribers", Integer.class);
		assertThat(defaultMax.intValue()).isEqualTo(val1);
		Integer defaultMax2 = TestUtils.getPropertyValue(defaultChannel2, "dispatcher.maxSubscribers", Integer.class);
		assertThat(defaultMax2.intValue()).isEqualTo(val2);
		Integer explicitMax = TestUtils.getPropertyValue(explicitChannel, "dispatcher.maxSubscribers", Integer.class);
		assertThat(explicitMax.intValue()).isEqualTo(val3);
		Integer execMax = TestUtils.getPropertyValue(executorChannel, "dispatcher.maxSubscribers", Integer.class);
		assertThat(execMax.intValue()).isEqualTo(val4);
		Integer explicitExecMax =
				TestUtils.getPropertyValue(explicitExecutorChannel, "dispatcher.maxSubscribers", Integer.class);
		assertThat(explicitExecMax.intValue()).isEqualTo(val5);
	}

	protected void doTestMulticast(int val1, int val2) {
		Integer defaultMax = TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(pubSubDefaultChannel, "dispatcher"), "maxSubscribers", Integer.class);
		assertThat(defaultMax.intValue()).isEqualTo(val1);
		Integer explicitMax = TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(pubSubExplicitChannel, "dispatcher"), "maxSubscribers", Integer.class);
		assertThat(explicitMax.intValue()).isEqualTo(val2);
		Integer explicitMin =
				TestUtils.getPropertyValue(pubSubExplicitChannel, "dispatcher.minSubscribers", Integer.class);
		assertThat(explicitMin.intValue()).isEqualTo(1);
	}

}
