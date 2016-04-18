/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.test.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Base class for module tests where logging is set to TRACE for the duration
 * of the test and reverted to the previous value. Also logs a start/end
 * message. Duplicated in s-i-core/src/test for use there, to avoid circular dep.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2.2
 *
 */
public class LogAdjustingTestSupport {

	/*
	 * If you make changes here, consider doing the same in the core version.
	 */

	@Rule
	public TestName testName = new TestName();

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Collection<Logger> loggersToAdjust = new ArrayList<Logger>();

	private final Collection<Level> oldCategories = new ArrayList<Level>();


	public LogAdjustingTestSupport() {
		this("org.springframework.integration");
	}

	public LogAdjustingTestSupport(String... loggersToAdjust) {
		for (String loggerToAdjust : loggersToAdjust) {
			this.loggersToAdjust.add(LogManager.getLogger(loggerToAdjust));
		}
	}

	@Before
	public void beforeTest() {
		for (Logger loggerToAdjust : this.loggersToAdjust) {
			this.oldCategories.add(loggerToAdjust.getEffectiveLevel());
			loggerToAdjust.setLevel(Level.TRACE);
		}
		this.logger.warn("!!!! Starting test: " + this.testName.getMethodName() + " !!!!");
	}

	@After
	public void afterTest() {
		logger.warn("!!!! Finished test: " + this.testName.getMethodName() + " !!!!");
		Iterator<Level> oldCategory = this.oldCategories.iterator();
		for (Logger loggerToAdjust : this.loggersToAdjust) {
			loggerToAdjust.setLevel(oldCategory.next());
		}
	}

}
