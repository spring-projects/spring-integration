/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.integration.test.mock;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.0.9
 */
@SpringJUnitConfig
@SpringIntegrationTest
public abstract class CachedSpringIntegrationTestAnnotationTests {

	private static int beanFactoryPostProcessorCallCounter;

	@Autowired
	protected AbstractEndpoint someEndpoint;

	public static class SpringIntegrationTestAnnotationCaching1Tests
			extends CachedSpringIntegrationTestAnnotationTests {

		@Test
		public void testSingleApplicationContext() {
			assertThat(beanFactoryPostProcessorCallCounter).isEqualTo(1);
			assertThat(this.someEndpoint.isRunning()).isTrue();
		}

	}

	@SpringIntegrationTest(noAutoStartup = "someEndpoint")
	public static class SpringIntegrationTestAnnotationCaching2Tests
			extends CachedSpringIntegrationTestAnnotationTests {

		@Test
		public void testSingleApplicationContext() {
			assertThat(beanFactoryPostProcessorCallCounter).isEqualTo(1);
			assertThat(this.someEndpoint.isRunning()).isFalse();
		}

	}

	@Configuration
	public static class ContextConfiguration {

		@Bean
		public static BeanFactoryPostProcessor tesBeanFactoryPostProcessor() {
			return beanFactory -> beanFactoryPostProcessorCallCounter++;
		}

		@Bean
		public AbstractEndpoint someEndpoint() {
			return new AbstractEndpoint() {

				@Override
				protected void doStart() {

				}

				@Override
				protected void doStop() {

				}

			};
		}

	}

}
