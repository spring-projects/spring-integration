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
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;
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

		String maxRetries = element.getAttribute("max-retries");

		// Default to 3 attempts and no delay in between.
		BeanDefinitionBuilder backOffBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(FixedBackOff.class)
						.addConstructorArgValue(0)
						.addConstructorArgValue(maxRetries);

		Element backOffPolicyEle = DomUtils.getChildElementByTagName(element, "fixed-back-off");
		if (backOffPolicyEle != null) {
			backOffBuilder = BeanDefinitionBuilder.genericBeanDefinition(FixedBackOff.class)
					.addConstructorArgValue(backOffPolicyEle.getAttribute("interval"))
					.addConstructorArgValue(maxRetries);
		}
		else {
			backOffPolicyEle = DomUtils.getChildElementByTagName(element, "exponential-back-off");
			if (backOffPolicyEle != null) {
				backOffBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExponentialBackOff.class)
						.addPropertyValue("maxAttempts", maxRetries)
						.addPropertyValue("multiplier", backOffPolicyEle.getAttribute("multiplier"))
						.addPropertyValue("initialInterval", backOffPolicyEle.getAttribute("initial"))
						.addPropertyValue("maxInterval", backOffPolicyEle.getAttribute("maximum"))
						.addPropertyValue("jitter", backOffPolicyEle.getAttribute("jitter"));
			}
		}

		builder.addPropertyValue("backOff", backOffBuilder.getBeanDefinition());
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

}
