/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.aot;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.DecoratingProxy;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.RequestReplyExchanger;

/**
 * {@link BeanRegistrationAotProcessor} for registering proxy interfaces of the {@link GatewayProxyFactoryBean} beans.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class GatewayProxyBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanType = registeredBean.getBeanClass();
		if (GatewayProxyFactoryBean.class.isAssignableFrom(beanType)) {
			GatewayProxyFactoryBean proxyFactoryBean =
					registeredBean.getBeanFactory()
							.getBean(BeanFactory.FACTORY_BEAN_PREFIX + registeredBean.getBeanName(),
									GatewayProxyFactoryBean.class);
			Class<?> serviceInterface = proxyFactoryBean.getObjectType();
			if (!RequestReplyExchanger.class.equals(serviceInterface)) {
				return (generationContext, beanRegistrationCode) ->
						generationContext.getRuntimeHints().proxies()
								.registerJdkProxy(serviceInterface, SpringProxy.class, Advised.class,
										DecoratingProxy.class);
			}
		}
		return null;
	}

}
