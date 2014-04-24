/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.mongodb.rules;

import com.mongodb.MongoClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import com.mongodb.Mongo;

/**
 * A {@link MethodRule} implementation that checks for a running MongoDB process.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1
 */
public final class MongoDbAvailableRule implements MethodRule {

	private final Log logger = LogFactory.getLog(this.getClass());

	public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				MongoDbAvailable mongoAvailable = method.getAnnotation(MongoDbAvailable.class);
				if (mongoAvailable != null) {
					try {
						Mongo mongo = new MongoClient();
						mongo.getDatabaseNames();
					}
					catch (Exception e) {
						logger.warn("MongoDb is not available. Skipping the test: " +
								target.getClass().getSimpleName() + "." + method.getName() + "()");
						return;
					}
				}
				base.evaluate();
			}
		};
	}

}
