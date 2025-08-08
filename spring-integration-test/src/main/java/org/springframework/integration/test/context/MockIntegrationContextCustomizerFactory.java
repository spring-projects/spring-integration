/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.test.context;

import java.util.List;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * The {@link ContextCustomizerFactory} implementation to produce a
 * {@link MockIntegrationContextCustomizer} if a {@link SpringIntegrationTest} annotation
 * is present on the test class.
 * <p>
 * Honors the {@link org.springframework.test.context.NestedTestConfiguration} semantics.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 5.0
 */
class MockIntegrationContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		return TestContextAnnotationUtils.hasAnnotation(testClass, SpringIntegrationTest.class)
				? new MockIntegrationContextCustomizer()
				: null;
	}

}
