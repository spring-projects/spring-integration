/*
 * Copyright 2002-2008 the original author or authors.
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

import org.junit.Test;
import org.springframework.integration.support.ComponentMetadata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
		
		executor.execute(new Runnable() {
			public void run() {
				for (int i = 0; i < times; i++) {
					ComponentMetadata event = new ComponentMetadata();
					event.setAttribute("foo", "foo");
					event.setAttribute("bar", "bar");
					event.setComponentName("MessageHistoryTests");
					history.addEvent(event);
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
