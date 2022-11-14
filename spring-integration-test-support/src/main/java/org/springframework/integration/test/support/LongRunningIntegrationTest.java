/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.integration.test.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Rule to prevent long running tests from running on every build; set environment
 * variable RUN_LONG_INTEGRATION_TESTS on a CI nightly build to ensure coverage.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class LongRunningIntegrationTest extends TestWatcher {

	private static final Log LOGGER = LogFactory.getLog(LongRunningIntegrationTest.class);

	private static final String RUN_LONG_PROP = "RUN_LONG_INTEGRATION_TESTS";

	private boolean shouldRun = false;

	public LongRunningIntegrationTest() {
		for (String value : new String[] {System.getenv(RUN_LONG_PROP), System.getProperty(RUN_LONG_PROP)}) {
			if ("true".equalsIgnoreCase(value)) {
				this.shouldRun = true;
				break;
			}
		}
	}

	@Override
	public Statement apply(Statement base, Description description) {
		if (!this.shouldRun) {
			LOGGER.info("Skipping long running test " + description.getDisplayName());
			return new Statement() {

				@Override
				public void evaluate() {
					Assume.assumeTrue(false);
				}
			};
		}
		else {
			return super.apply(base, description);
		}
	}

}
