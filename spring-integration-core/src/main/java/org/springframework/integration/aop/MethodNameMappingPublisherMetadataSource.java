/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodNameMappingPublisherMetadataSource implements PublisherMetadataSource {

	private final Map<String, String> payloadExpressionMap;

	private volatile Map<String, Map<String, String>> headerExpressionMap = Collections.emptyMap();

	private volatile Map<String, String> channelMap = Collections.emptyMap();


	public MethodNameMappingPublisherMetadataSource(Map<String, String> payloadExpressionMap) {
		Assert.notEmpty(payloadExpressionMap, "payloadExpressionMap must not be empty");
		this.payloadExpressionMap = payloadExpressionMap;
	}


	public void setHeaderExpressionMap(Map<String, Map<String, String>> headerExpressionMap) {
		this.headerExpressionMap = headerExpressionMap;
	}

	public void setChannelMap(Map<String, String> channelMap) {
		this.channelMap = channelMap;
	}

	public String getPayloadExpression(Method method) {
		for (Map.Entry<String, String> entry : this.payloadExpressionMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public Map<String, String> getHeaderExpressions(Method method) {
		for (Map.Entry<String, Map<String, String>> entry : this.headerExpressionMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public String getChannelName(Method method) {
		for (Map.Entry<String, String> entry : this.channelMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

}
