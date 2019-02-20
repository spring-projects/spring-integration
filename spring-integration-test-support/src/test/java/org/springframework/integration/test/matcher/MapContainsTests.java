/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
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
		map = new HashMap<>();
		map.put(SOME_KEY, SOME_VALUE);
		map.put(OTHER_KEY, OTHER_VALUE);
	}

	@Test
	public void hasKey_validKey_matching() {
		Assert.assertThat(map, Matchers.hasKey(SOME_KEY));

	}

	@Test
	public void hasKey_unknownKey_notMatching() {
		Assert.assertThat(map, Matchers.not(Matchers.hasKey(UNKNOWN_KEY)));
	}

	@Test
	public void hasEntry_withValidKeyValue_matches() {
		Assert.assertThat(map, Matchers.hasEntry(SOME_KEY, SOME_VALUE));
		Assert.assertThat(map, Matchers.hasEntry(OTHER_KEY, OTHER_VALUE));
	}

	@Test
	public void hasEntry_withUnknownKey_notMatching() {
		Assert.assertThat(map, Matchers.not(Matchers.hasEntry("test.unknown", SOME_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_matches() {
		Assert.assertThat(map, Matchers.hasEntry(Matchers.is(SOME_KEY), Matchers.instanceOf(String.class)));
		Assert.assertThat(map, Matchers.hasEntry(Matchers.is(SOME_KEY), Matchers.notNullValue()));
		Assert.assertThat(map, Matchers.hasEntry(Matchers.is(SOME_KEY), Matchers.is(SOME_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_notMatching() {
		Assert.assertThat(map,
				Matchers.not(Matchers.hasEntry(SOME_KEY, Matchers.is(Matchers.instanceOf(Integer.class)))));
	}

	@Test
	public void hasEntry_withTypedValueMap_matches() {
		Map<String, String> map = new HashMap<>();
		map.put("a", "b");
		map.put("c", "d");
		Assert.assertThat(map, Matchers.hasEntry("a", "b"));
		Assert.assertThat(map, Matchers.not(Matchers.hasEntry(SOME_KEY, Matchers.is("a"))));
		Assert.assertThat(map, MapContentMatchers.hasAllEntries(map));
	}

	@Test
	public void hasAllEntries_withValidKeyValueOrMatcherValue_matches() {
		Map<String, Object> expectedInHeaderMap = new HashMap<>();
		expectedInHeaderMap.put(SOME_KEY, SOME_VALUE);
		expectedInHeaderMap.put(OTHER_KEY, Matchers.is(OTHER_VALUE));
		Assert.assertThat(map, MapContentMatchers.hasAllEntries(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withInvalidValidKeyValueOrMatcherValue_notMatching() {
		Map<String, Object> expectedInHeaderMap = new HashMap<>();
		expectedInHeaderMap.put(SOME_KEY, SOME_VALUE); // valid
		expectedInHeaderMap.put(UNKNOWN_KEY, Matchers.not(Matchers.nullValue())); // fails
		Assert.assertThat(map, Matchers.not(MapContentMatchers.hasAllEntries(expectedInHeaderMap)));
		expectedInHeaderMap.remove(UNKNOWN_KEY);
		expectedInHeaderMap.put(OTHER_KEY, SOME_VALUE); // fails
	}

}
