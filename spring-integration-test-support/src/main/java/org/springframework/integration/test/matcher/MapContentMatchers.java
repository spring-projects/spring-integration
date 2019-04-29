/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.AllOf;

/**
 * Matchers that examine the contents of a {@link Map}.
 * <p>
 * It is possible to match a single entry by value or matcher like this:
 * </p>
 *
 * <pre class="code">
 * assertThat(map, hasEntry(SOME_KEY, is(SOME_VALUE)));
 * assertThat(map, hasEntry(SOME_KEY, is(String.class)));
 * assertThat(map, hasEntry(SOME_KEY, notNullValue()));
 * </pre>
 *
 * <p>
 * It's also possible to match multiple entries in a map:
 * </p>
 *
 * <pre class="code">
 * {@code
 * Map<String, Object> expectedInMap = new HashMap<String, Object>();
 * expectedInMap.put(SOME_KEY, SOME_VALUE);
 * expectedInMap.put(OTHER_KEY, is(OTHER_VALUE));
 * assertThat(map, hasAllEntries(expectedInMap));
 * }
 * </pre>
 *
 * <p>If you only need to verify the existence of a key:</p>
 *
 * <pre class="code">
 * assertThat(map, hasKey(SOME_KEY));
 * </pre>
 *
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 */
public final class MapContentMatchers<T, V> extends TypeSafeMatcher<Map<? super T, ? super V>> {

	private final T key;

	private final Matcher<V> valueMatcher;

	private MapContentMatchers(T key, V value) {
		this(key, Matchers.equalTo(value));
	}

	private MapContentMatchers(T key, Matcher<V> valueMatcher) {
		this.key = key;
		this.valueMatcher = valueMatcher;
	}

	@Override
	public boolean matchesSafely(Map<? super T, ? super V> item) {
		return item.containsKey(key) && valueMatcher.matches(item.get(key));
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("an entry with key ").appendValue(key)
				.appendText(" and value matching ").appendDescriptionOf(
				valueMatcher);

	}

	/**
	 * Create {@link Matcher} for map entry.
	 * @param key the key to check.
	 * @param value the value to check.
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @return the {@link Matcher} for map entry.
	 * @deprecated since 5.2 in favor of {@link Matchers#hasEntry(Object, Object)}.
	 */
	@Deprecated
	public static <K, V> Matcher<Map<? extends K, ? extends V>> hasEntry(K key, V value) {
		return Matchers.hasEntry(key, value);
	}

	/**
	 * Create {@link Matcher} for map entry.
	 * @param key the key to check.
	 * @param valueMatcher the {@link Matcher} for value.
	 * @param <T> the key type.
	 * @param <V> the value type.
	 * @return the {@link Matcher} for map entry.
	 * @deprecated since 5.2 in favor of {@link Matchers#hasEntry(Matcher, Matcher)}.
	 */
	@Deprecated
	public static <T, V> Matcher<Map<? extends T, ? extends V>> hasEntry(T key, Matcher<V> valueMatcher) {
		return Matchers.hasEntry(Matchers.is(key), valueMatcher);
	}

	/**
	 * Create {@link Matcher} for map key.
	 * @param key the key to check.
	 * @param <T> the key type.
	 * @return {@link Matcher} for map key.
	 * @deprecated since 5.2 in favor of {@link Matchers#hasKey}.
	 */
	@Deprecated
	public static <T> Matcher<Map<? extends T, ?>> hasKey(T key) {
		return Matchers.hasKey(key);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T, V> Matcher<Map<? extends T, ? extends V>> hasAllEntries(Map<T, V> entries) {
		List<Matcher<? super Map<T, V>>> matchers = new ArrayList<>(entries.size());
		for (Map.Entry<T, V> entry : entries.entrySet()) {
			final V value = entry.getValue();
			if (value instanceof Matcher<?>) {
				matchers.add(Matchers.hasEntry(Matchers.is(entry.getKey()), (Matcher<V>) value));
			}
			else {
				matchers.add(Matchers.hasEntry(entry.getKey(), value));
			}
		}
		//return AllOf.allOf(matchers); //Does not work with Hamcrest 1.3
		return new AllOf(matchers);
	}

}
