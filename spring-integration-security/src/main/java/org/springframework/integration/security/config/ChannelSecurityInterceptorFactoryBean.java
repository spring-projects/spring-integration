/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.security.config;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.ChannelSecurityMetadataSource;
import org.springframework.integration.security.channel.DefaultChannelAccessPolicy;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.intercept.AfterInvocationManager;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

/**
 * The {@link FactoryBean} for {@code <security:secured-channels/>} JavaConfig variant to provide options
 * for {@link ChannelSecurityInterceptor} beans.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class ChannelSecurityInterceptorFactoryBean implements FactoryBean<ChannelSecurityInterceptor>, BeanNameAware, BeanFactoryAware {

	private final ChannelSecurityInterceptor interceptor = new ChannelSecurityInterceptor(new ChannelSecurityMetadataSource());

	private BeanFactory beanFactory;

	private String name;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		if (this.interceptor.getAuthenticationManager() == null && beanFactory.containsBean("authenticationManager")) {
			this.interceptor.setAuthenticationManager(beanFactory.getBean("authenticationManager", AuthenticationManager.class));
		}
		if (this.interceptor.getAccessDecisionManager() == null && beanFactory.containsBean("accessDecisionManager")) {
			this.interceptor.setAccessDecisionManager(beanFactory.getBean("accessDecisionManager", AccessDecisionManager.class));
		}
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	public ChannelSecurityInterceptorFactoryBean setAccessDecisionManager(AccessDecisionManager accessDecisionManager) {
		interceptor.setAccessDecisionManager(accessDecisionManager);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setAfterInvocationManager(AfterInvocationManager afterInvocationManager) {
		interceptor.setAfterInvocationManager(afterInvocationManager);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setAlwaysReauthenticate(boolean alwaysReauthenticate) {
		interceptor.setAlwaysReauthenticate(alwaysReauthenticate);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setAuthenticationManager(AuthenticationManager newManager) {
		interceptor.setAuthenticationManager(newManager);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setPublishAuthorizationSuccess(boolean publishAuthorizationSuccess) {
		interceptor.setPublishAuthorizationSuccess(publishAuthorizationSuccess);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setRejectPublicInvocations(boolean rejectPublicInvocations) {
		interceptor.setRejectPublicInvocations(rejectPublicInvocations);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setRunAsManager(RunAsManager runAsManager) {
		interceptor.setRunAsManager(runAsManager);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setValidateConfigAttributes(boolean validateConfigAttributes) {
		interceptor.setValidateConfigAttributes(validateConfigAttributes);
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean accessPolicy(String pattern, String sendAccess) {
		return this.accessPolicy(pattern, sendAccess, null);
	}

	public ChannelSecurityInterceptorFactoryBean accessPolicy(String pattern, String sendAccess, String receiveAccess) {
		Assert.hasText(pattern);
		((ChannelSecurityMetadataSource) interceptor.obtainSecurityMetadataSource())
				.addPatternMapping(Pattern.compile(pattern), new DefaultChannelAccessPolicy(sendAccess, receiveAccess));
		return this;
	}

	public ChannelSecurityInterceptorFactoryBean setAccessPolicies(Map<String, DefaultChannelAccessPolicy> accessPolicies) {
		Assert.notNull(accessPolicies);
		ChannelSecurityMetadataSource channelSecurityMetadataSource = (ChannelSecurityMetadataSource) interceptor.obtainSecurityMetadataSource();
		for (Map.Entry<String, DefaultChannelAccessPolicy> entry : accessPolicies.entrySet()) {
			channelSecurityMetadataSource.addPatternMapping(Pattern.compile(entry.getKey()), entry.getValue());
		}
		return this;
	}

	@Override
	public ChannelSecurityInterceptor getObject() throws Exception {
		((AutowireCapableBeanFactory) this.beanFactory).initializeBean(this.interceptor, this.name);
		return this.interceptor;
	}

	@Override
	public Class<?> getObjectType() {
		return ChannelSecurityInterceptor.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
