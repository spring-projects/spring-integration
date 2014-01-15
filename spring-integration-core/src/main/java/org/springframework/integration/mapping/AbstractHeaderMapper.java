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

package org.springframework.integration.mapping;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for HeaderMapper implementations.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public abstract class AbstractHeaderMapper<T> implements RequestReplyHeaderMapper<T> {

	public static final String STANDARD_REQUEST_HEADER_NAME_PATTERN = "STANDARD_REQUEST_HEADERS";

	public static final String STANDARD_REPLY_HEADER_NAME_PATTERN = "STANDARD_REPLY_HEADERS";

	private static final String[] TRANSIENT_HEADER_NAMES = new String[] {
		MessageHeaders.ID,
		MessageHeaders.ERROR_CHANNEL,
		MessageHeaders.REPLY_CHANNEL,
		MessageHeaders.TIMESTAMP
	};

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final String standardHeaderPrefix;

	private volatile String userDefinedHeaderPrefix = "";

	private volatile List<String> requestHeaderNames = new ArrayList<String>();

	private volatile List<String> replyHeaderNames = new ArrayList<String>();

	protected AbstractHeaderMapper() {
		this.standardHeaderPrefix = this.getStandardHeaderPrefix();
		this.requestHeaderNames.addAll(this.getStandardRequestHeaderNames());
		this.replyHeaderNames.addAll(this.getStandardReplyHeaderNames());
	}

	/**
	 * Provide the header names that should be mapped from a request (for inbound/outbound adapters)
	 * TO a Spring Integration Message's headers.
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * <p>
	 * This will match the header name directly or, for non-standard headers, it will match
	 * the header name prefixed with the value, if specified, by {@link #setUserDefinedHeaderPrefix(String)}.
	 *
	 * @param requestHeaderNames The request header names.
	 */
	public void setRequestHeaderNames(String[] requestHeaderNames) {
		Assert.notNull(requestHeaderNames, "'requestHeaderNames' must not be null");
		this.requestHeaderNames = Arrays.asList(requestHeaderNames);
	}

	/**
	 * Provide the header names that should be mapped to a response (for inbound/outbound adapters)
	 * FROM a Spring Integration Message's headers.
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * <p>
	 * Any non-standard headers will be prefixed with the value specified by {@link #setUserDefinedHeaderPrefix(String)}.
	 *
	 * @param replyHeaderNames The reply header names.
	 */
	public void setReplyHeaderNames(String[] replyHeaderNames) {
		Assert.notNull(replyHeaderNames, "'replyHeaderNames' must not be null");
		this.replyHeaderNames = Arrays.asList(replyHeaderNames);
	}

	/**
	 * Specify a prefix to be prepended to the header name for any integration
	 * message header that is being mapped to or from a user-defined value.
	 * <p>
	 * This does not affect the standard properties for the particular protocol, such as
	 * contentType for AMQP, etc. The header names used for mapping such properties are
	 * defined in a corresponding Headers class as constants (e.g. AmqpHeaders).
	 *
	 * @param userDefinedHeaderPrefix The user defined header prefix.
	 */
	public void setUserDefinedHeaderPrefix(String userDefinedHeaderPrefix) {
		this.userDefinedHeaderPrefix = (userDefinedHeaderPrefix != null) ? userDefinedHeaderPrefix : "";
	}

	/**
	 * Maps headers from a Spring Integration MessageHeaders instance to the target instance
	 * matching on the set of REQUEST headers (if different).
	 *
	 * @param headers The headers.
	 * @param target The target.
	 */
	@Override
	public void fromHeadersToRequest(MessageHeaders headers, T target) {
		this.fromHeaders(headers, target, this.requestHeaderNames);
	}
	/**
	 * Maps headers from a Spring Integration MessageHeaders instance to the target instance
	 * matching on the set of REPLY headers (if different).
	 *
	 * @param headers The headers.
	 * @param target The target.
	 */
	@Override
	public void fromHeadersToReply(MessageHeaders headers, T target) {
		this.fromHeaders(headers, target, this.replyHeaderNames);
	}
	/**
	 * Maps headers/properties of the target object to Map of MessageHeaders
	 * matching on the set of REQUEST headers
	 *
	 * @param source The source.
	 * @return The headers.
	 */
	@Override
	public Map<String, Object> toHeadersFromRequest(T source) {
		return this.toHeaders(source, this.requestHeaderNames);
	}
	/**
	 * Maps headers/properties of the target object to Map of MessageHeaders
	 * matching on the set of REPLY headers
	 *
	 * @param source The source.
	 * @return The headers.
	 */
	@Override
	public Map<String, Object> toHeadersFromReply(T source) {
		return this.toHeaders(source, this.replyHeaderNames);
	}

	private void fromHeaders(MessageHeaders headers, T target, List<String> headerPatterns){
		try {
			Map<String, Object> subset = new HashMap<String, Object>();
			for (String headerName : headers.keySet()) {
				if (this.shouldMapHeader(headerName, headerPatterns)){
					subset.put(headerName, headers.get(headerName));
				}
			}
			this.populateStandardHeaders(subset, target);
			this.populateUserDefinedHeaders(subset, target);
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping from MessageHeaders", e);
			}
		}
	}

	private void populateUserDefinedHeaders(Map<String, Object> headers, T target) {
		for (String headerName : headers.keySet()) {
			Object value = headers.get(headerName);
			if (value != null) {
				try {
					if (!headerName.startsWith(this.standardHeaderPrefix)){
						String key = this.addPrefixIfNecessary(this.userDefinedHeaderPrefix, headerName);
						this.populateUserDefinedHeader(key, value, target);
					}
				}
				catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("failed to map from Message header '" + headerName + "' to target", e);
					}
				}
			}
		}
	}

	/**
	 * Maps headers from a source instance to the MessageHeaders of a
	 * Spring Integration Message.
	 */
	private Map<String, Object> toHeaders(T source, List<String> headerPatterns) {
		Map<String, Object> headers = new HashMap<String, Object>();
		Map<String, Object> standardHeaders = this.extractStandardHeaders(source);
		this.copyHeaders(this.standardHeaderPrefix, standardHeaders, headers, headerPatterns);
		Map<String, Object> userDefinedHeaders = this.extractUserDefinedHeaders(source);
		this.copyHeaders(this.userDefinedHeaderPrefix, userDefinedHeaders, headers, headerPatterns);
		return headers;
	}

	private <V> void copyHeaders(String prefix, Map<String, Object> source, Map<String, Object> target, List<String> headerPatterns) {
		if (!CollectionUtils.isEmpty(source)) {
			for (Map.Entry<String, Object> entry : source.entrySet()) {
				try {
					String headerName = this.addPrefixIfNecessary(prefix, entry.getKey());
					if (this.shouldMapHeader(headerName, headerPatterns)){
						target.put(headerName, entry.getValue());
					}
				}
				catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("error occurred while mapping header '"
								+ entry.getKey() + "' to Message header", e);
					}
				}
			}
		}
	}

	private boolean shouldMapHeader(String headerName, List<String> patterns) {
		if (!StringUtils.hasText(headerName)
				|| ObjectUtils.containsElement(TRANSIENT_HEADER_NAMES, headerName)) {
			return false;
		}
		if (patterns != null && patterns.size() > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern.toLowerCase(), headerName.toLowerCase())) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
				else if (STANDARD_REQUEST_HEADER_NAME_PATTERN.equals(pattern)
						&& this.containsElementIgnoreCase(this.getStandardRequestHeaderNames(), headerName)) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
				else if (STANDARD_REPLY_HEADER_NAME_PATTERN.equals(pattern)
						&& this.containsElementIgnoreCase(this.getStandardReplyHeaderNames(), headerName)) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected <V> V getHeaderIfAvailable(Map<String, Object> headers, String name, Class<V> type) {
		Object value = headers.get(name);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			if (logger.isWarnEnabled()) {
				logger.warn("skipping header '" + name + "' since it is not of expected type [" + type + "], it is [" +
						value.getClass() + "]");
			}
			return null;
		}
		else {
			return (V) value;
		}
	}

	private boolean containsElementIgnoreCase(List<String> headerNames, String name) {
		for (String headerName : headerNames) {
			if (headerName.equalsIgnoreCase(name)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds the prefix to the header name
	 */
	private String addPrefixIfNecessary(String prefix, String propertyName) {
		String headerName = propertyName;
		if (StringUtils.hasText(prefix) && !headerName.startsWith(prefix) &&
				!headerName.equals(MessageHeaders.CONTENT_TYPE) &&
				(!JsonHeaders.HEADERS.contains(headerName) || !JsonHeaders.HEADERS.contains(JsonHeaders.PREFIX + headerName))) {
			headerName = prefix + propertyName;
		}
		if (JsonHeaders.HEADERS.contains(JsonHeaders.PREFIX + headerName)) {
			headerName = JsonHeaders.PREFIX + headerName;
		}
		return headerName;
	}

	/**
	 * @return  The list of standard REQUEST headers. Implementation provided by a subclass
	 */
	protected List<String> getStandardReplyHeaderNames(){
		return Collections.emptyList();
	}

	/**
	 * @return The PREFIX used by standard headers (if any)
	 */
	protected List<String> getStandardRequestHeaderNames(){
		return Collections.emptyList();
	}

	/**
	 * @return The list of standard REPLY headers. Implementation provided by a subclass
	 */
	protected abstract String getStandardHeaderPrefix();

	protected abstract Map<String, Object> extractStandardHeaders(T source);

	protected abstract Map<String, Object> extractUserDefinedHeaders(T source);

	protected abstract void populateStandardHeaders(Map<String, Object> headers, T target);

	protected abstract void populateUserDefinedHeader(String headerName, Object headerValue, T target);

}
