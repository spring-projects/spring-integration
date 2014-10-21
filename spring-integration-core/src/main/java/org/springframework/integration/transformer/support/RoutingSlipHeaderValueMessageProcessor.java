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

package org.springframework.integration.transformer.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class RoutingSlipHeaderValueMessageProcessor extends AbstractHeaderValueMessageProcessor<Map<List<String>, Integer>> {

	private final Map<List<String>, Integer> routingSlip;

	public RoutingSlipHeaderValueMessageProcessor(String... routingSlipPath) {
		Assert.noNullElements(routingSlipPath);
		this.routingSlip = Collections.singletonMap(Collections.unmodifiableList(Arrays.asList(routingSlipPath)), 0);
	}

	@Override
	public Map<List<String>, Integer> processMessage(Message<?> message) {
		return this.routingSlip;
	}

}
