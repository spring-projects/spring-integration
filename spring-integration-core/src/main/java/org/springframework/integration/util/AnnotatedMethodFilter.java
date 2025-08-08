/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.MethodFilter;
import org.springframework.util.StringUtils;

/**
 * A MethodFilter implementation that enables the following:
 * <ol>
 *   <li>matching on method name, if available</li>
 *   <li>exclusion of void-returning methods if 'requiresReply' is true</li>
 *   <li>limiting to annotated methods if at least one is present</li>
 * </ol>.
 *
 * @author Mark Fisher
 *
 * @since 2.0
 */
public class AnnotatedMethodFilter implements MethodFilter {

	private final Class<? extends Annotation> annotationType;

	private final String methodName;

	private final boolean requiresReply;

	public AnnotatedMethodFilter(Class<? extends Annotation> annotationType, String methodName, boolean requiresReply) {
		this.annotationType = annotationType;
		this.methodName = methodName;
		this.requiresReply = requiresReply;
	}

	public List<Method> filter(List<Method> methods) {
		List<Method> annotatedCandidates = new ArrayList<>();
		List<Method> fallbackCandidates = new ArrayList<>();
		for (Method method : methods) {
			if (method.isBridge()) {
				continue;
			}
			if (this.requiresReply && method.getReturnType().equals(void.class)) {
				continue;
			}
			if (StringUtils.hasText(this.methodName) && !this.methodName.equals(method.getName())) {
				continue;
			}
			if (this.annotationType != null && AnnotationUtils.findAnnotation(method, this.annotationType) != null) {
				annotatedCandidates.add(method);
			}
			else {
				fallbackCandidates.add(method);
			}
		}
		return (!annotatedCandidates.isEmpty()) ? annotatedCandidates : fallbackCandidates;
	}

}
