/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationContextTests {

	@Autowired
	@Qualifier(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
	private IntegrationProperties integrationProperties;

	@Autowired
	@Qualifier("fooService")
	private AbstractEndpoint serviceActivator;

	@Autowired
	@Qualifier("fooServiceExplicit")
	private AbstractEndpoint serviceActivatorExplicit;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Test
	public void testIntegrationContextComponents() {
		assertThat(this.integrationProperties.isMessagingTemplateThrowExceptionOnLateReply()).isTrue();
		assertThat(this.integrationProperties.getTaskSchedulerPoolSize()).isEqualTo(20);
		assertThat(this.serviceActivator.getIntegrationProperties()).isSameAs(this.integrationProperties);
		assertThat(TestUtils.<Integer>getPropertyValue(this.taskScheduler, "poolSize"))
				.isEqualTo(20);
		assertThat(this.serviceActivator.isAutoStartup()).isFalse();
		assertThat(this.serviceActivator.isRunning()).isFalse();
		assertThat(this.serviceActivatorExplicit.isAutoStartup()).isTrue();
		assertThat(this.serviceActivatorExplicit.isRunning()).isTrue();
	}

}
