/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Base class for MessageGroupProcessor implementations that aggregate the group of Messages into a single Message.
 * 
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractAggregatingMessageGroupProcessor implements MessageGroupProcessor {

	private final Log logger = LogFactory.getLog(this.getClass());

	@SuppressWarnings("unchecked")
	public final void processAndSend(MessageGroup group, MessagingTemplate channelTemplate,
			MessageChannel outputChannel) {
		Assert.notNull(group, "MessageGroup must not be null");
		Assert.notNull(outputChannel, "'outputChannel' must not be null");
		Object payload = this.aggregatePayloads(group);
		Map<String, Object> headers = this.aggregateHeaders(group);
		MessageBuilder<?> builder = (payload instanceof Message) ? MessageBuilder.fromMessage((Message<?>) payload)
				: MessageBuilder.withPayload(payload);
		Message<?> message = builder.copyHeadersIfAbsent(headers).build();
		channelTemplate.send(outputChannel, message);
	}

	/**
	 * This default implementation simply returns all headers that have no conflicts among the group. An absent header
	 * on one or more Messages within the group is not considered a conflict. Subclasses may override this method with
	 * more advanced conflict-resolution strategies if necessary.
	 */
	protected Map<String, Object> aggregateHeaders(MessageGroup group) {
		Map<String, Object> aggregatedHeaders = new HashMap<String, Object>();
		Set<String> conflictKeys = new HashSet<String>();
		for (Message<?> message : group.getUnmarked()) {
			MessageHeaders currentHeaders = message.getHeaders();
			for (String key : currentHeaders.keySet()) {
				if (MessageHeaders.ID.equals(key) || MessageHeaders.TIMESTAMP.equals(key)
						|| MessageHeaders.SEQUENCE_SIZE.equals(key)) {
					continue;
				}
				Object value = currentHeaders.get(key);
				if (!aggregatedHeaders.containsKey(key)) {
					aggregatedHeaders.put(key, value);
				} else if (!value.equals(aggregatedHeaders.get(key))) {
					conflictKeys.add(key);
				}
			}
		}
		for (String keyToRemove : conflictKeys) {
			if (logger.isDebugEnabled()) {
				logger.debug("Excluding header '" + keyToRemove + "' upon aggregation due to conflict(s) "
						+ "in MessageGroup with correlation key: " + group.getGroupId());
			}
			aggregatedHeaders.remove(keyToRemove);
		}
		return aggregatedHeaders;
	}

	protected abstract Object aggregatePayloads(MessageGroup group);

	protected MessageListProcessor getAdapter(Object candidate, Class<? extends Annotation> annotationType) {
		Method method = findAggregatorMethod(candidate, annotationType);
		if (method == null) {
			return null;
		}
		return new MethodInvokingMessageListProcessor(candidate, method);
	}

	private Method findAggregatorMethod(Object candidate, Class<? extends Annotation> annotationType) {
		Class<?> targetClass = AopUtils.getTargetClass(candidate);
		if (targetClass == null) {
			targetClass = candidate.getClass();
		}
		Method method = this.findAnnotatedMethod(targetClass, annotationType);
		if (method == null) {
			method = this.findSinglePublicMethod(targetClass);
		}
		return method;
	}

	private Method findAnnotatedMethod(final Class<?> targetClass, final Class<? extends Annotation> annotationType) {
		final AtomicReference<Method> annotatedMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
				if (annotation != null) {
					Assert.isNull(annotatedMethod.get(), "found more than one method on target class [" + targetClass
							+ "] with the annotation type [" + annotationType.getName() + "]");
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
