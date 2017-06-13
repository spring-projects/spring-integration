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

package org.springframework.integration.test.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Similar to {@code Log4jLevelAdjuster} that can be applied as a &#064;ClassRule but you
 * will not get a log message indicating breaks between tests.
 *
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class Log4jClassLevelAdjuster extends TestWatcher {

	private static final Log logger = LogFactory.getLog(Log4jClassLevelAdjuster.class);

	private final Class<?>[] classes;

	private final Level level;

	private final String[] categories;

	public Log4jClassLevelAdjuster(Level level, Class<?>... classes) {
		this.level = level;
		this.classes = classes;
		this.categories = new String[0];
	}

	public Log4jClassLevelAdjuster(Level level, String... categories) {
		this.level = level;
		this.classes = new Class<?>[0];
		Set<String> cats = new LinkedHashSet<String>(Arrays.asList(categories));
		cats.add(getClass().getPackage().getName());
		this.categories = new ArrayList<String>(cats).toArray(new String[cats.size()]);
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				Map<Class<?>, Level> oldLevels = new HashMap<Class<?>, Level>();
				for (Class<?> cls : classes) {
					oldLevels.put(cls, LogManager.getLogger(cls).getEffectiveLevel());
					LogManager.getLogger(cls).setLevel(level);
				}
				Map<String, Level> oldCatLevels = new HashMap<String, Level>();
				for (String category : categories) {
					oldCatLevels.put(category, LogManager.getLogger(category).getEffectiveLevel());
					LogManager.getLogger(category).setLevel(level);
				}
				logger.debug("++++++++++++++++++++++++++++ "
						+ "Overridden log level setting for: " + Arrays.asList(classes) + " and "
						+ Arrays.asList(categories) + " for test " + description.getDisplayName());
				try {
					base.evaluate();
				}
				finally {
					logger.debug("++++++++++++++++++++++++++++ "
							+ "Restoring log level setting for: " + Arrays.asList(classes) + " and "
							+ Arrays.asList(categories) + " for test " + description.getDisplayName());
					// raw Class type used to avoid http://bugs.sun.com/view_bug.do?bug_id=6682380
					for (@SuppressWarnings("rawtypes") Class cls : classes) {
						LogManager.getLogger(cls).setLevel(oldLevels.get(cls));
					}
					for (String category : categories) {
						LogManager.getLogger(category).setLevel(oldCatLevels.get(category));
					}
				}
			}
		};
	}

}
