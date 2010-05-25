/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.history;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.context.NamedComponent;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MessageHistoryTests {

	private long times = 1000;

	private ExecutorService executor = Executors.newCachedThreadPool();

	@Test
	public void testConcurrentModificationsOnObjectMethods() throws Exception{
		final MessageHistory history = new MessageHistory();
		final MessageHistory otherHistory = new MessageHistory();
		final NamedComponent component = new NamedComponent() {
			public String getComponentType() {
				return "testType";
			}
			public String getComponentName() {
				return "testName";
			}
		};
		executor.execute(new Runnable() {
			public void run() {
				for (int i = 0; i < times; i++) {
					history.addEvent(component);
                    otherHistory.addEvent(component);
				}
			}
		});
		executor.execute(new Runnable() {
			public void run() {
				for (int i = 0; i < times; i++) {
					history.toString();
				}
			}
		});
		executor.execute(new Runnable() {
			public void run() {
				for (int i = 0; i < times; i++) {
					history.hashCode();
				}
			}
		});
		executor.execute(new Runnable() {
			public void run() {
				for (int i = 0; i < times; i++) {
					history.equals(new Object());
				}
			}
		});
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.SECONDS);
	}

}
