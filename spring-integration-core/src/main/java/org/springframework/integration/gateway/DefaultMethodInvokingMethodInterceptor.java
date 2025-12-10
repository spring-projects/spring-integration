/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.gateway;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Method interceptor to invoke default methods from the interfaces on the proxy.
 * <p>
 * The copy of {@code DefaultMethodInvokingMethodInterceptor} from Spring Data Commons.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class DefaultMethodInvokingMethodInterceptor implements MethodInterceptor {

	private static final Lookup LOOKUP = MethodHandles.lookup();

	private final Map<Method, MethodHandle> methodHandleCache =
			new ConcurrentReferenceHashMap<>(10, ReferenceType.WEAK);

	@Override
	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (!method.isDefault()) {
			return invocation.proceed();
		}
		Object proxy = ((ProxyMethodInvocation) invocation).getProxy();
		@Nullable Object[] arguments = invocation.getArguments();
		return getMethodHandle(method)
				.bindTo(proxy)
				.invokeWithArguments(arguments);
	}

	private MethodHandle getMethodHandle(Method method) throws Exception {
		return this.methodHandleCache.computeIfAbsent(method, DefaultMethodInvokingMethodInterceptor::lookup);
	}

	/**
	 * Lookup a {@link MethodHandle} given {@link Method} to look up.
	 * @param method must not be {@literal null}.
	 * @return the method handle.
	 */
	private static MethodHandle lookup(Method method) {
		try {
			Class<?> declaringClass = method.getDeclaringClass();
			Lookup lookup = MethodHandles.privateLookupIn(declaringClass, LOOKUP);
			MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());

			return Modifier.isStatic(method.getModifiers())
					? lookup.findStatic(declaringClass, method.getName(), methodType)
					: lookup.findSpecial(declaringClass, method.getName(), methodType, declaringClass);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
