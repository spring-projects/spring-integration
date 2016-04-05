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
import java.util.Map;

/**
 * Simple implementation of {@link PublisherMetadataSource} that allows for
 * configuration of a single channel name, payload expression, and
 * array of header key=value expressions.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class SimplePublisherMetadataSource implements PublisherMetadataSource {

	private volatile String channelName;

	private volatile String payloadExpression;

	private volatile Map<String, String> headerExpressions;


	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getChannelName(Method method) {
		return this.channelName;
	}

	public void setPayloadExpression(String payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	public String getPayloadExpression(Method method) {
		return this.payloadExpression;
	}

	public void setHeaderExpressions(Map<String, String> headerExpressions) {
		this.headerExpressions = headerExpressions;
	}

	public Map<String, String> getHeaderExpressions(Method method) {
		return this.headerExpressions;
	}

}
