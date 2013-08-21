/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.expression;

import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class SpelFunction implements BeanNameAware {

	private final Method method;

	private String name;

	public SpelFunction(Method method) {
		Assert.notNull(method, "'method' must be provided");
		this.method = method;
	}

	public Method getMethod() {
		return method;
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
