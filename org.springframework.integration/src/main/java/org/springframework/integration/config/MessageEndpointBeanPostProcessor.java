/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.message.Message;

/**
 * A post-processor that applies an advice-chain by creating a proxy for an endpoint.
 *
 * @author Mark Fisher
 */
public class MessageEndpointBeanPostProcessor implements BeanPostProcessor {

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AbstractEndpoint) {
			AbstractEndpoint endpoint = (AbstractEndpoint) bean;
			List<Advice> adviceChain = endpoint.getAdviceChain();
			if (adviceChain.size() > 0) {
				ProxyFactory proxyFactory = new ProxyFactory(endpoint);
				for (Advice advice : adviceChain) {
					proxyFactory.addAdvisor(new EndpointInvokeMethodAdvisor(advice));
				}
				bean = proxyFactory.getProxy();
			}
		}
		return bean;
	}


	@SuppressWarnings("serial")
	private static class EndpointInvokeMethodAdvisor extends StaticMethodMatcherPointcutAdvisor {

		EndpointInvokeMethodAdvisor(Advice advice) {
			super(advice);
		}


		@SuppressWarnings("unchecked")
		public boolean matches(Method method, Class clazz) {
			return method.getName().equals("invoke")
					&& method.getParameterTypes().length == 1
					&& method.getParameterTypes()[0].equals(Message.class);
		}

	}

}
