/*
 * Copyright 2022-2024 the original author or authors.
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
import java.util.stream.Stream;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;

/**
 * A {@link BeanFactoryInitializationAotProcessor} for registering proxy interfaces
 * of the {@link GatewayProxyFactoryBean} beans or {@link MessagingGateway} interfaces.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class GatewayProxyInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		Stream<? extends Class<?>> gatewayProxyInterfaces =
				Arrays.stream(beanFactory.getBeanDefinitionNames())
						.map((beanName) -> RegisteredBean.of(beanFactory, beanName))
						.filter((bean) -> GatewayProxyFactoryBean.class.isAssignableFrom(bean.getBeanClass()))
						.flatMap((bean) -> Stream.ofNullable(bean.getBeanType().getGeneric(0).resolve()));

		return (generationContext, beanFactoryInitializationCode) -> {
			ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
			gatewayProxyInterfaces.forEach((gatewayInterface) ->
					proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(gatewayInterface)));
		};
	}

}
