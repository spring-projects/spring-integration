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

package org.springframework.integration.gateway;

import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

/**
 * Represents the metadata associated with a Gateway method. This is most useful when there are
 * multiple methods per Gateway interface.
 * <p>
 * The sub-element of a &lt;gateway&gt; element would look like this:
 * &lt;method name="echo" request-channel="inputA" reply-timeout="2" request-timeout="200"/&gt;
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class GatewayMethodMetadata {

	private final Map<String, Expression> headerExpressions = new HashMap<>();

	private Expression payloadExpression;

	private String requestChannelName;

	private String replyChannelName;

	private String requestTimeout;

	private String replyTimeout;

	@Nullable
	public Expression getPayloadExpression() {
		return this.payloadExpression;
	}

	public void setPayloadExpression(@Nullable Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	public Map<String, Expression> getHeaderExpressions() {
		return this.headerExpressions;
	}

	public void setHeaderExpressions(@Nullable Map<String, Expression> headerExpressions) {
		this.headerExpressions.clear();
		if (headerExpressions != null) {
			this.headerExpressions.putAll(headerExpressions);
		}
	}

	public String getRequestChannelName() {
		return this.requestChannelName;
	}

	public void setRequestChannelName(String requestChannelName) {
		this.requestChannelName = requestChannelName;
	}

	public String getReplyChannelName() {
		return this.replyChannelName;
	}

	public void setReplyChannelName(String replyChannelName) {
		this.replyChannelName = replyChannelName;
	}

	@Nullable
	public String getRequestTimeout() {
		return this.requestTimeout;
	}

	public void setRequestTimeout(@Nullable String requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	@Nullable
	public String getReplyTimeout() {
		return this.replyTimeout;
	}

	public void setReplyTimeout(@Nullable String replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

}
