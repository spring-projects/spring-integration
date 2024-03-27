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

package org.springframework.integration.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.mapping.AbstractHeaderMapper.CompositeHeaderMatcher;
import org.springframework.integration.mapping.AbstractHeaderMapper.ContentBasedHeaderMatcher;
import org.springframework.integration.mapping.AbstractHeaderMapper.HeaderMatcher;
import org.springframework.integration.mapping.AbstractHeaderMapper.PatternBasedHeaderMatcher;
import org.springframework.integration.mapping.AbstractHeaderMapper.PrefixBasedMatcher;
import org.springframework.integration.mapping.AbstractHeaderMapper.SinglePatternBasedHeaderMatcher;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 * @author Artem Bilan
 *
 * @since 4.1
 */
public class HeaderMapperTests {

	private final GenericTestHeaderMapper mapper = new GenericTestHeaderMapper();

	@Test
	public void toHeadersFromRequest() {
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.get(GenericTestHeaders.REQUEST_ONLY)).isEqualTo("request-123");
		assertThat(attributes.containsKey(GenericTestHeaders.REPLY_ONLY)).isFalse();
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(2);
	}

	@Test
	public void toHeadersFromRequestWithStar() {
		this.mapper.setRequestHeaderNames("*");
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.get(GenericTestHeaders.REQUEST_ONLY)).isEqualTo("request-123");
		assertThat(attributes.get(GenericTestHeaders.REPLY_ONLY)).isEqualTo("reply-123");
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(4);
	}

	@Test
	public void toHeadersFromRequestWithCustomPatterns() {
		this.mapper.setRequestHeaderNames("foo*", "generic_reply*");
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isNull();
		assertThat(attributes.get(GenericTestHeaders.REQUEST_ONLY)).isNull();
		assertThat(attributes.get(GenericTestHeaders.REPLY_ONLY)).isEqualTo("reply-123");
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(2);
	}

	@Test
	public void toHeadersFromRequestWithStandardRequestPattern() {
		this.mapper.setRequestHeaderNames("foo*", GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();
		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.get(GenericTestHeaders.REQUEST_ONLY)).isEqualTo("request-123");
		assertThat(attributes.containsKey(GenericTestHeaders.REPLY_ONLY)).isFalse();
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.get("foo2")).isEqualTo("bar");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(4);
	}

	@Test
	public void toHeadersFromRequestWithOnlyStandardHeaders() {
		this.mapper.setRequestHeaderNames(GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();
		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.get(GenericTestHeaders.REQUEST_ONLY)).isEqualTo("request-123");
		assertThat(attributes.containsKey(GenericTestHeaders.REPLY_ONLY)).isFalse();
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(2);
	}

	@Test
	public void toHeadersFromReply() {
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.containsKey(GenericTestHeaders.REQUEST_ONLY)).isFalse();
		assertThat(attributes.get(GenericTestHeaders.REPLY_ONLY)).isEqualTo("reply-123");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(2);
	}

	@Test
	public void toHeadersFromReplyWithStar() {
		this.mapper.setReplyHeaderNames("*");
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.get(GenericTestHeaders.REQUEST_ONLY)).isEqualTo("request-123");
		assertThat(attributes.get(GenericTestHeaders.REPLY_ONLY)).isEqualTo("reply-123");
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(4);
	}

	@Test
	public void toHeadersFromReplyWithStandardReplyPattern() {
		this.mapper.setReplyHeaderNames("foo*", GenericTestHeaderMapper.STANDARD_REPLY_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();

		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.containsKey(GenericTestHeaders.REQUEST_ONLY)).isFalse();
		assertThat(attributes.get(GenericTestHeaders.REPLY_ONLY)).isEqualTo("reply-123");
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.get("foo2")).isEqualTo("bar");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(4);
	}

	@Test
	public void toHeadersFromReplyWithOnlyStandardReplyHeaders() {
		this.mapper.setReplyHeaderNames(GenericTestHeaderMapper.STANDARD_REPLY_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();

		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertThat(attributes.get(GenericTestHeaders.APP_ID)).isEqualTo("appId");
		assertThat(attributes.containsKey(GenericTestHeaders.REQUEST_ONLY)).isFalse();
		assertThat(attributes.get(GenericTestHeaders.REPLY_ONLY)).isEqualTo("reply-123");
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(2);
	}

	@Test
	public void customTransientHeaderNames() {
		GenericTestHeaderMapper customMapper = new GenericTestHeaderMapper() {

			@Override
			protected Collection<String> getTransientHeaderNames() {
				return Arrays.asList("foo", GenericTestHeaders.APP_ID);
			}
		};
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = customMapper.toHeadersFromReply(properties);
		// foo custom header and app Id not mapped
		assertThat(attributes.containsKey(GenericTestHeaders.APP_ID)).isFalse();
		assertThat(attributes.containsKey("foo")).isFalse();
		assertThat(attributes.size()).as("Wrong number of mapped header(s)").isEqualTo(1);
	}

	private GenericTestProperties createSimpleGenericTestProperties() {
		GenericTestProperties properties = new GenericTestProperties();
		properties.setAppId("appId");
		properties.setRequestOnly("request-123");
		properties.setReplyOnly("reply-123");

		properties.setUserDefinedHeader("foo", "bar");
		return properties;
	}

	@Test
	public void fromHeadersToRequest() {
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isEqualTo("request-456");
		assertThat(properties.getReplyOnly()).isNull();
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(0);
	}

	@Test
	public void fromHeadersToRequestWithStar() {
		this.mapper.setRequestHeaderNames("*");
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isEqualTo("request-456");
		assertThat(properties.getReplyOnly()).isEqualTo("reply-456");
		assertThat(properties.getUserDefinedHeaders().get("foo")).isEqualTo("bar");
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(1);
	}

	@Test
	public void fromHeadersToRequestWithStandardRequestPattern() {
		this.mapper.setRequestHeaderNames("foo", GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isEqualTo("request-456");
		assertThat(properties.getReplyOnly()).isNull();
		assertThat(properties.getUserDefinedHeaders().get("foo")).isEqualTo("bar");
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(1);
	}

	@Test
	public void fromHeadersToRequestWithStandardRequestPatternAndNegatives() {
		this.mapper.setRequestHeaderNames("foo", "!foo", "bar", "!baz", "\\!qux", "!fiz*",
				GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(GenericTestHeaders.APP_ID, "myAppId");
		headers.put(GenericTestHeaders.REDELIVERED, true);
		headers.put(GenericTestHeaders.REQUEST_ONLY, "request-456");
		headers.put(GenericTestHeaders.REPLY_ONLY, "reply-456");
		headers.put("foo", "foo");
		headers.put("bar", "bar");
		headers.put("baz", "baz");
		headers.put("!qux", "qux");
		headers.put("fizbuz", "fizbuz");
		MessageHeaders messageHeaders = new MessageHeaders(headers);
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isEqualTo("request-456");
		assertThat(properties.getReplyOnly()).isNull();
		assertThat(properties.getUserDefinedHeaders().get("bar")).isEqualTo("bar");
		assertThat(properties.getUserDefinedHeaders().get("!qux")).isEqualTo("qux");
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(2);
	}

	@Test
	public void fromHeadersToReply() {
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToReply(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isNull();
		assertThat(properties.getReplyOnly()).isEqualTo("reply-456");
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(0);
	}

	@Test
	public void fromHeadersToReplyWithStar() {
		this.mapper.setReplyHeaderNames("*");
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToReply(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isEqualTo("request-456");
		assertThat(properties.getReplyOnly()).isEqualTo("reply-456");
		assertThat(properties.getUserDefinedHeaders().get("foo")).isEqualTo("bar");
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(1);
	}

	@Test
	public void fromHeadersToReplyWithStandardReplyPattern() {
		this.mapper.setReplyHeaderNames("foo", GenericTestHeaderMapper.STANDARD_REPLY_HEADER_NAME_PATTERN);
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToReply(messageHeaders, properties);
		assertThat(properties.getAppId()).isEqualTo("myAppId");
		assertThat(properties.getTransactionSize()).isNull();
		assertThat(properties.getRedelivered()).isEqualTo(true);
		assertThat(properties.getRequestOnly()).isNull();
		assertThat(properties.getReplyOnly()).isEqualTo("reply-456");
		assertThat(properties.getUserDefinedHeaders().get("foo")).isEqualTo("bar");
		assertThat(properties.getUserDefinedHeaders().size()).isEqualTo(1);
	}

	public MessageHeaders createSimpleMessageHeaders() {
		Map<String, Object> headers = new HashMap<>();
		headers.put(GenericTestHeaders.APP_ID, "myAppId");
		headers.put(GenericTestHeaders.REDELIVERED, true);
		headers.put(GenericTestHeaders.REQUEST_ONLY, "request-456");
		headers.put(GenericTestHeaders.REPLY_ONLY, "reply-456");

		headers.put("foo", "bar");

		return new MessageHeaders(headers);
	}

	@Test
	public void prefixHeaderPatternMatching() {
		PatternBasedHeaderMatcher strategy =
				new PatternBasedHeaderMatcher(Collections.singleton("fOo*"));

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "foo123", true);
		assertMapping(strategy, "FoO", true);

		assertMapping(strategy, "123foo", false);
		assertMapping(strategy, "_foo", false);
	}

	@Test
	public void suffixHeaderPatternMatching() {
		PatternBasedHeaderMatcher strategy =
				new PatternBasedHeaderMatcher(Collections.singleton("*fOo"));

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "123foo", true);
		assertMapping(strategy, "FoO", true);

		assertMapping(strategy, "foo123", false);
		assertMapping(strategy, "foo_", false);
	}

	@Test
	public void prefixSingleHeaderPatternMatching() {
		SinglePatternBasedHeaderMatcher strategy =
				new SinglePatternBasedHeaderMatcher("Foo*");

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "foo123", true);
		assertMapping(strategy, "FoO", true);

		assertMapping(strategy, "123foo", false);
		assertMapping(strategy, "_foo", false);
	}

	@Test
	public void suffixSingleHeaderPatternMatching() {
		SinglePatternBasedHeaderMatcher strategy =
				new SinglePatternBasedHeaderMatcher("*fOo");

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "123foo", true);
		assertMapping(strategy, "FoO", true);

		assertMapping(strategy, "foo123", false);
		assertMapping(strategy, "foo_", false);
	}

	@Test
	public void contentHeaderMatching() {
		ContentBasedHeaderMatcher strategy =
				new ContentBasedHeaderMatcher(true, Arrays.asList("foo", "bar"));

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "bar", true);
		assertMapping(strategy, "FOO", true);
		assertMapping(strategy, "somethingElse", false);
	}

	@Test
	public void contentHeaderReverseMatching() {
		ContentBasedHeaderMatcher strategy =
				new ContentBasedHeaderMatcher(false, Arrays.asList("foo", "bar"));

		assertMapping(strategy, "foo", false);
		assertMapping(strategy, "bar", false);
		assertMapping(strategy, "somethingElse", true);
		assertMapping(strategy, "anything", true);
	}

	@Test
	public void prefixHeaderMatching() {
		PrefixBasedMatcher strategy = new PrefixBasedMatcher(true, "foo_");

		assertMapping(strategy, "foo_", true);
		assertMapping(strategy, "foo_ANYTHING", true);
		assertMapping(strategy, "something_foo_", false);
		assertMapping(strategy, "somethingElse", false);
	}

	@Test
	public void prefixHeaderReverseMatching() {
		PrefixBasedMatcher strategy = new PrefixBasedMatcher(false, "foo_");

		assertMapping(strategy, "foo_", false);
		assertMapping(strategy, "foo_ANYTHING", false);
		assertMapping(strategy, "something_foo_", true);
		assertMapping(strategy, "somethingElse", true);
	}

	@Test
	public void compositeOneMatch() {
		HeaderMatcher strategy = new CompositeHeaderMatcher(
				new PrefixBasedMatcher(true, "foo_"),
				new PrefixBasedMatcher(true, "bar_"));

		assertMapping(strategy, "foo_ANYTHING", true);
		assertMapping(strategy, "bar_ANYTHING", true);
		assertMapping(strategy, "somethingElse", false);
	}

	protected void assertMapping(HeaderMatcher strategy, String candidate, boolean match) {
		assertThat(strategy.matchHeader(candidate)).as("Wrong mapping result for " + candidate + "").isEqualTo(match);
	}

	private abstract static class GenericTestHeaders {

		public static final String PREFIX = "generic_";

		public static final String APP_ID = PREFIX + "appId";

		public static final String TRANSACTION_SIZE = PREFIX + "transactionSize";

		public static final String REDELIVERED = PREFIX + "redelivered";

		public static final String REQUEST_ONLY = PREFIX + "requestOnly";

		public static final String REPLY_ONLY = PREFIX + "replyOnly";

	}

	private static class GenericTestHeaderMapper extends AbstractHeaderMapper<GenericTestProperties> {

		GenericTestHeaderMapper() {
			super(GenericTestHeaders.PREFIX,
					Arrays.asList(GenericTestHeaders.APP_ID, GenericTestHeaders.TRANSACTION_SIZE,
							GenericTestHeaders.REDELIVERED, GenericTestHeaders.REQUEST_ONLY),
					Arrays.asList(GenericTestHeaders.APP_ID, GenericTestHeaders.TRANSACTION_SIZE,
							GenericTestHeaders.REDELIVERED, GenericTestHeaders.REPLY_ONLY));
		}

		@Override
		protected Map<String, Object> extractStandardHeaders(GenericTestProperties source) {
			Map<String, Object> result = new HashMap<>();
			if (StringUtils.hasText(source.getAppId())) {
				result.put(GenericTestHeaders.APP_ID, source.getAppId());
			}
			if (source.getTransactionSize() != null) {
				result.put(GenericTestHeaders.TRANSACTION_SIZE, source.getTransactionSize());
			}
			if (source.getRedelivered() != null) {
				result.put(GenericTestHeaders.REDELIVERED, source.getRedelivered());
			}
			if (StringUtils.hasText(source.getRequestOnly())) {
				result.put(GenericTestHeaders.REQUEST_ONLY, source.getRequestOnly());
			}
			if (StringUtils.hasText(source.getReplyOnly())) {
				result.put(GenericTestHeaders.REPLY_ONLY, source.getReplyOnly());
			}
			return result;

		}

		@Override
		protected Map<String, Object> extractUserDefinedHeaders(GenericTestProperties source) {
			return source.getUserDefinedHeaders();
		}

		@Override
		protected void populateStandardHeaders(Map<String, Object> headers, GenericTestProperties target) {
			String appId = getHeaderIfAvailable(headers, GenericTestHeaders.APP_ID, String.class);
			if (StringUtils.hasText(appId)) {
				target.setAppId(appId);
			}
			Integer transactionSize = getHeaderIfAvailable(headers, GenericTestHeaders.TRANSACTION_SIZE, Integer.class);
			if (transactionSize != null) {
				target.setTransactionSize(transactionSize);
			}
			Boolean redelivered = getHeaderIfAvailable(headers, GenericTestHeaders.REDELIVERED, Boolean.class);
			if (redelivered != null) {
				target.setRedelivered(redelivered);
			}
			String requestOnly = getHeaderIfAvailable(headers, GenericTestHeaders.REQUEST_ONLY, String.class);
			if (StringUtils.hasText(requestOnly)) {
				target.setRequestOnly(requestOnly);
			}
			String replyOnly = getHeaderIfAvailable(headers, GenericTestHeaders.REPLY_ONLY, String.class);
			if (StringUtils.hasText(replyOnly)) {
				target.setReplyOnly(replyOnly);
			}

		}

		@Override
		protected void populateUserDefinedHeader(String headerName, Object headerValue, GenericTestProperties target) {
			target.setUserDefinedHeader(headerName, headerValue);
		}

	}

	private static class GenericTestProperties {

		private String appId;

		private Integer transactionSize;

		private Boolean redelivered;

		private String requestOnly;

		private String replyOnly;

		private final Map<String, Object> userDefinedHeaders = new HashMap<String, Object>();

		GenericTestProperties() {
			super();
		}

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		@Nullable
		public Integer getTransactionSize() {
			return transactionSize;
		}

		public void setTransactionSize(Integer transactionSize) {
			this.transactionSize = transactionSize;
		}

		@Nullable
		public Boolean getRedelivered() {
			return redelivered;
		}

		public void setRedelivered(boolean redelivered) {
			this.redelivered = redelivered;
		}

		public String getRequestOnly() {
			return requestOnly;
		}

		public void setRequestOnly(String requestOnly) {
			this.requestOnly = requestOnly;
		}

		public String getReplyOnly() {
			return replyOnly;
		}

		public void setReplyOnly(String replyOnly) {
			this.replyOnly = replyOnly;
		}

		public Map<String, Object> getUserDefinedHeaders() {
			return userDefinedHeaders;
		}

		public void setUserDefinedHeader(String name, Object value) {
			this.userDefinedHeaders.put(name, value);
		}

	}

}
