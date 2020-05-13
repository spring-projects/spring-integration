/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.mapping;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.ResolvableType;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link RequestReplyHeaderMapper} implementations.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Stephane Nicoll
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public abstract class AbstractHeaderMapper<T> implements RequestReplyHeaderMapper<T>, BeanClassLoaderAware {

	/**
	 * A special pattern that only matches standard request headers.
	 */
	public static final String STANDARD_REQUEST_HEADER_NAME_PATTERN = "STANDARD_REQUEST_HEADERS";

	/**
	 * A special pattern that only matches standard reply headers.
	 */
	public static final String STANDARD_REPLY_HEADER_NAME_PATTERN = "STANDARD_REPLY_HEADERS";

	/**
	 * A special pattern that matches any header that is not a standard header (i.e. any
	 * header that does not start with the configured standard header prefix)
	 */
	public static final String NON_STANDARD_HEADER_NAME_PATTERN = "NON_STANDARD_HEADERS";

	private static final Collection<String> TRANSIENT_HEADER_NAMES =
			Arrays.asList(MessageHeaders.ID, MessageHeaders.TIMESTAMP);

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	private final String standardHeaderPrefix;

	private final Collection<String> requestHeaderNames;

	private final Collection<String> replyHeaderNames;

	private HeaderMatcher requestHeaderMatcher;

	private HeaderMatcher replyHeaderMatcher;

	private ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * Create a new instance.
	 * @param standardHeaderPrefix the header prefix that identifies standard header. Such prefix helps to
	 * differentiate user-defined headers from standard headers. If set, user-defined headers are also
	 * mapped by default
	 * @param requestHeaderNames the header names that should be mapped from a request to {@link MessageHeaders}
	 * @param replyHeaderNames the header names that should be mapped to a response from {@link MessageHeaders}
	 */
	protected AbstractHeaderMapper(String standardHeaderPrefix,
			Collection<String> requestHeaderNames, Collection<String> replyHeaderNames) {

		this.standardHeaderPrefix = standardHeaderPrefix;
		this.requestHeaderNames = requestHeaderNames;
		this.replyHeaderNames = replyHeaderNames;
		this.requestHeaderMatcher = createDefaultHeaderMatcher(this.standardHeaderPrefix, this.requestHeaderNames);
		this.replyHeaderMatcher = createDefaultHeaderMatcher(this.standardHeaderPrefix, this.replyHeaderNames);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	protected ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Provide the header names that should be mapped from a request
	 * to a {@link MessageHeaders}.
	 * <p>The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * @param requestHeaderNames The request header names.
	 */
	public void setRequestHeaderNames(String... requestHeaderNames) {
		Assert.notNull(requestHeaderNames, "'requestHeaderNames' must not be null");
		this.requestHeaderMatcher = createHeaderMatcher(Arrays.asList(requestHeaderNames));
	}

	/**
	 * Provide the header names that should be mapped to a response
	 * from a {@link MessageHeaders}.
	 * <p>The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * @param replyHeaderNames The reply header names.
	 */
	public void setReplyHeaderNames(String... replyHeaderNames) {
		Assert.notNull(replyHeaderNames, "'replyHeaderNames' must not be null");
		this.replyHeaderMatcher = createHeaderMatcher(Arrays.asList(replyHeaderNames));
	}

	/**
	 * Create the initial {@link HeaderMatcher} based on the specified headers and
	 * standard header prefix.
	 * @param standardHeaderPrefix the prefix for standard headers.
	 * @param headerNames the collection of header names to map.
	 * @return the default {@link HeaderMatcher} instance.
	 */
	protected HeaderMatcher createDefaultHeaderMatcher(String standardHeaderPrefix, Collection<String> headerNames) {
		return new ContentBasedHeaderMatcher(true, headerNames);
	}

	/**
	 * Create a {@link HeaderMatcher} that match if any of the specified {@code patterns}
	 * match. The pattern can be a header name, a wildcard pattern such as
	 * {@code foo*}, {@code *foo}, or {@code within*foo}.
	 * <p>Special patterns are also recognized: {@link #STANDARD_REQUEST_HEADER_NAME_PATTERN},
	 * {@link #STANDARD_REQUEST_HEADER_NAME_PATTERN} and {@link #NON_STANDARD_HEADER_NAME_PATTERN}.
	 * @param patterns the patterns to apply
	 * @return a header mapper that match if any of the specified patters match
	 */
	protected HeaderMatcher createHeaderMatcher(Collection<String> patterns) {
		List<HeaderMatcher> matchers = new ArrayList<>();
		for (String pattern : patterns) {
			if (STANDARD_REQUEST_HEADER_NAME_PATTERN.equals(pattern)) {
				matchers.add(new ContentBasedHeaderMatcher(true, this.requestHeaderNames));
			}
			else if (STANDARD_REPLY_HEADER_NAME_PATTERN.equals(pattern)) {
				matchers.add(new ContentBasedHeaderMatcher(true, this.replyHeaderNames));
			}
			else if (NON_STANDARD_HEADER_NAME_PATTERN.equals(pattern)) {
				matchers.add(new PrefixBasedMatcher(false, this.standardHeaderPrefix));
			}
			else {
				String thePattern = pattern;
				boolean negate = false;
				if (pattern.startsWith("!")) {
					thePattern = pattern.substring(1);
					negate = true;
				}
				else if (pattern.startsWith("\\!")) {
					thePattern = pattern.substring(1);
				}
				if (negate) {
					// negative matchers get priority
					matchers.add(0, new SinglePatternBasedHeaderMatcher(thePattern, negate));
				}
				else {
					matchers.add(new SinglePatternBasedHeaderMatcher(thePattern, negate));
				}
			}
		}
		return new CompositeHeaderMatcher(matchers);
	}

	@Override
	public void fromHeadersToRequest(MessageHeaders headers, T target) {
		fromHeaders(headers, target, this.requestHeaderMatcher);
	}

	@Override
	public void fromHeadersToReply(MessageHeaders headers, T target) {
		fromHeaders(headers, target, this.replyHeaderMatcher);
	}

	@Override
	public Map<String, Object> toHeadersFromRequest(T source) {
		return toHeaders(source, this.requestHeaderMatcher);
	}

	@Override
	public Map<String, Object> toHeadersFromReply(T source) {
		return toHeaders(source, this.replyHeaderMatcher);
	}

	private void fromHeaders(MessageHeaders headers, T target, HeaderMatcher headerMatcher) {
		try {
			Map<String, Object> subset = new HashMap<>();
			for (Map.Entry<String, Object> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				if (shouldMapHeader(headerName, headerMatcher)) {
					subset.put(headerName, entry.getValue());
				}
			}
			populateStandardHeaders(headers, subset, target);
			populateUserDefinedHeaders(subset, target);
		}
		catch (Exception e) {
			this.logger.warn("error occurred while mapping from MessageHeaders", e);
		}
	}

	private void populateUserDefinedHeaders(Map<String, Object> headers, T target) {
		for (Entry<String, Object> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			Object value = entry.getValue();
			if (value != null && !isMessageChannel(headerName, value)) {
				try {
					if (!headerName.startsWith(this.standardHeaderPrefix)) {
						String key = createTargetPropertyName(headerName, true);
						populateUserDefinedHeader(key, value, target);
					}
				}
				catch (Exception e) {
					if (this.logger.isWarnEnabled()) {
						this.logger.warn("failed to map from Message header '" + headerName + "' to target", e);
					}
				}
			}
		}
	}

	private boolean isMessageChannel(String headerName, Object headerValue) {
		if (headerValue instanceof MessageChannel) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Cannot map a MessageChannel instance in header " + headerName);
			}
			return true;
		}
		return false;
	}

	/**
	 * Map headers from a source instance to the {@link MessageHeaders} of
	 * a {@link org.springframework.messaging.Message}.
	 */
	private Map<String, Object> toHeaders(T source, HeaderMatcher headerMatcher) {
		Map<String, Object> headers = new HashMap<>();
		Map<String, Object> standardHeaders = extractStandardHeaders(source);
		copyHeaders(standardHeaders, headers, headerMatcher);
		Map<String, Object> userDefinedHeaders = extractUserDefinedHeaders(source);
		copyHeaders(userDefinedHeaders, headers, headerMatcher);
		return headers;
	}

	private void copyHeaders(Map<String, Object> source, Map<String, Object> target, HeaderMatcher headerMatcher) {
		if (!CollectionUtils.isEmpty(source)) {
			for (Map.Entry<String, Object> entry : source.entrySet()) {
				try {
					String headerName = createTargetPropertyName(entry.getKey(), false);
					if (shouldMapHeader(headerName, headerMatcher)) {
						Object value = entry.getValue();
						target.put(headerName, value);
						if (this.replyHeaderMatcher == headerMatcher &&
								JsonHeaders.TYPE_ID.equals(headerName) && value != null) {

							ResolvableType resolvableType =
									createJsonResolvableTypHeaderInAny(value, source.get(JsonHeaders.CONTENT_TYPE_ID),
											source.get(JsonHeaders.KEY_TYPE_ID));
							if (resolvableType != null) {
								target.put(JsonHeaders.RESOLVABLE_TYPE, resolvableType);
							}
						}
					}
				}
				catch (Exception e) {
					if (this.logger.isWarnEnabled()) {
						this.logger.warn("error occurred while mapping header '"
								+ entry.getKey() + "' to Message header", e);
					}
				}
			}
		}
	}

	@Nullable
	private ResolvableType createJsonResolvableTypHeaderInAny(Object typeId, @Nullable Object contentId,
			@Nullable Object keyId) {

		try {
			return JsonHeaders.buildResolvableType(getClassLoader(), typeId, contentId, keyId);
		}
		catch (Exception e) {
			this.logger.debug("Cannot build a ResolvableType from 'json__TypeId__' header", e);
		}
		return null;
	}

	private boolean shouldMapHeader(String headerName, HeaderMatcher headerMatcher) {
		return !(!StringUtils.hasText(headerName) || getTransientHeaderNames().contains(headerName))
				&& headerMatcher.matchHeader(headerName);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected <V> V getHeaderIfAvailable(Map<String, Object> headers, String name, Class<V> type) {
		Object value = headers.get(name);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("skipping header '" + name + "' since it is not of expected type [" + type + "], " +
						"it is [" + value.getClass() + "]");
			}
			return null;
		}
		else {
			return (V) value;
		}
	}

	/**
	 * Alter the specified {@code propertyName} if necessary. By default, the original
	 * {@code propertyName} is returned.
	 * @param propertyName the original name of the property.
	 * @param fromMessageHeaders specify if the property originates from a {@link MessageHeaders}
	 * instance (true) or from the type managed by this mapper (false).
	 * @return the property name for mapping.
	 */
	protected String createTargetPropertyName(String propertyName, boolean fromMessageHeaders) {
		return propertyName;
	}

	/**
	 * Return the transient header names. Transient headers are never mapped.
	 * @return the names of headers to be skipped from mapping.
	 */
	protected Collection<String> getTransientHeaderNames() {
		return TRANSIENT_HEADER_NAMES;
	}

	/**
	 * Extract the standard headers from the specified source.
	 * @param source the source object to extract standard headers.
	 * @return the map of headers to be mapped.
	 */
	protected abstract Map<String, Object> extractStandardHeaders(T source);

	/**
	 * Extract the user-defined headers from the specified source.
	 * @param source the source object to extract user defined headers.
	 * @return the map of headers to be mapped.
	 */
	protected abstract Map<String, Object> extractUserDefinedHeaders(T source);

	/**
	 * Populate the specified standard headers to the specified source.
	 * @param headers the map of standard headers to be populated.
	 * @param target the target object to populate headers.
	 */
	protected abstract void populateStandardHeaders(Map<String, Object> headers, T target);

	/**
	 * Populate the specified standard headers to the specified source.
	 * If not implemented, calls {@link #populateStandardHeaders(Map, Object)}.
	 * @param allHeaders all headers including transient.
	 * @param subset the map of standard headers to be populated.
	 * @param target the target object to populate headers.
	 * @since 5.1
	 */
	protected void populateStandardHeaders(@Nullable Map<String, Object> allHeaders, Map<String, Object> subset,
			T target) {

		populateStandardHeaders(subset, target);
	}

	/**
	 * Populate the specified user-defined headers to the specified source.
	 * @param headerName the user defined header name to be populated.
	 * @param headerValue the user defined header value to be populated.
	 * @param target the target object to populate headers.
	 */
	protected abstract void populateUserDefinedHeader(String headerName, Object headerValue, T target);

	/**
	 * Strategy interface to determine if a given header name matches.
	 *
	 * @since 4.1
	 */
	@FunctionalInterface
	public interface HeaderMatcher {

		/**
		 * Specify if the given {@code headerName} matches.
		 * @param headerName the header name to be matched.
		 * @return {@code true} if {@code headerName} matches to this {@link HeaderMatcher}.
		 */
		boolean matchHeader(String headerName);

		/**
		 * Return true if this match should be explicitly excluded from the mapping.
		 * @return true if negated.
		 */
		default boolean isNegated() {
			return false;
		}

	}

	/**
	 * A content-based {@link HeaderMatcher} that matches if the specified
	 * header is contained within a list of candidates. The case of the
	 * header does not matter.
	 *
	 * @since 4.1
	 */
	protected static class ContentBasedHeaderMatcher implements HeaderMatcher {

		private static final Log LOGGER = LogFactory.getLog(HeaderMatcher.class);

		private final boolean match;

		private final Collection<String> content;

		public ContentBasedHeaderMatcher(boolean match, Collection<String> content) {
			this.match = match;
			Assert.notNull(content, "Content must not be null");
			this.content = content;
		}

		@Override
		public boolean matchHeader(String headerName) {
			boolean result = (this.match == containsIgnoreCase(headerName));
			if (result && LOGGER.isDebugEnabled()) {
				StringBuilder message = new StringBuilder("headerName=[{0}] WILL be mapped, ");
				if (!this.match) {
					message.append("not ");
				}
				message.append("found in {1}");
				LOGGER.debug(MessageFormat.format(message.toString(), headerName, this.content));
			}
			return result;
		}

		private boolean containsIgnoreCase(String name) {
			for (String headerName : this.content) {
				if (headerName.equalsIgnoreCase(name)) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * A pattern-based {@link HeaderMatcher} that matches if the specified
	 * header matches one of the specified simple patterns.
	 *
	 * @since 4.1
	 *
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected static class PatternBasedHeaderMatcher implements HeaderMatcher {

		private static final Log LOGGER = LogFactory.getLog(HeaderMatcher.class);

		private final Collection<String> patterns = new ArrayList<>();

		public PatternBasedHeaderMatcher(Collection<String> patterns) {
			Assert.notNull(patterns, "Patterns must no be null");
			Assert.notEmpty(patterns, "At least one pattern must be specified");
			for (String pattern : patterns) {
				this.patterns.add(pattern.toLowerCase());
			}
		}

		@Override
		public boolean matchHeader(String headerName) {
			String header = headerName.toLowerCase();
			for (String pattern : this.patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, header)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(MessageFormat.format(
								"headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * A pattern-based {@link HeaderMatcher} that matches if the specified
	 * header matches the specified simple pattern.
	 * <p> The {@code negate == true} state indicates if the matching should be treated as "not matched".
	 *
	 * @since 4.3
	 *
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected static class SinglePatternBasedHeaderMatcher implements HeaderMatcher {

		private static final Log LOGGER = LogFactory.getLog(HeaderMatcher.class);

		private final String pattern;

		private final boolean negate;

		public SinglePatternBasedHeaderMatcher(String pattern) {
			this(pattern, false);
		}

		public SinglePatternBasedHeaderMatcher(String pattern, boolean negate) {
			Assert.notNull(pattern, "Pattern must no be null");
			this.pattern = pattern.toLowerCase();
			this.negate = negate;
		}

		@Override
		public boolean matchHeader(String headerName) {
			String header = headerName.toLowerCase();
			if (PatternMatchUtils.simpleMatch(this.pattern, header)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(MessageFormat.format(
							"headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, this.pattern));
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean isNegated() {
			return this.negate;
		}

	}

	/**
	 * A prefix-based {@link HeaderMatcher} that matches if the specified
	 * header starts with a configurable prefix.
	 *
	 * @since 4.1
	 */
	protected static class PrefixBasedMatcher implements HeaderMatcher {

		private static final Log LOGGER = LogFactory.getLog(HeaderMatcher.class);

		private final boolean match;

		private final String prefix;

		public PrefixBasedMatcher(boolean match, String prefix) {
			this.match = match;
			this.prefix = prefix;
		}

		@Override
		public boolean matchHeader(String headerName) {
			boolean result = (this.match == headerName.startsWith(this.prefix));
			if (result && LOGGER.isDebugEnabled()) {
				StringBuilder message = new StringBuilder("headerName=[{0}] WILL be mapped, ");
				if (!this.match) {
					message.append("does not ");
				}
				message.append("start with [{1}]");
				LOGGER.debug(MessageFormat.format(message.toString(), headerName, this.prefix));
			}
			return result;
		}

	}

	/**
	 * A composite {@link HeaderMatcher} that matches if one of provided
	 * {@link HeaderMatcher}s matches to the {@code headerName}.
	 *
	 * @since 4.1
	 */
	protected static class CompositeHeaderMatcher implements HeaderMatcher {

		private static final Log LOGGER = LogFactory.getLog(HeaderMatcher.class);

		private final Collection<HeaderMatcher> matchers;

		CompositeHeaderMatcher(Collection<HeaderMatcher> strategies) {
			this.matchers = strategies;
		}

		CompositeHeaderMatcher(HeaderMatcher... strategies) {
			this(Arrays.asList(strategies));
		}

		@Override
		public boolean matchHeader(String headerName) {
			for (HeaderMatcher strategy : this.matchers) {
				if (strategy.matchHeader(headerName)) {
					if (strategy.isNegated()) {
						break;
					}
					return true;
				}
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
			}
			return false;
		}

	}

}
