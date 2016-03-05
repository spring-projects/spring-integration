/*
 * Copyright 2015-2016 the original author or authors.
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

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;

import org.springframework.util.ObjectUtils;

/**
 * A matcher that evaluates against the result of invoking a function,
 * wrapped by the {@link EqualsResultMatcher.Evaluator}
 *
 * The goal is to defer the computation until the matcher needs to be actually evaluated.
 * Mainly useful in conjunction with retrying matchers such as {@link EventuallyMatcher}
 *
 * @author Marius Bogoevici
 * @since 4.2
 */
public class EqualsResultMatcher<U> extends DiagnosingMatcher<U> {

	private final Evaluator<U> evaluator;

	public EqualsResultMatcher(Evaluator<U> evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	protected boolean matches(Object item, Description mismatchDescription) {
		return ObjectUtils.nullSafeEquals(item, evaluator.evaluate());
	}

	@Override
	public void describeTo(Description description) {
	}

	public interface Evaluator<U> {
		U evaluate();
	}

	public static <U> EqualsResultMatcher<U> equalsResult(Evaluator<U> evaluator) {
		return new EqualsResultMatcher<U>(evaluator);
	}
}
