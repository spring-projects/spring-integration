/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
