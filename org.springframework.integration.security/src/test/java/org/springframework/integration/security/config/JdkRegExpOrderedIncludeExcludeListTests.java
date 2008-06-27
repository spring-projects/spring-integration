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
package org.springframework.integration.security.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class JdkRegExpOrderedIncludeExcludeListTests {

	@Test
	public void testSimpleInclusionIncludeByDefaultFalse() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] { true }, new String[] { ".*" });
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(false, patterns);
		assertTrue("Did not match expected name", matcher.isIncluded("anyoldthing"));

	}

	@Test
	public void testNoPatternsIncludeByDefaultTrue() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] {}, new String[] {});
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(true, patterns);
		assertTrue("Did not match expected name", matcher.isIncluded("anyoldthing"));
	}

	@Test
	public void testNoPatternsIncludeByDefaultFalse() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] {}, new String[] {});
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(false, patterns);
		assertFalse("Unexpected match when match by default false and no patterns", matcher.isIncluded("anyoldthing"));
	}

	@Test
	public void testExcludeThenIncludeWithIncludeByDefaultFalse() {
		List<IncludeExcludePattern> patterns = createIncludeExcludeList(new boolean[] { false, true }, new String[] {
				"admin.*", ".*" });
		JdkRegExpOrderedIncludeExcludeList matcher = new JdkRegExpOrderedIncludeExcludeList(false, patterns);
		assertFalse("Unexpected match when match by default false and should have been excluded", matcher
				.isIncluded("adminChannel"));
	}

	List<IncludeExcludePattern> createIncludeExcludeList(boolean[] includeExclude, String[] patterns) {
		assertEquals("flag and patterns arrays must be same length", includeExclude.length, patterns.length);

		List<IncludeExcludePattern> includeExcludePatterns = new ArrayList<IncludeExcludePattern>(patterns.length);
		for (int i = 0; i < includeExclude.length; i++) {
			includeExcludePatterns.add(new IncludeExcludePattern(includeExclude[i], patterns[i]));
		}
		return includeExcludePatterns;
	}

}
