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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class RouterFactoryBeanDelegationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("strings")
	private PollableChannel strings;

	@Autowired
	@Qualifier("discard")
	private PollableChannel discard;

	@Autowired
	@Qualifier("org.springframework.integration.config.RouterFactoryBean#0")
	private AbstractMappingMessageRouter router;

	@Test
	public void checkResolutionRequiredConfiguredOnTargetRouter() {
		boolean resolutionRequired = TestUtils.<Boolean>getPropertyValue(router, "resolutionRequired");
		assertThat(resolutionRequired).as("The 'resolutionRequired' property should be 'true'").isTrue();
	}

	@Test
	public void routeWithMappedType() {
		input.send(new GenericMessage<>("test"));
		assertThat(discard.receive(0)).isNull();
		assertThat(strings.receive(0)).isNotNull();
	}

	@Test
	public void routeWithUnmappedType() {
		input.send(new GenericMessage<>(123));
		assertThat(strings.receive(0)).isNull();
		assertThat(discard.receive(0)).isNotNull();
	}

}
