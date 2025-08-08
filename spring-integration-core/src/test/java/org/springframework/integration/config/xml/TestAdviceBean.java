/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.lang.reflect.Method;

import org.springframework.aop.MethodBeforeAdvice;

/**
 * @author Mark Fisher
 */
public class TestAdviceBean implements MethodBeforeAdvice {

	private final int id;

	public TestAdviceBean(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public void before(Method method, Object[] args, Object target) throws Throwable {
	}

}
