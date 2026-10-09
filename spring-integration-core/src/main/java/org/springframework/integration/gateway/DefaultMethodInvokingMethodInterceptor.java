/*
 * Copyright 2015-present the original author or authors.
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
