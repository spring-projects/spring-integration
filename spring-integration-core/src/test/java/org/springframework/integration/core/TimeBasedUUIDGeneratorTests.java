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
package org.springframework.integration.core;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 *
 */
public class TimeBasedUUIDGeneratorTests {

	@Test
	public void testGreaterThen(){
		UUID id = TimeBasedUUIDGenerator.generateId();
		for (int i = 0; i < 1000; i++) {
			UUID newId = TimeBasedUUIDGenerator.generateId();
			// tests, that newly created UUID is always greater then the previous one.
			Assert.assertTrue(newId.compareTo(id) == 1);
			id = newId;
		}
	}
	@Test
	public void testUniqueness(){
		long timestamp = System.currentTimeMillis();
		UUID id = TimeBasedUUIDGenerator.generateIdFromTimestamp(timestamp);
		for (int i = 0; i < 1000; i++) {
			UUID newId = TimeBasedUUIDGenerator.generateIdFromTimestamp(timestamp);
			// tests, that newly created UUID is always greater then the previous one.
			Assert.assertTrue(newId.compareTo(id) == 1);
			id = newId;
		}
	}
	@Test
	public void performanceTestSynch(){
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			TimeBasedUUIDGenerator.generateId();
		}
		stopWatch.stop();
		System.out.println("Generated 10000 UUID (sync) via TimeBasedUUIDGenerator.generateId(): in " + stopWatch.getTotalTimeSeconds() + " seconds");
		
		stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			UUID.randomUUID();
		}
		stopWatch.stop();
		System.out.println("Generated 10000 UUID  (sync) via UUID.randomUUID(): in " + stopWatch.getTotalTimeSeconds() + " seconds");
	}
	@Test
	public void performanceTestAsynch() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			executor.execute(new Runnable() {
				public void run() {
					TimeBasedUUIDGenerator.generateId();
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		stopWatch.stop();
		System.out.println("Generated 10000 UUID (async) via TimeBasedUUIDGenerator.generateId(): in " + stopWatch.getTotalTimeSeconds() + " seconds");
		
		executor = Executors.newFixedThreadPool(1);
		stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			executor.execute(new Runnable() {
				public void run() {
					UUID.randomUUID();
				}
			});
		}
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
		stopWatch.stop();
		System.out.println("Generated 10000 UUID (async) via UUID.randomUUID(): in " + stopWatch.getTotalTimeSeconds() + " seconds");
	}
}
