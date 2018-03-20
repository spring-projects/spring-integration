/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.util.Assert;

/**
 * An advisor that will apply the {@link MessagePublishingInterceptor} to any
 * methods containing the provided annotations. If no annotations are provided,
 * the default will be {@link Publisher @Publisher}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SuppressWarnings("serial")
public class PublisherAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private final Set<Class<? extends Annotation>> publisherAnnotationTypes;

	private final MessagePublishingInterceptor interceptor;

	@SuppressWarnings("unchecked")
	public PublisherAnnotationAdvisor(Class<? extends Annotation>... publisherAnnotationTypes) {
		this.publisherAnnotationTypes = new HashSet<>(Arrays.asList(publisherAnnotationTypes));
		PublisherMetadataSource metadataSource = new MethodAnnotationPublisherMetadataSource(this.publisherAnnotationTypes);
		this.interceptor = new MessagePublishingInterceptor(metadataSource);
	}

	@SuppressWarnings("unchecked")
	public PublisherAnnotationAdvisor() {
		this(Publisher.class);
	}


	/**
	 * A channel bean name to be used as default for publishing.
	 * @param defaultChannelName the default channel name.
	 * @since 4.0.3
	 */
	public void setDefaultChannelName(String defaultChannelName) {
		this.interceptor.setDefaultChannelName(defaultChannelName);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.interceptor.setChannelResolver(new BeanFactoryChannelResolver(beanFactory));
		this.interceptor.setBeanFactory(beanFactory);
	}

	@Override
	public Advice getAdvice() {
		return this.interceptor;
	}

	@Override
	public Pointcut getPointcut() {
		return this.buildPointcut();
	}

	private Pointcut buildPointcut() {
		ComposablePointcut result = null;
		for (Class<? extends Annotation> publisherAnnotationType : this.publisherAnnotationTypes) {
			Pointcut cpc = new MetaAnnotationMatchingPointcut(publisherAnnotationType, true);
			Pointcut mpc = new MetaAnnotationMatchingPointcut(null, publisherAnnotationType);
			if (result == null) {
				result = new ComposablePointcut(cpc).union(mpc);
			}
			else {
				result.union(cpc).union(mpc);
			}
		}
		return result;
	}


	private static final class MetaAnnotationMatchingPointcut implements Pointcut {

		private final ClassFilter classFilter;

		private final MethodMatcher methodMatcher;


		/**
		 * Create a new MetaAnnotationMatchingPointcut for the given annotation type.
		 * @param classAnnotationType the annotation type to look for at the class level
		 * @param checkInherited whether to explicitly check the superclasses and
		 * interfaces for the annotation type as well (even if the annotation type
		 * is not marked as inherited itself)
		 */
		MetaAnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType, boolean checkInherited) {
			this.classFilter = new AnnotationClassFilter(classAnnotationType, checkInherited);
			this.methodMatcher = MethodMatcher.TRUE;
		}

		/**
		 * Create a new MetaAnnotationMatchingPointcut for the given annotation type.
		 * @param classAnnotationType the annotation type to look for at the class level
		 * (can be <code>null</code>)
		 * @param methodAnnotationType the annotation type to look for at the method level
		 * (can be <code>null</code>)
		 */
		MetaAnnotationMatchingPointcut(
				Class<? extends Annotation> classAnnotationType, Class<? extends Annotation> methodAnnotationType) {

			Assert.isTrue((classAnnotationType != null || methodAnnotationType != null),
					"Either Class annotation type or Method annotation type needs to be specified (or both)");

			if (classAnnotationType != null) {
				this.classFilter = new AnnotationClassFilter(classAnnotationType);
			}
			else {
				this.classFilter = ClassFilter.TRUE;
			}

			if (methodAnnotationType != null) {
				this.methodMatcher = new MetaAnnotationMethodMatcher(methodAnnotationType);
			}
			else {
				this.methodMatcher = MethodMatcher.TRUE;
			}
		}


		@Override
		public ClassFilter getClassFilter() {
			return this.classFilter;
		}

		@Override
		public MethodMatcher getMethodMatcher() {
			return this.methodMatcher;
		}
	}


	private static final class MetaAnnotationMethodMatcher extends AnnotationMethodMatcher {

		private final Class<? extends Annotation> annotationType;


		/**
		 * Create a new AnnotationClassFilter for the given annotation type.
		 * @param annotationType the annotation type to look for
		 */
		MetaAnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
			super(annotationType);
			this.annotationType = annotationType;
		}


		@Override
		@SuppressWarnings("rawtypes")
		public boolean matches(Method method, Class targetClass) {
			if (AnnotationUtils.getAnnotation(method, this.annotationType) != null) {
				return true;
			}
			// The method may be on an interface, so let's check on the target class as well.
			Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			return (specificMethod != method &&
					(AnnotationUtils.getAnnotation(specificMethod, this.annotationType) != null));
		}
	}

}
