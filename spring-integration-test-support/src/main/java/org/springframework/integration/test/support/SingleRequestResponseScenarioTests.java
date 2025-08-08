/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.test.support;

import java.util.Collections;
import java.util.List;

/**
 * Convenience class for a single {@link RequestResponseScenario} test
 *
 * @author David Turanski
 */
public abstract class SingleRequestResponseScenarioTests extends AbstractRequestResponseScenarioTests {

	@Override
	protected List<RequestResponseScenario> defineRequestResponseScenarios() {
		return Collections.singletonList(defineRequestResponseScenario());
	}

	protected abstract RequestResponseScenario defineRequestResponseScenario();

}
