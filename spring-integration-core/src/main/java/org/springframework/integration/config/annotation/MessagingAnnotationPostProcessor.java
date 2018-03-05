/*
 * Copyright 2002-2018 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Role;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

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
public class MessagingAnnotationPostProcessor implements BeanPostProcessor, BeanFactoryAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR

	private final Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> postProcessors =
			new HashMap<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>>();

	private ConfigurableListableBeanFactory beanFactory;

	private final Set<Class<?>> noAnnotationsCache =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(256));

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isAssignable(ConfigurableListableBeanFactory.class, beanFactory.getClass(),
				"a ConfigurableListableBeanFactory is required");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	protected ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		this.postProcessors.put(Filter.class, new FilterAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(Router.class, new RouterAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(Transformer.class, new TransformerAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(ServiceActivator.class, new ServiceActivatorAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(Splitter.class, new SplitterAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(Aggregator.class, new AggregatorAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(InboundChannelAdapter.class,
				new InboundChannelAdapterAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(BridgeFrom.class, new BridgeFromAnnotationPostProcessor(this.beanFactory));
		this.postProcessors.put(BridgeTo.class, new BridgeToAnnotationPostProcessor(this.beanFactory));
		Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> customPostProcessors =
				setupCustomPostProcessors();
		if (!CollectionUtils.isEmpty(customPostProcessors)) {
			this.postProcessors.putAll(customPostProcessors);
		}
	}

	protected Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> setupCustomPostProcessors() {
		return null;
	}

	public <A extends Annotation> void addMessagingAnnotationPostProcessor(Class<A> annotation,
			MethodAnnotationPostProcessor<A> postProcessor) {
		this.postProcessors.put(annotation, postProcessor);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");

		Class<?> beanClass = AopUtils.getTargetClass(bean);

		// the set will hold records of prior class scans and indicate if no messaging annotations were found
		if (this.noAnnotationsCache.contains(beanClass)) {
			return bean;
		}

		ReflectionUtils.doWithMethods(beanClass, method -> {
			Map<Class<? extends Annotation>, List<Annotation>> annotationChains = new HashMap<>();
			for (Class<? extends Annotation> annotationType :
					this.postProcessors.keySet()) {
				if (AnnotatedElementUtils.isAnnotated(method, annotationType.getName())) {
					List<Annotation> annotationChain = getAnnotationChain(method, annotationType);
					if (annotationChain.size() > 0) {
						annotationChains.put(annotationType, annotationChain);
					}
				}
			}
			if (StringUtils.hasText(MessagingAnnotationUtils.endpointIdValue(method))
					&& annotationChains.keySet().size() > 1) {
				throw new IllegalStateException("@EndpointId on " + method.toGenericString()
						+ " can only have one EIP annotation, found: " + annotationChains.keySet().size());
			}
			for (Entry<Class<? extends Annotation>, List<Annotation>> entry : annotationChains.entrySet()) {
				Class<? extends Annotation> annotationType = entry.getKey();
				List<Annotation> annotations = entry.getValue();
				processAnnotationTypeOnMethod(bean, beanName, method, annotationType, annotations);
			}

			if (annotationChains.size() == 0) {
				this.noAnnotationsCache.add(beanClass);
			}
		}, ReflectionUtils.USER_DECLARED_METHODS);

		return bean;
	}

	protected void processAnnotationTypeOnMethod(Object bean, String beanName, Method method,
			Class<? extends Annotation> annotationType, List<Annotation> annotations) {
		MethodAnnotationPostProcessor<?> postProcessor =
				MessagingAnnotationPostProcessor.this.postProcessors.get(annotationType);
		if (postProcessor != null && postProcessor.shouldCreateEndpoint(method, annotations)) {
			Method targetMethod = method;
			if (AopUtils.isJdkDynamicProxy(bean)) {
				try {
					targetMethod = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
				}
				catch (NoSuchMethodException e) {
					throw new IllegalArgumentException("Service methods must be extracted to the service "
							+ "interface for JdkDynamicProxy. The affected bean is: '" + beanName + "' "
							+ "and its method: '" + method + "'", e);
				}
			}
			Object result = postProcessor.postProcess(bean, beanName, targetMethod, annotations);
			if (result != null && result instanceof AbstractEndpoint) {
				AbstractEndpoint endpoint = (AbstractEndpoint) result;
				String autoStartup = MessagingAnnotationUtils.resolveAttribute(annotations, "autoStartup",
						String.class);
				if (StringUtils.hasText(autoStartup)) {
					autoStartup = getBeanFactory().resolveEmbeddedValue(autoStartup);
					if (StringUtils.hasText(autoStartup)) {
						endpoint.setAutoStartup(Boolean.parseBoolean(autoStartup));
					}
				}

				String phase = MessagingAnnotationUtils.resolveAttribute(annotations, "phase", String.class);
				if (StringUtils.hasText(phase)) {
					phase = getBeanFactory().resolveEmbeddedValue(phase);
					if (StringUtils.hasText(phase)) {
						endpoint.setPhase(Integer.parseInt(phase));
					}
				}

				Role role = AnnotationUtils.findAnnotation(method, Role.class);
				if (role != null) {
					endpoint.setRole(role.value());
				}

				String endpointBeanName = generateBeanName(beanName, method, annotationType);
				endpoint.setBeanName(endpointBeanName);
				getBeanFactory().registerSingleton(endpointBeanName, endpoint);
				getBeanFactory().initializeBean(endpoint, endpointBeanName);
			}
		}
	}

	/**
	 * @param method         the method.
	 * @param annotationType the annotation type.
	 * @return the hierarchical list of annotations in top-bottom order.
	 */
	protected List<Annotation> getAnnotationChain(Method method, Class<? extends Annotation> annotationType) {
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		List<Annotation> annotationChain = new LinkedList<Annotation>();
		Set<Annotation> visited = new HashSet<Annotation>();
		for (Annotation ann : annotations) {
			recursiveFindAnnotation(annotationType, ann, annotationChain, visited);
			if (annotationChain.size() > 0) {
				Collections.reverse(annotationChain);
				return annotationChain;
			}
		}
		return annotationChain;
	}

	protected boolean recursiveFindAnnotation(Class<? extends Annotation> annotationType, Annotation ann,
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

	protected String generateBeanName(String originalBeanName, Method method,
			Class<? extends Annotation> annotationType) {
		String name = MessagingAnnotationUtils.endpointIdValue(method);
		if (!StringUtils.hasText(name)) {
			String baseName = originalBeanName + "." + method.getName() + "."
					+ ClassUtils.getShortNameAsProperty(annotationType);
			name = baseName;
			int count = 1;
			while (this.beanFactory.containsBean(name)) {
				name = baseName + "#" + (++count);
			}
		}
		return name;
	}

	protected Map<Class<? extends Annotation>, MethodAnnotationPostProcessor<?>> getPostProcessors() {
		return this.postProcessors;
	}

}
