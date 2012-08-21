/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.condition.*;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} implementation that
 * detects and registers {@link RequestMappingInfo}s for {@link HttpRequestHandlingEndpointSupport}
 * from a Spring Integration HTTP configuration of
 * &lt;inbound-channel-adapter&gt; or &lt;inbound-gateway&gt; element.
 *
 * @author Artem Bilan
 *
 * @see RequestMappingHandlerMapping
 * @since 3.0
 */
public class IntegrationRequestMappingHandlerMapping extends AbstractHandlerMapping
		implements InitializingBean, ApplicationListener<ContextRefreshedEvent> {

	private static final Method HANDLE_REQUEST_METHOD = ReflectionUtils.findMethod(HttpRequestHandler.class,
			"handleRequest", HttpServletRequest.class, HttpServletResponse.class);

	private final HandlerMappingBridge delegate = new HandlerMappingBridge();

	private boolean useSuffixPatternMatch = true;

	private boolean useTrailingSlashMatch = true;

	/**
	 * @see RequestMappingHandlerMapping#setUseSuffixPatternMatch
	 */
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * @see RequestMappingHandlerMapping#setUseTrailingSlashMatch
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
	}

	/**
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#setDetectHandlerMethodsInAncestorContexts
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.delegate.setDetectHandlerMethodsInAncestorContexts(detectHandlerMethodsInAncestorContexts);
	}

	/**
	 * @see RequestMappingHandlerMapping#useSuffixPatternMatch
	 */
	public boolean useSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	/**
	 * @see RequestMappingHandlerMapping#useTrailingSlashMatch
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		final HandlerMethod handlerMethod = this.delegate.getHandlerInternal(request);
		return handlerMethod != null ? handlerMethod.getBean() : null;
	}

	/**
	 * Initialize {@link #delegate}, since it is out of Application Context
	 */
	public void afterPropertiesSet() throws Exception {
		this.delegate.setApplicationContext(this.getApplicationContext());
		this.delegate.setPathMatcher(this.getPathMatcher());
		this.delegate.setUrlPathHelper(this.getUrlPathHelper());
	}

	/**
	 * Handles {@link ContextRefreshedEvent} to invoke {@link #delegate#afterPropertiesSet()}
	 * as late as possible after application context startup.
	 * Also it checks 'event.getApplicationContext()' to ignore
	 * other {@link ContextRefreshedEvent}s which may be published
	 * in the 'parent-child' contexts, e.g. in the Spring-MVC applications.
	 *
	 * @param event - {@link ContextRefreshedEvent} which occurs
	 *              after Application context is completely initialized.
	 * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#afterPropertiesSet()
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.getApplicationContext().equals(event.getApplicationContext())) {
			this.delegate.afterPropertiesSet();
		}
	}

	/**
	 * An internal bridge {@link RequestMappingInfoHandlerMapping} implementation.
	 */
	private class HandlerMappingBridge extends RequestMappingInfoHandlerMapping {

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return HttpRequestHandlingEndpointSupport.class.isAssignableFrom(beanType);
		}

		@Override
		protected void detectHandlerMethods(Object handler) {
			if (handler instanceof String) {
				handler = this.getApplicationContext().getBean((String) handler);
			}
			RequestMappingInfo mapping = this.getMappingForEndpoint((HttpRequestHandlingEndpointSupport) handler);
			this.registerHandlerMethod(handler, HANDLE_REQUEST_METHOD, mapping);
		}

		@Override
		protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
			return super.getHandlerInternal(request);
		}

		/**
		 * Created a {@link RequestMappingInfo} from a 'Spring Integration HTTP Inbound Endpoint' {@link RequestMapping}.
		 *
		 * @see RequestMappingHandlerMapping#getMappingForMethod
		 */
		private RequestMappingInfo getMappingForEndpoint(HttpRequestHandlingEndpointSupport endpoint) {
			RequestMapping requestMapping = endpoint.getRequestMapping();
			return new RequestMappingInfo(
					new PatternsRequestCondition(requestMapping.getPathPatterns(),
							this.getUrlPathHelper(),
							this.getPathMatcher(),
							IntegrationRequestMappingHandlerMapping.this.useSuffixPatternMatch(),
							IntegrationRequestMappingHandlerMapping.this.useTrailingSlashMatch()),
					new RequestMethodsRequestCondition(requestMapping.getRequestMethods()),
					new ParamsRequestCondition(requestMapping.getParams()),
					new HeadersRequestCondition(requestMapping.getHeaders()),
					new ConsumesRequestCondition(requestMapping.getConsumes(), requestMapping.getHeaders()),
					new ProducesRequestCondition(requestMapping.getProduces(), requestMapping.getHeaders()),
					null);
		}

		/**
		 * No-op
		 */
		@Override
		protected final RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			return null;
		}

	}

}
