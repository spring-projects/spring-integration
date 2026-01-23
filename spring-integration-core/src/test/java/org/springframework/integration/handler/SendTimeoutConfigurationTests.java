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

package org.springframework.integration.handler;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.test.util.TestUtils;
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
public class SendTimeoutConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void serviceActivator() {
		assertThat(getTimeout("serviceActivator")).isEqualTo(123);
	}

	@Test
	public void filter() {
		assertThat(getTimeout("filter")).isEqualTo(123);
	}

	@Test
	public void transformer() {
		assertThat(getTimeout("transformer")).isEqualTo(123);
	}

	@Test
	public void splitter() {
		assertThat(getTimeout("splitter")).isEqualTo(123);
	}

	@Test
	public void router() {
		assertThat(getTimeout("router")).isEqualTo(123);
	}

	private long getTimeout(String endpointName) {
		return TestUtils.<Long>getPropertyValue(
				this.context.getBean(endpointName), "handler.messagingTemplate.sendTimeout");
	}

}
