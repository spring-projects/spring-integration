/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.config.annotation.MethodAnnotationPostProcessor;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.util.CollectionUtils;

/**
 * A {@link BeanPostProcessor} implementation that processes method-level
 * messaging annotations such as @Transformer, @Splitter, @Router, and @Filter.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Gary Russell
 * @author Rick Hogge
 */
public class MessagingAnnotationPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> postProcessors = new HashMap<>();

	@SuppressWarnings("NullAway.Init")
	private BeanDefinitionRegistry registry;

	@SuppressWarnings("NullAway.Init")
	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		this.registry = registry;

		this.postProcessors.put(Filter.class, new FilterAnnotationPostProcessor());
		this.postProcessors.put(Router.class, new RouterAnnotationPostProcessor());
		this.postProcessors.put(Transformer.class, new TransformerAnnotationPostProcessor());
		this.postProcessors.put(ServiceActivator.class, new ServiceActivatorAnnotationPostProcessor());
		this.postProcessors.put(Splitter.class, new SplitterAnnotationPostProcessor());
		this.postProcessors.put(Aggregator.class, new AggregatorAnnotationPostProcessor());
		this.postProcessors.put(InboundChannelAdapter.class, new InboundChannelAdapterAnnotationPostProcessor());
		this.postProcessors.put(BridgeFrom.class, new BridgeFromAnnotationPostProcessor());
		this.postProcessors.put(BridgeTo.class, new BridgeToAnnotationPostProcessor());

		Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> customPostProcessors =
				setupCustomPostProcessors();
		if (!CollectionUtils.isEmpty(customPostProcessors)) {
			this.postProcessors.putAll(customPostProcessors);
		}
		this.postProcessors.values().stream()
				.filter(BeanFactoryAware.class::isInstance)
				.map(BeanFactoryAware.class::cast)
				.forEach((processor) -> processor.setBeanFactory((BeanFactory) this.registry));

		String[] beanNames = registry.getBeanDefinitionNames();

		for (String beanName : beanNames) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (beanDef instanceof AnnotatedBeanDefinition annotatedBeanDefinition
					&& annotatedBeanDefinition.getFactoryMethodMetadata() != null) {

				processCandidate(beanName, annotatedBeanDefinition);
			}
		}
	}

	/**
	 * The factory method for {@link MessagingAnnotationBeanPostProcessor} based
	 * on the environment from this {@link MessagingAnnotationPostProcessor}.
	 * @return the {@link MessagingAnnotationBeanPostProcessor} instance based on {@link #postProcessors}.
	 * @since 6.2
	 */
	public MessagingAnnotationBeanPostProcessor messagingAnnotationBeanPostProcessor() {
		return new MessagingAnnotationBeanPostProcessor(this.postProcessors);
	}

	private void processCandidate(String beanName, AnnotatedBeanDefinition beanDefinition) {
		MethodMetadata methodMetadata = beanDefinition.getFactoryMethodMetadata();
		MergedAnnotations annotations = Objects.requireNonNull(methodMetadata).getAnnotations();
		if (methodMetadata instanceof StandardMethodMetadata standardMethodMetadata) {
			annotations = MergedAnnotations.from(standardMethodMetadata.getIntrospectedMethod());
		}

		List<MessagingAnnotationBeanPostProcessor.MessagingMetaAnnotation> messagingAnnotations =
				MessagingAnnotationBeanPostProcessor.obtainMessagingAnnotations(this.postProcessors.keySet(),
						annotations, beanName);

		for (MessagingAnnotationBeanPostProcessor.MessagingMetaAnnotation messagingAnnotation : messagingAnnotations) {
			Class<? extends Annotation> annotationType = messagingAnnotation.annotationType();
			List<Annotation> annotationChain =
					MessagingAnnotationUtils.getAnnotationChain(messagingAnnotation.annotation(), annotationType);

			processMessagingAnnotationOnBean(beanName, beanDefinition, annotationType, annotationChain);
		}
	}

	private void processMessagingAnnotationOnBean(String beanName, AnnotatedBeanDefinition beanDefinition,
			Class<? extends Annotation> annotationType, List<Annotation> annotationChain) {

		MethodAnnotationPostProcessor<?> messagingAnnotationProcessor = this.postProcessors.get(annotationType);
		if (messagingAnnotationProcessor != null) {
			if (messagingAnnotationProcessor.beanAnnotationAware()) {
				if (messagingAnnotationProcessor.shouldCreateEndpoint(
						Objects.requireNonNull(beanDefinition.getFactoryMethodMetadata()).getAnnotations(), annotationChain)) {
					messagingAnnotationProcessor.processBeanDefinition(beanName, beanDefinition, annotationChain);
				}
				else {
					throw new BeanDefinitionValidationException(
							"The input channel for endpoint on '@Bean' method must be set for the "
									+ annotationType + ". The bean definition with the problem is: " + beanName);
				}
			}
			else {
				throw new BeanDefinitionValidationException(
						"The messaging annotation '" + annotationType + "' cannot be declared on '@Bean'. " +
								"The bean definition with the problem is: " + beanName);
			}
		}
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return this.registry;
	}

	protected @Nullable Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> setupCustomPostProcessors() {
		return null;
	}

	public <A extends Annotation> void addMessagingAnnotationPostProcessor(Class<A> annotation,
			MethodAnnotationPostProcessor<A> postProcessor) {

		this.postProcessors.put(annotation, postProcessor);
	}

	protected Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> getPostProcessors() {
		return this.postProcessors;
	}

}
