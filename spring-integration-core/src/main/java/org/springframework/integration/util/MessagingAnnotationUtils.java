/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.Payloads;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility methods to support annotation processing.
 *
 * @author Gary Russell
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Chris Bono
 * @author Ngoc Nhan
 *
 * @since 4.0
 */
public final class MessagingAnnotationUtils {

	/**
	 * Get the attribute value from the annotation hierarchy, returning the first
	 * {@link MessagingAnnotationUtils#hasValue non-empty}) value closest to the annotated method.
	 * @param annotations The meta-annotations in order (closest first).
	 * @param name The attribute name.
	 * @param requiredType The expected type.
	 * @param <T> The type.
	 * @return The value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T resolveAttribute(List<Annotation> annotations, String name, Class<T> requiredType) {
		for (Annotation annotation : annotations) {
			Object value = AnnotationUtils.getValue(annotation, name);
			if (requiredType.isInstance(value) && hasValue(value)) {
				return (T) value;
			}
		}
		return null;
	}

	/**
	 * Determine if the value of a named attribute from an annotation instance contains an actual value.
	 * @param annotationValue the value of the annotation attribute
	 * @return {@code false} when {@code annotationValue} is null, an empty string, an empty array, or any annotation
	 * whose 'value' field is set to {@link ValueConstants#DEFAULT_NONE} - {@code true} otherwise
	 */
	public static boolean hasValue(Object annotationValue) {
		if (annotationValue == null) {
			return false;
		}
		// Empty array
		if (ObjectUtils.isArray(annotationValue) && ObjectUtils.isEmpty(annotationValue)) {
			return false;
		}
		// Empty string
		if ((annotationValue instanceof String) && ObjectUtils.isEmpty(annotationValue)) {
			return false;
		}
		// Annotation with 'value' set to special 'none' string
		return (!(annotationValue instanceof Annotation)) ||
				!ValueConstants.DEFAULT_NONE.equals(AnnotationUtils.getValue((Annotation) annotationValue));
	}

	public static Method findAnnotatedMethod(Object target, final Class<? extends Annotation> annotationType) {
		final AtomicReference<Method> reference = new AtomicReference<>();

		ReflectionUtils.doWithMethods(AopProxyUtils.ultimateTargetClass(target),
				method -> reference.compareAndSet(null, method),
				method -> ReflectionUtils.USER_DECLARED_METHODS.matches(method) &&
						AnnotatedElementUtils.isAnnotated(method, annotationType.getName()));

		return reference.get();
	}

	/**
	 * Find the one of {@link Payload}, {@link Header} or {@link Headers} annotation from
	 * the provided {@code annotations} array. Optionally also detects {@link Payloads}.
	 * @param annotations the annotations to scan.
	 * @param payloads true if @Payloads should be detected.
	 * @return the matched annotation or {@code null}.
	 * @throws MessagingException if more than one of {@link Payload}, {@link Header}
	 * or {@link Headers} annotations are presented.
	 */
	public static Annotation findMessagePartAnnotation(Annotation[] annotations, boolean payloads) {
		if (annotations == null || annotations.length == 0) {
			return null;
		}
		Annotation match = null;
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> type = annotation.annotationType();
			if (type.equals(Payload.class) // NOSONAR boolean complexity
					|| type.equals(Header.class)
					|| type.equals(Headers.class)
					|| (payloads && type.equals(Payloads.class))) {
				if (match != null) {
					throw new MessagingException("At most one parameter annotation can be provided "
							+ "for message mapping, but found two: [" + match.annotationType().getName() + "] and ["
							+ annotation.annotationType().getName() + "]");
				}
				match = annotation;
			}
		}
		return match;
	}

	/**
	 * Return the {@link EndpointId#value()} property, if present.
	 * @param method the methods.
	 * @return the id, or null.
	 * @since 5.0.4
	 */
	@Nullable
	public static String endpointIdValue(Method method) {
		return endpointIdValue(MergedAnnotations.from(method));
	}

	/**
	 * Return the {@link EndpointId#value()} property, if present.
	 * @param mergedAnnotations the {@link MergedAnnotations} to analyze.
	 * @return the id, or null.
	 * @since 6.0
	 */
	@Nullable
	public static String endpointIdValue(MergedAnnotations mergedAnnotations) {
		MergedAnnotation<EndpointId> endpointIdAnnotation = mergedAnnotations.get(EndpointId.class);
		return endpointIdAnnotation.getValue(AnnotationUtils.VALUE, String.class).orElse(null);
	}

	/**
	 * Get a chain of its meta-annotations for the provided instance and expected type.
	 * @param messagingAnnotation the {@link Annotation} to take a chain for its meta-annotations.
	 * @param annotationType the annotation type.
	 * @return the hierarchical list of annotations in top-bottom order.
	 * @since 6.0
	 */
	public static List<Annotation> getAnnotationChain(Annotation messagingAnnotation,
			Class<? extends Annotation> annotationType) {

		List<Annotation> annotationChain = new LinkedList<>();
		Set<Annotation> visited = new HashSet<>();

		recursiveFindAnnotation(annotationType, messagingAnnotation, annotationChain, visited);
		if (!annotationChain.isEmpty()) {
			Collections.reverse(annotationChain);
		}

		return annotationChain;
	}

	private static boolean recursiveFindAnnotation(Class<? extends Annotation> annotationType, Annotation ann,
			List<Annotation> annotationChain, Set<Annotation> visited) {

		if (ann.annotationType().equals(annotationType)) {
			annotationChain.add(ann);
			return true;
		}
		for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
			if (!ann.equals(metaAnn) && !visited.contains(metaAnn)
					&& !(metaAnn.annotationType().getPackage().getName().startsWith("java.lang"))) {
				visited.add(metaAnn); // prevent infinite recursion if the same annotation is found again
				if (recursiveFindAnnotation(annotationType, metaAnn, annotationChain, visited)) {
					annotationChain.add(ann);
					return true;
				}
			}
		}
		return false;
	}

	private MessagingAnnotationUtils() {
	}

}
