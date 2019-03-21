/*
 * Copyright 2015-2019 the original author or authors.
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

import java.util.function.Supplier;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;

import org.springframework.util.ObjectUtils;

/**
 * A matcher that evaluates against the result of invoking a function,
 * wrapped by the {@link java.util.function.Supplier}
 *
 * The goal is to defer the computation until the matcher needs to be actually evaluated.
 * Mainly useful in conjunction with retrying matchers such as {@code EventuallyMatcher}
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class EqualsResultMatcher<U> extends DiagnosingMatcher<U> {

	private final Supplier<U> supplier;

	public EqualsResultMatcher(Supplier<U> supplier) {
		this.supplier = supplier;
	}

	@Override
	protected boolean matches(Object item, Description mismatchDescription) {
		return ObjectUtils.nullSafeEquals(item, supplier.get());
	}

	@Override
	public void describeTo(Description description) {
	}

	public static <U> EqualsResultMatcher<U> equalsResult(Supplier<U> supplier) {
		return new EqualsResultMatcher<>(supplier);
	}

}
