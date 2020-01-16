/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.test.matcher;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Alex Peters
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class MapContainsTests {

	static final String UNKNOWN_KEY = "unknownKey";

	static final String SOME_VALUE = "bar";

	static final String SOME_KEY = "test.foo";

	static final String OTHER_KEY = "test.number";

	static final Integer OTHER_VALUE = 123;

	private HashMap<String, Object> map;

	@BeforeEach
	public void setUp() {
		map = new HashMap<>();
		map.put(SOME_KEY, SOME_VALUE);
		map.put(OTHER_KEY, OTHER_VALUE);
	}

	@Test
	public void hasKey_validKey_matching() {
		MatcherAssert.assertThat(map, Matchers.hasKey(SOME_KEY));

	}

	@Test
	public void hasKey_unknownKey_notMatching() {
		MatcherAssert.assertThat(map, Matchers.not(Matchers.hasKey(UNKNOWN_KEY)));
	}

	@Test
	public void hasEntry_withValidKeyValue_matches() {
		MatcherAssert.assertThat(map, Matchers.hasEntry(SOME_KEY, SOME_VALUE));
		MatcherAssert.assertThat(map, Matchers.hasEntry(OTHER_KEY, OTHER_VALUE));
	}

	@Test
	public void hasEntry_withUnknownKey_notMatching() {
		MatcherAssert.assertThat(map, Matchers.not(Matchers.hasEntry("test.unknown", SOME_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_matches() {
		MatcherAssert.assertThat(map, Matchers.hasEntry(Matchers.is(SOME_KEY), Matchers.instanceOf(String.class)));
		MatcherAssert.assertThat(map, Matchers.hasEntry(Matchers.is(SOME_KEY), Matchers.notNullValue()));
		MatcherAssert.assertThat(map, Matchers.hasEntry(Matchers.is(SOME_KEY), Matchers.is(SOME_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_notMatching() {
		MatcherAssert.assertThat(map,
				Matchers.not(Matchers.hasEntry(SOME_KEY, Matchers.is(Matchers.instanceOf(Integer.class)))));
	}

	@Test
	public void hasEntry_withTypedValueMap_matches() {
		Map<String, String> map = new HashMap<>();
		map.put("a", "b");
		map.put("c", "d");
		MatcherAssert.assertThat(map, Matchers.hasEntry("a", "b"));
		MatcherAssert.assertThat(map, Matchers.not(Matchers.hasEntry(SOME_KEY, Matchers.is("a"))));
		MatcherAssert.assertThat(map, MapContentMatchers.hasAllEntries(map));
	}

	@Test
	public void hasAllEntries_withValidKeyValueOrMatcherValue_matches() {
		Map<String, Object> expectedInHeaderMap = new HashMap<>();
		expectedInHeaderMap.put(SOME_KEY, SOME_VALUE);
		expectedInHeaderMap.put(OTHER_KEY, Matchers.is(OTHER_VALUE));
		MatcherAssert.assertThat(map, MapContentMatchers.hasAllEntries(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withInvalidValidKeyValueOrMatcherValue_notMatching() {
		Map<String, Object> expectedInHeaderMap = new HashMap<>();
		expectedInHeaderMap.put(SOME_KEY, SOME_VALUE); // valid
		expectedInHeaderMap.put(UNKNOWN_KEY, Matchers.not(Matchers.nullValue())); // fails
		MatcherAssert.assertThat(map, Matchers.not(MapContentMatchers.hasAllEntries(expectedInHeaderMap)));
		expectedInHeaderMap.remove(UNKNOWN_KEY);
		expectedInHeaderMap.put(OTHER_KEY, SOME_VALUE); // fails
	}

}
