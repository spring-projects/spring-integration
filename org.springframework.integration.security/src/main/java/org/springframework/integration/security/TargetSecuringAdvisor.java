/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.security;

import org.aopalliance.aop.Advice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ClassFilters;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.aop.support.RootClassFilter;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Target;
import org.springframework.security.AccessDecisionManager;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.util.StringUtils;

public class TargetSecuringAdvisor extends AbstractPointcutAdvisor implements Pointcut {

	private ClassFilter classFilter;

	private MethodMatcher matcher;

	private Advice targetSecuringInterceptor;

	public TargetSecuringAdvisor(AccessDecisionManager accessDecisionManager, String securityConfig) {
		targetSecuringInterceptor = new TargetSecuringInterceptor(new ConfigAttributeDefinition(StringUtils
				.tokenizeToStringArray(securityConfig, ",")), accessDecisionManager);
		classFilter = ClassFilters.union(new RootClassFilter(Target.class), new RootClassFilter(BlockingTarget.class));
		NameMatchMethodPointcut nameMatcher = new NameMatchMethodPointcut();
		nameMatcher.addMethodName("send");
		matcher = MethodMatchers.intersection(nameMatcher, new TargetSendMethodArgMatcher());
	}

	public Pointcut getPointcut() {
		return this;
	}

	public Advice getAdvice() {
		return targetSecuringInterceptor;
	}

	public ClassFilter getClassFilter() {
		return classFilter;
	}

	public MethodMatcher getMethodMatcher() {
		return matcher;
	}

}
