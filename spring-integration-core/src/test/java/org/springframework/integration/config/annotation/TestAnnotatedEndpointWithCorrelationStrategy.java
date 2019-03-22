/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ReleaseStrategy;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@MessageEndpoint("endpointWithCorrelationStrategy")
public class TestAnnotatedEndpointWithCorrelationStrategy {

	@Aggregator(inputChannel = "inputChannel")
	public String aggregatingMethod(List<String> payloads) {
		return payloads.stream()
				.collect(Collectors.joining());
	}

	@ReleaseStrategy
	public boolean isComplete(List<String> payloads) {
		return payloads.size() == 3;
	}

	@CorrelationStrategy
	public String correlate(String payload) {
		return payload.substring(0, 1);
	}

}
