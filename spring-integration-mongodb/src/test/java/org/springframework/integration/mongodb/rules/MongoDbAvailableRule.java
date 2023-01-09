/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.mongodb.rules;

import java.util.concurrent.TimeUnit;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;
import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * A {@link MethodRule} implementation that checks for a running MongoDB process.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public final class MongoDbAvailableRule implements MethodRule {

	@Override
	public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				MongoDbAvailable mongoAvailable = method.getAnnotation(MongoDbAvailable.class);
				if (mongoAvailable != null) {
					MongoClientSettings settings = MongoClientSettings.builder()
							.applyToSocketSettings(ss -> ss.connectTimeout(100, TimeUnit.MILLISECONDS))
							.applyToClusterSettings(cs -> cs.serverSelectionTimeout(100, TimeUnit.MILLISECONDS))
							.build();
					try (MongoClient mongo = new MongoClientImpl(settings, null)) {
						mongo.listDatabaseNames()
								.first();
					}
					catch (Exception e) {
						Assume.assumeTrue(
								"Skipping test due to MongoDb not being available on hosts: " +
										settings.getClusterSettings().getHosts() + ":\n" + e,
								false);
					}
				}
				base.evaluate();
			}
		};
	}

}
