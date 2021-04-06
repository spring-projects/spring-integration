/*
 * Copyright 2015-2021 the original author or authors.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import org.springframework.util.ReflectionUtils;

/**
 * Method interceptor to invoke default methods on the gateway proxy.
 *
 * The copy of {@code DefaultMethodInvokingMethodInterceptor} from Spring Data Commons.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Artem Bilan
 *
 * @since 5.3
 */
class DefaultMethodInvokingMethodInterceptor implements MethodInterceptor {

	private final MethodHandleLookup methodHandleLookup = MethodHandleLookup.getMethodHandleLookup();

	private final Map<Method, MethodHandle> methodHandleCache =
			new ConcurrentReferenceHashMap<>(10, ReferenceType.WEAK);

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable { // NOSONAR
		Method method = invocation.getMethod();
		if (!method.isDefault()) {
			return invocation.proceed();
		}
		Object[] arguments = invocation.getArguments();
		Object proxy = ((ProxyMethodInvocation) invocation).getProxy();
		return getMethodHandle(method)
				.bindTo(proxy)
				.invokeWithArguments(arguments);
	}

	private MethodHandle getMethodHandle(Method method) {
		return this.methodHandleCache.computeIfAbsent(method,
				(key) -> {
					try {
						return this.methodHandleLookup.lookup(key);
					}
					catch (ReflectiveOperationException ex) {
						throw new IllegalStateException(ex);
					}
				});
	}

	enum MethodHandleLookup {

		/**
		 * Encapsulated {@link MethodHandle} lookup working on Java 9.
		 */
		ENCAPSULATED {

			@Nullable
			private final transient Method privateLookupIn =
					ReflectionUtils.findMethod(MethodHandles.class, "privateLookupIn", Class.class, Lookup.class);

			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {
				if (this.privateLookupIn == null) {
					throw new IllegalStateException("Could not obtain MethodHandles.privateLookupIn!");
				}
				return doLookup(method, getLookup(method.getDeclaringClass(), this.privateLookupIn));
			}

			@Override
			boolean isAvailable() {
				return this.privateLookupIn != null;
			}

			private Lookup getLookup(Class<?> declaringClass, Method privateLookupIn) {
				Lookup lookup = MethodHandles.lookup();
				try {
					return (Lookup) privateLookupIn.invoke(MethodHandles.class, declaringClass, lookup);
				}
				catch (ReflectiveOperationException e) {
					return lookup;
				}
			}

		},

		/**
		 * Open (via reflection construction of {@link Lookup}) method handle lookup. Works with Java 8 and
		 * with Java 9 permitting illegal access.
		 */
		OPEN {

			@Nullable
			private final transient Constructor<Lookup> constructor;

			{
				Constructor<Lookup> ctor = null;
				try {
					ctor = Lookup.class.getDeclaredConstructor(Class.class);
					ReflectionUtils.makeAccessible(ctor);
				}
				catch (Exception ex) {
					// this is the signal that we are on Java 9 (encapsulated) and can't use the accessible constructor
					// approach.
					if (!ex.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
						throw new IllegalStateException(ex);
					}
				}
				this.constructor = ctor;
			}

			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {
				if (this.constructor != null) {
					return this.constructor.newInstance(method.getDeclaringClass())
							.unreflectSpecial(method, method.getDeclaringClass());
				}
				else {
					throw new IllegalStateException("Could not obtain MethodHandles.lookup constructor!");
				}
			}

			@Override
			boolean isAvailable() {
				return this.constructor != null;
			}

		},

		/**
		 * Fallback {@link MethodHandle} lookup using {@link MethodHandles#lookup() public lookup}.
		 */
		FALLBACK {

			@Override
			MethodHandle lookup(Method method) throws ReflectiveOperationException {
				return doLookup(method, MethodHandles.lookup());
			}

			@Override
			boolean isAvailable() {
				return true;
			}

		};

		private static MethodHandle doLookup(Method method, Lookup lookup) throws ReflectiveOperationException {
			MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
			return lookup.findSpecial(method.getDeclaringClass(), method.getName(),
					methodType, method.getDeclaringClass());
		}

		abstract MethodHandle lookup(Method method) throws ReflectiveOperationException;

		/**
		 * @return {@literal true} if the lookup is available.
		 */
		abstract boolean isAvailable();

		/**
		 * Obtain the first available {@link MethodHandleLookup}.
		 * @return the {@link MethodHandleLookup}
		 * @throws IllegalStateException if no {@link MethodHandleLookup} is available.
		 */
		public static MethodHandleLookup getMethodHandleLookup() {
			for (MethodHandleLookup it : MethodHandleLookup.values()) {
				if (it.isAvailable()) {
					return it;
				}
			}
			throw new IllegalStateException("No MethodHandleLookup available!");
		}

	}

}
