/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.enablecomponentscan;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;
import org.springframework.integration.dsl.flows.IntegrationFlowTests;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
public class EnableComponentScanTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testCustomIntegrationComponentScan() {
		assertThat(applicationContext.getBeanNamesForType(IntegrationFlowTests.ControlBusGateway.class)).isNotEmpty();
	}

	@Configuration
	@EnableIntegration
	@Import(CustomIntegrationComponentScanRegistrar.class)
	public static class IntegrationComponentScanAutoConfiguration {

	}

	private static class CustomIntegrationComponentScanRegistrar extends IntegrationComponentScanRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {

			super.registerBeanDefinitions(
					AnnotationMetadata.introspect(IntegrationComponentScanConfiguration.class), registry);
		}

		@Override
		protected Collection<String> getBasePackages(AnnotationAttributes importingClassMetadata,
				BeanDefinitionRegistry registry) {

			return Collections.singleton(ClassUtils.getPackageName(IntegrationFlowTests.ControlBusGateway.class));
		}

		@IntegrationComponentScan
		private static class IntegrationComponentScanConfiguration {

		}

	}

}
