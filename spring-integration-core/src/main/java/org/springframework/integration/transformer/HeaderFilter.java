/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Transformer that removes Message headers.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class HeaderFilter extends IntegrationObjectSupport implements Transformer {

	private final String[] headersToRemove;

	private volatile boolean patternMatch = true;


	public HeaderFilter(String... headersToRemove) {
		Assert.notEmpty(headersToRemove, "At least one header name to remove is required.");
		this.headersToRemove = headersToRemove;
	}

	public void setPatternMatch(boolean patternMatch) {
		this.patternMatch = patternMatch;
	}

	@Override
	public String getComponentType() {
		return "header-filter";
	}

	@Override
	public Message<?> transform(Message<?> message) {
		AbstractIntegrationMessageBuilder<?> builder = this.getMessageBuilderFactory().fromMessage(message);
		if (this.patternMatch){
			builder.removeHeaders(headersToRemove);
		}
		else {
			for (String headerToRemove : headersToRemove) {
				builder.removeHeader(headerToRemove);
			}
		}
		return builder.build();
	}

}
