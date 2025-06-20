/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public class RetryAdviceParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RequestHandlerRetryAdvice.class);
		BeanDefinitionBuilder retryTemplateBuilder = BeanDefinitionBuilder.genericBeanDefinition(RetryTemplate.class);
		boolean customTemplate = false;
		Element backOffPolicyEle = DomUtils.getChildElementByTagName(element, "fixed-back-off");
		BeanDefinitionBuilder backOffBuilder = null;
		if (backOffPolicyEle != null) {
			backOffBuilder = BeanDefinitionBuilder.genericBeanDefinition(ParsedFixedBackOffPolicy.class);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(
					backOffBuilder, backOffPolicyEle, "interval", "backOffPeriodSimple");
		}
		else {
			backOffPolicyEle = DomUtils.getChildElementByTagName(element, "exponential-back-off");
			if (backOffPolicyEle != null) {
				backOffBuilder = BeanDefinitionBuilder.genericBeanDefinition(ParsedExponentialBackOffPolicy.class);
				IntegrationNamespaceUtils.setValueIfAttributeDefined(backOffBuilder, backOffPolicyEle, "initial",
						"initialIntervalSimple");
				IntegrationNamespaceUtils.setValueIfAttributeDefined(backOffBuilder, backOffPolicyEle,
						"multiplier", "multiplierSimple");
				IntegrationNamespaceUtils.setValueIfAttributeDefined(backOffBuilder, backOffPolicyEle, "maximum",
						"maxIntervalSimple");
			}
		}
		if (backOffBuilder != null) {
			retryTemplateBuilder.addPropertyValue("backOffPolicy", backOffBuilder.getBeanDefinition());
			customTemplate = true;
		}
		String maxAttemptsAttr = element.getAttribute("max-attempts");
		if (StringUtils.hasText(maxAttemptsAttr)) {
			BeanDefinitionBuilder retryPolicyBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(SimpleRetryPolicy.class)
							.addConstructorArgValue(element.getAttribute("max-attempts"));
			retryTemplateBuilder.addPropertyValue("retryPolicy", retryPolicyBuilder.getBeanDefinition());
			customTemplate = true;
		}
		if (customTemplate) {
			builder.addPropertyValue("retryTemplate", retryTemplateBuilder.getBeanDefinition());
		}
		String recoveryChannelAttr = element.getAttribute("recovery-channel");
		if (StringUtils.hasText(recoveryChannelAttr)) {
			BeanDefinitionBuilder emsrBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ErrorMessageSendingRecoverer.class);
			emsrBuilder.addConstructorArgReference(recoveryChannelAttr);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(emsrBuilder, element, "send-timeout");
			builder.addPropertyValue("recoveryCallback", emsrBuilder.getBeanDefinition());
		}
		return builder.getBeanDefinition();
	}

	private static final class ParsedFixedBackOffPolicy extends FixedBackOffPolicy {

		ParsedFixedBackOffPolicy() {
		}

		public void setBackOffPeriodSimple(long backOffPeriod) {
			setBackOffPeriod(backOffPeriod);
		}

	}

	private static final class ParsedExponentialBackOffPolicy extends ExponentialBackOffPolicy {

		ParsedExponentialBackOffPolicy() {
		}

		public void setInitialIntervalSimple(long initialInterval) {
			setInitialInterval(initialInterval);
		}

		public void setMultiplierSimple(double multiplier) {
			setMultiplier(multiplier);
		}

		public void setMaxIntervalSimple(long maxInterval) {
			setMaxInterval(maxInterval);
		}

	}

}
