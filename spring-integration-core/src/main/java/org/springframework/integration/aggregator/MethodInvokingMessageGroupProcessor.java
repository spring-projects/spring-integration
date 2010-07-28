/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * MessageGroupProcessor that serves as an adapter for the invocation of a POJO method.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodInvokingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	private final MessageListMethodAdapter adapter;

	/**
	 * Creates a wrapper around the target passed in. This constructor will choose the best fitting method and throw an
	 * exception when methods are ambiguous or no fitting methods can be found.
	 * 
	 * @param target the object to wrap
	 * @throws IllegalStateException when no single method can be found unambiguously
	 */
	public MethodInvokingMessageGroupProcessor(Object target) {
		this.adapter = new MessageListMethodAdapter(target, this.findAggregatorMethod(target));
	}

	/**
	 * Creates a wrapper around the object passed in. This constructor will look for a named method specifically and
	 * fail when it cannot find a method with the given name.
	 * 
	 * @param target the object to wrap
	 * @param method the name of the method to look for
	 */
	public MethodInvokingMessageGroupProcessor(Object target, String method) {
		this.adapter = new MessageListMethodAdapter(target, method);
	}

	@Override
	protected final Object aggregatePayloads(MessageGroup group) {
		final Collection<Message<?>> messagesUpForProcessing = group.getUnmarked();
		Object result = this.adapter.executeMethod(messagesUpForProcessing);
		return result;
	}

	private Method findAggregatorMethod(Object candidate) {
		Class<?> targetClass = AopUtils.getTargetClass(candidate);
		if (targetClass == null) {
			targetClass = candidate.getClass();
		}
		Method method = this.findAnnotatedMethod(targetClass);
		if (method == null) {
			method = this.findSinglePublicMethod(targetClass);
		}
		return method;
	}

	private Method findAnnotatedMethod(final Class<?> targetClass) {
		final AtomicReference<Method> annotatedMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.findAnnotation(method, Aggregator.class);
				if (annotation != null) {
					Assert.isNull(annotatedMethod.get(), "found more than one method on target class [" + targetClass
							+ "] with the annotation type [" + Aggregator.class.getName() + "]");
					annotatedMethod.set(method);
				}
			}
		});
		return annotatedMethod.get();
	}

	private Method findSinglePublicMethod(Class<?> targetClass) {
		Set<Method> methods = new HashSet<Method>();
		for (Method method : targetClass.getMethods()) {
			if (!method.getDeclaringClass().equals(Object.class)) {
				methods.add(method);
			}
		}
		removeListIncompatibleMethodsFrom(methods);
		removeVoidMethodsFrom(methods);
		removeUnfittingFrom(methods);
		if (methods.size() > 1) {
			throw new IllegalArgumentException("Class [" + targetClass + "] contains more than one public Method.");
		}
		return methods.isEmpty() ? null : methods.iterator().next();
	}

	private void removeListIncompatibleMethodsFrom(Set<Method> candidates) {
		removeMethodsMatchingSelector(candidates, new MethodSelector() {
			public boolean select(Method method) {
				int found = 0;
				for (Class<?> parameterClass : method.getParameterTypes()) {
					if (Collection.class.isAssignableFrom(parameterClass)) {
						found++;
					}
				}
				return found != 1;
			}
		});
	}

	private void removeVoidMethodsFrom(Set<Method> candidates) {
		removeMethodsMatchingSelector(candidates, new MethodSelector() {
			public boolean select(Method method) {
				return method.getReturnType().getName().equals("void");
			}
		});
	}

	private Set<Method> removeUnfittingFrom(Set<Method> candidates) {
		return removeMethodsMatchingSelector(candidates, new MethodSelector() {
			public boolean select(Method method) {
				Annotation[][] parameterAnnotations = method.getParameterAnnotations();
				Class<?>[] parameterTypes = method.getParameterTypes();
				return (!isFittinglyAnnotated(parameterTypes, parameterAnnotations));
			}
		});
	}

	private boolean isFittinglyAnnotated(Class<?>[] parameterTypes, Annotation[][] parameterAnnotations) {
		int candidateParametersFound = 0;
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> parameterType = parameterTypes[i];
			if (Collection.class.isAssignableFrom(parameterType)) {
				boolean headerAnnotationFound = false;
				for (Annotation annotation : parameterAnnotations[i]) {
					if (annotation instanceof Header) {
						headerAnnotationFound = true;
					}
				}
				if (!headerAnnotationFound) {
					candidateParametersFound++;
				}
			}
		}
		return candidateParametersFound == 1;
	}

	private Set<Method> removeMethodsMatchingSelector(Set<Method> candidates, MethodSelector selector) {
		Set<Method> removed = new HashSet<Method>();
		Iterator<Method> iterator = candidates.iterator();
		while (iterator.hasNext()) {
			Method method = iterator.next();
			if (selector.select(method)) {
				iterator.remove();
				removed.add(method);
			}
		}
		return removed;
	}

	private interface MethodSelector {
		boolean select(Method method);
	}

}
