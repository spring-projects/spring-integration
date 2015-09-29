/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.file.filters;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.StopWatch;

/**
 * @author Gary Russell
 * @since 4.0.4
 *
 */
public class AcceptOnceFileListFilterTests {

	@Test
	// This test used to take 34 seconds to run; now 25 milliseconds.
	public void testPerformance_INT3572() {
		StopWatch watch = new StopWatch();
		watch.start();
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<String>();
		for (int i = 0; i < 100000; i++) {
			filter.accept("" + i);
		}
		watch.stop();
		assertTrue(watch.getTotalTimeMillis() < 5000);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCapacity() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<String>(2);
		assertTrue(filter.accept("foo"));
		assertTrue(filter.accept("bar"));
		assertFalse(filter.accept("foo"));
		assertTrue(filter.accept("baz"));
		assertTrue(filter.accept("foo"));
		Queue<String> seen = TestUtils.getPropertyValue(filter, "seen", Queue.class);
		assertEquals(2, seen.size());
		Set<String> seenSet = TestUtils.getPropertyValue(filter, "seenSet", Set.class);
		assertEquals(2, seenSet.size());
		assertThat(seen, contains("baz", "foo"));
		assertThat(seenSet, containsInAnyOrder("foo", "baz"));
	}

	@Test
	public void testRollback() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<String>();
		doTestRollback(filter);
	}

	@Test
	public void testRollbackComposite() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<String>();
		CompositeFileListFilter<String> composite = new CompositeFileListFilter<String>(
				Collections.singletonList(filter));
		doTestRollback(composite);
	}

	protected void doTestRollback(ReversibleFileListFilter<String> filter) {
		String[] files = new String[] {"foo", "bar", "baz"};
		List<String> passed = filter.filterFiles(files);
		assertTrue(Arrays.equals(files, passed.toArray()));
		List<String> now = filter.filterFiles(files);
		assertEquals(0, now.size());
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertEquals(2, now.size());
		assertEquals("bar", now.get(0));
		assertEquals("baz", now.get(1));
		now = filter.filterFiles(files);
		assertEquals(0, now.size());
	}

}
