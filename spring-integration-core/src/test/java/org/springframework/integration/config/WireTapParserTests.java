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

package org.springframework.integration.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class WireTapParserTests {

	@Autowired
	MessageChannel noSelectors;

	@Autowired
	MessageChannel accepting;

	@Autowired
	MessageChannel rejecting;

	@Autowired
	MessageChannel withId;

	@Autowired
	PollableChannel wireTapChannel;

	@Autowired @Qualifier("wireTap")
	WireTap wireTap;

	@Autowired
	List<WireTap> wireTaps;

	@Test
	public void simpleWireTap() {
		assertThat(wireTapChannel.receive(0)).isNull();
		Message<?> original = new GenericMessage<>("test");
		noSelectors.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertThat(intercepted).isNotNull();
		assertThat(intercepted).isEqualTo(original);
	}

	@Test
	public void simpleWireTapWithIdAndSelectorExpression() {
		assertThat(TestUtils.<Object>getPropertyValue(wireTap, "selector"))
				.isInstanceOf(ExpressionEvaluatingSelector.class);
		Message<?> original = new GenericMessage<>("test");
		withId.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertThat(intercepted).isNotNull();
		assertThat(intercepted).isEqualTo(original);
	}

	@Test
	public void wireTapWithAcceptingSelector() {
		assertThat(wireTapChannel.receive(0)).isNull();
		Message<?> original = new GenericMessage<>("test");
		accepting.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertThat(intercepted).isNotNull();
		assertThat(intercepted).isEqualTo(original);
	}

	@Test
	public void wireTapWithRejectingSelector() {
		assertThat(wireTapChannel.receive(0)).isNull();
		Message<?> original = new GenericMessage<>("test");
		rejecting.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertThat(intercepted).isNull();
	}

	@Test
	public void wireTapTimeouts() {
		int defaultTimeoutCount = 0;
		int expectedTimeoutCount = 0;
		int otherTimeoutCount = 0;
		for (WireTap wireTap : wireTaps) {
			long timeout = TestUtils.<Long>getPropertyValue(wireTap, "timeout");
			if (timeout == 0) {
				defaultTimeoutCount++;
			}
			else if (timeout == 1234) {
				expectedTimeoutCount++;
			}
			else {
				otherTimeoutCount++;
			}
		}
		assertThat(defaultTimeoutCount).isEqualTo(4);
		assertThat(expectedTimeoutCount).isEqualTo(1);
		assertThat(otherTimeoutCount).isEqualTo(0);
	}

}
