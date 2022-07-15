/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Strategy interface for post-processing annotated methods.
 *
 * @param <T> the target annotation type.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public interface MethodAnnotationPostProcessor<T extends Annotation> {

	String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";

	Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations);

	void processBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition, List<Annotation> annotations);

	/**
	 * Determine if the provided {@code method} and its {@code annotations} are eligible
	 * to create an {@link org.springframework.integration.endpoint.AbstractEndpoint}.
	 * @param method the method to check if it is eligible to create an Endpoint
	 * @param annotations the List of annotations to process
	 * @return the {@code boolean} flag to determine whether to create an
	 * {@link org.springframework.integration.endpoint.AbstractEndpoint}
	 * @since 4.0
	 */
	default boolean shouldCreateEndpoint(Method method, List<Annotation> annotations) {
		return shouldCreateEndpoint(MergedAnnotations.from(method), annotations);
	}

	default boolean shouldCreateEndpoint(MergedAnnotations mergedAnnotations, List<Annotation> annotations) {
		String inputChannel =
				MessagingAnnotationUtils.resolveAttribute(annotations, getInputChannelAttribute(), String.class);
		return StringUtils.hasText(inputChannel);
	}

	default String getInputChannelAttribute() {
		return INPUT_CHANNEL_ATTRIBUTE;
	}

	default boolean beanAnnotationAware() {
		return true;
	}

	default boolean supportsPojoMethod() {
		return true;
	}

}
