/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
