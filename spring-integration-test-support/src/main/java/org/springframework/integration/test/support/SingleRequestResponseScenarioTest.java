/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.support;

import java.util.Collections;
import java.util.List;

/**
 * Convenience class for a single {@link RequestResponseScenario} test
 *
 * @author David Turanski
 * @author Jiandong Ma
 *
 * @since 7.0
 */
public abstract class SingleRequestResponseScenarioTest extends AbstractRequestResponseScenarioTest {

	@Override
	protected List<RequestResponseScenario> defineRequestResponseScenarios() {
		return Collections.singletonList(defineRequestResponseScenario());
	}

	protected abstract RequestResponseScenario defineRequestResponseScenario();

}
