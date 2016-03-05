/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.test.matcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.test.matcher.MapContentMatchers.hasAllEntries;
import static org.springframework.integration.test.matcher.MapContentMatchers.hasEntry;
import static org.springframework.integration.test.matcher.MapContentMatchers.hasKey;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Peters
 * @author Gunnar Hillert
 *
 */
public class MapContainsTests {

	static final String UNKNOWN_KEY = "unknownKey";

	static final String SOME_VALUE = "bar";

	static final String SOME_KEY = "test.foo";

	static final String OTHER_KEY = "test.number";

	static final Integer OTHER_VALUE = Integer.valueOf(123);

	private HashMap<String, Object> map;

	@Before
	public void setUp() {
		map = new HashMap<String, Object>();
		map.put(SOME_KEY, SOME_VALUE);
		map.put(OTHER_KEY, OTHER_VALUE);
	}

	@Test
	public void hasKey_validKey_matching() throws Exception {
		assertThat(map, hasKey(SOME_KEY));

	}

	@Test
	public void hasKey_unknownKey_notMatching() throws Exception {
		assertThat(map, not(hasKey(UNKNOWN_KEY)));
	}

	@Test
	public void hasEntry_withValidKeyValue_matches() throws Exception {
		assertThat(map, hasEntry(SOME_KEY, SOME_VALUE));
		assertThat(map, hasEntry(OTHER_KEY, OTHER_VALUE));
	}

	@Test
	public void hasEntry_withUnknownKey_notMatching() throws Exception {
		assertThat(map, not(hasEntry("test.unknown", SOME_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_matches() throws Exception {
		assertThat(map, hasEntry(SOME_KEY, is(instanceOf(String.class))));
		assertThat(map, hasEntry(SOME_KEY, notNullValue()));
		assertThat(map, hasEntry(SOME_KEY, is(SOME_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_notMatching() throws Exception {
		assertThat(map, not(hasEntry(SOME_KEY, is(instanceOf(Integer.class)))));
	}

	@Test
	public void hasEntry_withTypedValueMap_matches() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("a", "b");
		map.put("c", "d");
		assertThat(map, hasEntry("a", "b"));
		assertThat(map, not(hasEntry(SOME_KEY, is("a"))));
		assertThat(map, hasAllEntries(map));
	}

	@Test
	public void hasAllEntries_withValidKeyValueOrMatcherValue_matches() throws Exception {
		Map<String, Object> expectedInHeaderMap = new HashMap<String, Object>();
		expectedInHeaderMap.put(SOME_KEY, SOME_VALUE);
		expectedInHeaderMap.put(OTHER_KEY, is(OTHER_VALUE));
		assertThat(map, hasAllEntries(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withInvalidValidKeyValueOrMatcherValue_notMatching() throws Exception {
		Map<String, Object> expectedInHeaderMap = new HashMap<String, Object>();
		expectedInHeaderMap.put(SOME_KEY, SOME_VALUE); // valid
		expectedInHeaderMap.put(UNKNOWN_KEY, not(nullValue())); // fails
		assertThat(map, not(hasAllEntries(expectedInHeaderMap)));
		expectedInHeaderMap.remove(UNKNOWN_KEY);
		expectedInHeaderMap.put(OTHER_KEY, SOME_VALUE); // fails
	}
}
