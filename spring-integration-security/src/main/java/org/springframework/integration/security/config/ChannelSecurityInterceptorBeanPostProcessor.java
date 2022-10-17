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

package org.springframework.integration.security.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.security.channel.ChannelAccessPolicy;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.ChannelSecurityMetadataSource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link org.springframework.beans.factory.config.BeanPostProcessor} that proxies
 * {@link MessageChannel}s to apply a {@link ChannelSecurityInterceptor}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell

 * @deprecated since 6.0 in favor of literally
 * {@code new AuthorizationChannelInterceptor(AuthorityAuthorizationManager.hasAnyRole())}
 */
@Deprecated(since = "6.0")
@SuppressWarnings("serial")
public class ChannelSecurityInterceptorBeanPostProcessor extends AbstractAutoProxyCreator {

	private final Map<String, Set<Pattern>> securityInterceptorMappings;

	private final Map<String, Map<Pattern, ChannelAccessPolicy>> accessPolicyMapping;

	public ChannelSecurityInterceptorBeanPostProcessor(Map<String, Set<Pattern>> securityInterceptorMappings) {
		this(securityInterceptorMappings, null);
	}

	public ChannelSecurityInterceptorBeanPostProcessor(Map<String, Set<Pattern>> securityInterceptorMappings,
			Map<String, Map<Pattern, ChannelAccessPolicy>> accessPolicyMapping) {

		this.securityInterceptorMappings = securityInterceptorMappings; //NOSONAR (inconsistent sync)
		this.accessPolicyMapping = accessPolicyMapping; //NOSONAR (inconsistent sync)
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		if (this.accessPolicyMapping != null
				&& bean instanceof ChannelSecurityInterceptor
				&& this.accessPolicyMapping.containsKey(beanName)) {
			Map<Pattern, ChannelAccessPolicy> accessPolicies = this.accessPolicyMapping.get(beanName);
			ChannelSecurityMetadataSource securityMetadataSource =
					(ChannelSecurityMetadataSource) ((ChannelSecurityInterceptor) bean).obtainSecurityMetadataSource();
			for (Map.Entry<Pattern, ChannelAccessPolicy> entry : accessPolicies.entrySet()) {
				securityMetadataSource.addPatternMapping(entry.getKey(), entry.getValue());
			}
		}
		return bean;
	}

	@Override
	@Nullable
	protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
			@Nullable TargetSource customTargetSource) throws BeansException {

		if (MessageChannel.class.isAssignableFrom(beanClass)) {
			List<Advisor> interceptors = new ArrayList<>();
			for (Map.Entry<String, Set<Pattern>> entry : this.securityInterceptorMappings.entrySet()) {
				if (isMatch(beanName, entry.getValue())) {
					DefaultBeanFactoryPointcutAdvisor channelSecurityInterceptor =
							new DefaultBeanFactoryPointcutAdvisor();
					channelSecurityInterceptor.setAdviceBeanName(entry.getKey());
					BeanFactory beanFactory = getBeanFactory();
					if (beanFactory != null) {
						channelSecurityInterceptor.setBeanFactory(beanFactory);
					}
					interceptors.add(channelSecurityInterceptor);
				}
			}
			if (!interceptors.isEmpty()) {
				return interceptors.toArray();
			}
		}

		return DO_NOT_PROXY;
	}

	private boolean isMatch(String beanName, Set<Pattern> patterns) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(beanName).matches()) {
				return true;
			}
		}
		return false;
	}

}
