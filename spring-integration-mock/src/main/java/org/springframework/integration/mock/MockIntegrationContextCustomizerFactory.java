/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mock;

import java.util.List;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * The {@link ContextCustomizerFactory} implementation to produce
 * {@link MockIntegrationContextCustomizer} if {@link SpringIntegrationTest} annotation
 * is present on the test class.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class MockIntegrationContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		SpringIntegrationTest springIntegrationTest =
				AnnotatedElementUtils.findMergedAnnotation(testClass, SpringIntegrationTest.class);
		return springIntegrationTest != null
				?
				new MockIntegrationContextCustomizer(springIntegrationTest)
				: null;
	}

}
