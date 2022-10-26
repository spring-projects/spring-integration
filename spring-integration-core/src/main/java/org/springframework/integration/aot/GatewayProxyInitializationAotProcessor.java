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

import java.util.Arrays;
import java.util.List;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.RequestReplyExchanger;

/**
 * A {@link BeanFactoryInitializationAotProcessor} for registering proxy interfaces
 * of the {@link GatewayProxyFactoryBean} beans.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class GatewayProxyInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		List<? extends Class<?>> gatewayProxyInterfaces =
				Arrays.stream(beanFactory.getBeanDefinitionNames())
						.map((beanName) -> RegisteredBean.of(beanFactory, beanName))
						.filter((bean) -> ProxyFactoryBean.class.isAssignableFrom(bean.getBeanClass()))
						.map((bean) -> bean.getBeanType().getGeneric(0).resolve(RequestReplyExchanger.class))
						.toList();

		return (generationContext, beanFactoryInitializationCode) -> {
			ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
			gatewayProxyInterfaces.forEach((gatewayInterface) ->
					proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(gatewayInterface)));
		};
	}

}
