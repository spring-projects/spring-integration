/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.aggregator;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link CorrelationStrategy}.
 * Uses a provided header attribute to determine the correlation key value.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class HeaderAttributeCorrelationStrategy implements CorrelationStrategy {

	private final String attributeName;


	public HeaderAttributeCorrelationStrategy(String attributeName) {
		Assert.hasText(attributeName, "the 'attributeName' must not be empty");
		this.attributeName = attributeName;
	}

	public Object getCorrelationKey(Message<?> message) {
		return message.getHeaders().get(this.attributeName);
	}

}
