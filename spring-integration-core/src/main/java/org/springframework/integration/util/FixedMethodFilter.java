/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.expression.MethodFilter;
import org.springframework.util.Assert;

/**
 * A {@link MethodFilter} implementation that will always return
 * the same Method instance within a single-element list if it is
 * present in the candidate list. If the Method is not present
 * in the candidate list, it will return an empty list.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class FixedMethodFilter implements MethodFilter {

	private final Method method;

	public FixedMethodFilter(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
	}

	public List<Method> filter(List<Method> methods) {
		if (methods != null && methods.contains(this.method)) {
			List<Method> filteredList = new ArrayList<Method>(1);
			filteredList.add(this.method);
			return filteredList;
		}
		return Collections.<Method>emptyList();
	}

}
