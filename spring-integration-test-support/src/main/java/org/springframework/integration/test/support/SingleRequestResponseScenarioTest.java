/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
