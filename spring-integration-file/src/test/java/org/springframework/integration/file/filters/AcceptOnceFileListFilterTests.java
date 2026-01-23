/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.file.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.0.4
 *
 */
public class AcceptOnceFileListFilterTests {

	@Test
	// This test used to take 34 seconds to run; now 25 milliseconds.
	public void testPerformance_INT3572() {
		StopWatch watch = new StopWatch();
		watch.start();
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<>();
		for (int i = 0; i < 100000; i++) {
			filter.accept("" + i);
		}
		watch.stop();
		assertThat(watch.getTotalTimeMillis()).isLessThan(5000);
	}

	@Test
	public void testCapacity() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<>(2);
		assertThat(filter.accept("foo")).isTrue();
		assertThat(filter.accept("bar")).isTrue();
		assertThat(filter.accept("foo")).isFalse();
		assertThat(filter.accept("baz")).isTrue();
		assertThat(filter.accept("foo")).isTrue();
		Queue<String> seen = TestUtils.getPropertyValue(filter, "seen");
		assertThat(seen.size()).isEqualTo(2);
		Set<String> seenSet = TestUtils.getPropertyValue(filter, "seenSet");
		assertThat(seenSet.size()).isEqualTo(2);
		assertThat(seen).containsExactly("baz", "foo");
		assertThat(seenSet).contains("foo", "baz");
	}

	@Test
	public void testRollback() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<>();
		doTestRollback(filter);
	}

	@Test
	public void testRollbackComposite() {
		AcceptOnceFileListFilter<String> filter = new AcceptOnceFileListFilter<>();
		CompositeFileListFilter<String> composite = new CompositeFileListFilter<>(Collections.singletonList(filter));
		doTestRollback(composite);
	}

	protected void doTestRollback(ReversibleFileListFilter<String> filter) {
		String[] files = new String[] {"foo", "bar", "baz"};
		List<String> passed = filter.filterFiles(files);
		assertThat(Arrays.equals(files, passed.toArray())).isTrue();
		List<String> now = filter.filterFiles(files);
		assertThat(now).isEmpty();
		filter.rollback(passed.get(1), passed);
		now = filter.filterFiles(files);
		assertThat(now.size()).isEqualTo(2);
		assertThat(now.get(0)).isEqualTo("bar");
		assertThat(now.get(1)).isEqualTo("baz");
		now = filter.filterFiles(files);
		assertThat(now).isEmpty();
	}

}
