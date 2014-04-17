/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods to support annotation processing.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public final class MessagingAnnotationUtils {

	/**
	 * Get the attribute value from the annotation hierarchy, returning the first non-empty
	 * value closest to the annotated method. While traversing up the hierarchy, for string-valued
	 * attributes, an empty string is ignored. For array-valued attributes, an empty
	 * array is ignored.
	 * The overridden attribute must be the same type.
	 * @param metaAnnotations The meta-annotations in order (closest first).
	 * @param annotation The meta-annotation (if directAnnotation is not null).
	 * @param name The attribute name.
	 * @param requiredType The expected type.
	 * @return The value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T resolveAttribute(List<Annotation> metaAnnotations, Annotation annotation, String name,
			Class<T> requiredType) {
		for (Annotation metaAnnotation : metaAnnotations) {
			if (metaAnnotation != null) {
				Object value = AnnotationUtils.getValue(metaAnnotation, name);
				if (value != null && value.getClass() == requiredType) {
					if (hasValue(value)) {
						return (T) value;
					}
				}
			}
		}
		return (T) AnnotationUtils.getValue(annotation, name);
	}

	public static boolean hasValue(Object value) {
		return value != null && (!(value instanceof String) || (StringUtils.hasText((String) value)))
				&& (!value.getClass().isArray() || ((Object[]) value).length > 0);
	}

	private MessagingAnnotationUtils() {}

}
