/*
 * Copyright 2002-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.twitter.outbound;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.TweetData;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.util.Assert;

/**
 * MessageHandler for sending regular status updates as well as 'replies' or 'mentions'.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class StatusUpdatingMessageHandler extends AbstractMessageHandler
		implements IntegrationEvaluationContextAware {

	private final Twitter twitter;

	private volatile Expression tweetDataExpression;

	private EvaluationContext evaluationContext;

	public StatusUpdatingMessageHandler(Twitter twitter) {
		Assert.notNull(twitter, "twitter must not be null");
		this.twitter = twitter;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		TypeLocator typeLocator = evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator) {
			/*
			 * Register the twitter api package so they don't need a FQCN for TweetData.
			 */
			((StandardTypeLocator) typeLocator).registerImport("org.springframework.social.twitter.api");
		}
		this.evaluationContext = evaluationContext;
	}

	@Override
	public String getComponentType() {
		return "twitter:outbound-channel-adapter";
	}

	/**
	 * An expression that is used to build the {@link TweetData}; must resolve to a
	 * {@link TweetData} object, or a {@link String}, or a {@link Tweet}.
	 * <p> When using a {@code TweetData} directly in the expression, it is not necessary
	 * to include the package:
	 * {@code "new TweetData("test").withMedia(headers.mediaResource).displayCoordinates(true)")}.
	 * @param tweetDataExpression The expression.
	 * @since 4.0
	 */
	public void setTweetDataExpression(Expression tweetDataExpression) {
		this.tweetDataExpression = tweetDataExpression;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object value;
		if (this.tweetDataExpression != null) {
			value = this.tweetDataExpression.getValue(this.evaluationContext, message);
		}
		else {
			value = message.getPayload();
		}
		Assert.notNull(value, "The tweetData cannot evaluate to 'null'.");

		TweetData tweetData = null;

		if (value instanceof TweetData) {
			tweetData = (TweetData) value;
		}
		else if (value instanceof Tweet) {
			tweetData = new TweetData(((Tweet) value).getText());
		}
		else if (value instanceof String) {
			tweetData = new TweetData((String) value);
		}
		else {
			throw new MessageHandlingException(message, "Unsupported tweetData: " + value);
		}

		this.twitter.timelineOperations().updateStatus(tweetData);
	}

}
