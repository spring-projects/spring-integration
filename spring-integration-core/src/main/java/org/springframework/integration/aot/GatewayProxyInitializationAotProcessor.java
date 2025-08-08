/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
