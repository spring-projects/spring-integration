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

package org.springframework.integration.twitter.outbound;

import java.util.Collections;
import java.util.List;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.messaging.Message;
import org.springframework.social.twitter.api.SearchParameters;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.util.Assert;

/**
 * The {@link AbstractReplyProducingMessageHandler} implementation to perform request/reply
 * Twitter search with {@link SearchParameters} as the result of {@link #searchArgsExpression}
 * expression evaluation.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class TwitterSearchOutboundGateway extends AbstractReplyProducingMessageHandler
		implements IntegrationEvaluationContextAware {

	private static final int DEFAULT_PAGE_SIZE = 20;

	private final Twitter twitter;

	private volatile Expression searchArgsExpression;

	private volatile EvaluationContext evaluationContext;

	public TwitterSearchOutboundGateway(Twitter twitter) {
		Assert.notNull(twitter, "'twitter' must not be null");
		this.twitter = twitter;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		TypeLocator typeLocator = evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			/*
			 * Register the twitter api package so they don't need a FQCN for SearchParameters.
			 */
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.social.twitter.api");
		}
		this.evaluationContext = evaluationContext;
	}

	/**
	 * An expression that is used to build the search; must resolve to a
	 * {@code SearchParameters} object, or a
	 * {@link String}, in which case the default page size of 20 is applied,
	 * or a list of up to 4 arguments, such as
	 * {@code "{payload, headers.pageSize, headers.sinceId, headers.maxId}"}.
	 * The first (required) argument must resolve to a String (query), the
	 * optional arguments must resolve to an Number and represent the
	 * page size, sinceId, and maxId respectively. Refer to the 'Spring
	 * Social Twitter' documentation for more details.
	 * <p> When using a {@code SearchParameters} directly, it is not necessary
	 * to include the package: {@code "new SearchParameters("#foo").count(20)")}.
	 * <p> Default: {@code "payload"}.
	 * @param searchArgsExpression The expression.
	 */
	public void setSearchArgsExpression(Expression searchArgsExpression) {
		Assert.notNull(searchArgsExpression, "'searchArgsExpression' must not be null");
		this.searchArgsExpression = searchArgsExpression;
	}

	@Override
	public String getComponentType() {
		return "twitter:search-outbound-gateway";
	}

	protected Twitter getTwitter() {
		return twitter;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Object args;
		if (this.searchArgsExpression != null) {
			args = this.searchArgsExpression.getValue(this.evaluationContext, requestMessage);
		}
		else {
			args = requestMessage.getPayload();
		}
		Assert.notNull(args, "The twitter search expression cannot evaluate to 'null'.");
		SearchParameters searchParameters;
		if (args instanceof SearchParameters) {
			searchParameters = (SearchParameters) args;
		}
		else if (args instanceof String) {
			searchParameters = new SearchParameters((String) args).count(DEFAULT_PAGE_SIZE);
		}
		else if (args instanceof List) {
			List<?> list = (List<?>) args;
			Assert.isTrue(list.size() > 0 && list.size() < 5, "Between 1 and 4 search arguments are required");
			Assert.isInstanceOf(String.class, list.get(0), "The first search argument (query) must be a String");
			searchParameters = new SearchParameters((String) list.get(0));
			if (list.size() > 1) {
				Assert.isInstanceOf(Number.class, list.get(1),
						"The second search argument (pageSize) must be a Number");
				searchParameters.count(((Number) list.get(1)).intValue());
				if (list.size() > 2) {
					Assert.isInstanceOf(Number.class, list.get(2),
							"The third search argument (sinceId) must be a Number");
					searchParameters.sinceId(((Number) list.get(2)).longValue());
				}
				if (list.size() > 3) {
					Assert.isInstanceOf(Number.class, list.get(3),
							"The fourth search argument (maxId) must be a Number");
					searchParameters.maxId(((Number) list.get(3)).longValue());
				}
			}
		}
		else {
			throw new IllegalArgumentException(
					"Search Expression must evaluate to a 'SearchParameters', 'String' or 'List'.");
		}
		SearchResults results = this.getTwitter().searchOperations().search(searchParameters);
		if (results != null) {
			List<Tweet> tweets = (results.getTweets() != null ? results.getTweets() : Collections.<Tweet>emptyList());
			return this.getMessageBuilderFactory().withPayload(tweets)
					.setHeader(TwitterHeaders.SEARCH_METADATA, results.getSearchMetadata());
		}
		else {
			return null;
		}

	}

}
