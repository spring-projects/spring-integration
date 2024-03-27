/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.expression.Expression;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MethodNameMappingPublisherMetadataSource implements PublisherMetadataSource {

	private final Map<String, Expression> payloadExpressionMap;

	private volatile Map<String, Map<String, Expression>> headerExpressionMap = Collections.emptyMap();

	private volatile Map<String, String> channelMap = Collections.emptyMap();

	public MethodNameMappingPublisherMetadataSource(Map<String, String> payloadExpressionMap) {
		Assert.notEmpty(payloadExpressionMap, "payloadExpressionMap must not be empty");
		this.payloadExpressionMap =
				payloadExpressionMap.entrySet()
						.stream()
						.collect(Collectors.toMap(Map.Entry::getKey,
								e -> EXPRESSION_PARSER.parseExpression(e.getValue())));
	}

	public void setHeaderExpressionMap(Map<String, Map<String, String>> headerExpressionMap) {
		this.headerExpressionMap =
				headerExpressionMap
						.entrySet()
						.stream()
						.collect(Collectors.toMap(Map.Entry::getKey,
								e -> e.getValue()
										.entrySet()
										.stream()
										.collect(Collectors.toMap(Map.Entry::getKey,
												entry -> EXPRESSION_PARSER.parseExpression(entry.getValue())))));
	}

	public void setChannelMap(Map<String, String> channelMap) {
		this.channelMap = channelMap;
	}

	@Override
	public Expression getExpressionForPayload(Method method) {
		for (Map.Entry<String, Expression> entry : this.payloadExpressionMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

	@Override
	public Map<String, Expression> getExpressionsForHeaders(Method method) {
		return this.headerExpressionMap
				.entrySet()
				.stream()
				.filter(e -> PatternMatchUtils.simpleMatch(e.getKey(), method.getName()))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);
	}

	@Override
	public String getChannelName(Method method) {
		for (Map.Entry<String, String> entry : this.channelMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

}
