/*
 * Copyright 2010 the original author or authors
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

package org.springframework.integration.twitter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;
import org.springframework.util.Assert;

import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Twitter;

/**
 * There are a lot of operations that are common to receiving the various types of messages when using the 
 * Twitter API, and this class abstracts most of them for you. Implementers must take note of
 * {@link org.springframework.integration.twitter.AbstractInboundTwitterEndpointSupport#runAsAPIRateLimitsPermit(org.springframework.integration.twitter.AbstractInboundTwitterEndpointSupport.ApiCallback)}
 * which will invoke the instance of {@link AbstractInboundTwitterEndpointSupport.ApiCallback} when the 
 * rate-limit API deems that its OK to do so. This class handles keeping tabs on that and on spacing out requests 
 * as required.
 * 
 * <p/>
 * Simialarly, this class handles keeping track on the latest inbound message its received and avoiding, where 
 * possible, redelivery of common messages. This functionality is enabled using the 
 * {@link org.springframework.integration.context.metadata.MetadataStore} implementation
 *
 * @author Josh Long
 * @since 2.0
 */
public abstract class AbstractInboundTwitterEndpointSupport<T> extends AbstractEndpoint implements Lifecycle, TrackableComponent {

	protected volatile OAuthConfiguration configuration;

	protected final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile MessageChannel requestChannel;

	protected volatile long markerId = -1;

	protected Twitter twitter;

	private final Object markerGuard = new Object();

	private final Object apiPermitGuard = new Object();
	
	private final HistoryWritingMessagePostProcessor historyWritingPostProcessor = new HistoryWritingMessagePostProcessor();

	public void setConfiguration(OAuthConfiguration configuration) {
		this.configuration = configuration;
	}


	abstract protected void markLastStatusId(T statusId);

	abstract protected List<T> sort( List<T> rl);

	protected void forwardAll( List<T> tResponses) {
		List<T> stats = new ArrayList<T>();

		for (T t : tResponses)
			stats.add(t);

		for (T twitterResponse : sort(stats))
			forward(twitterResponse);
	}

	public long getMarkerId() {
		return markerId;
	}
	
	public String getComponentType() {
		return "twitter:inbound-dm-channel-adapter";
	}
	
	public void setRequestChannel(MessageChannel requestChannel) {
		this.messagingTemplate.setDefaultChannel(requestChannel);
		this.requestChannel = requestChannel;
	}

	@Override
	protected void doStart() {
		try {
			this.historyWritingPostProcessor.setTrackableComponent(this);
			refresh();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void forward(T status) {
		synchronized (this.markerGuard) {
			Message<T> twtMsg = MessageBuilder.withPayload(status).build();
			messagingTemplate.convertAndSend(requestChannel, twtMsg, this.historyWritingPostProcessor);
			markLastStatusId(status);
		}
	}
	
	//abstract protected List<T> sort(List<T> rl);
	
	//abstract protected void markLastStatusId(T statusId);
	
	abstract protected void refresh() throws Exception;

	protected void forwardAll(ResponseList<T> tResponses) {
		List<T> stats = new ArrayList<T>();

		for (T t : tResponses)
			stats.add(t);

		for (T twitterResponse : sort(stats))
			forward(twitterResponse);
	}

	@SuppressWarnings("unchecked")
	protected void runAsAPIRateLimitsPermit(ApiCallback cb)
			throws Exception {
		synchronized (this.apiPermitGuard) {
			while (waitUntilPullAvailable()) {
				if (logger.isDebugEnabled()) {
					logger.debug("have room to make an API request now");
				}

				cb.run(this, twitter);
			}
		}
	}

	protected boolean handleReceivingRateLimitStatus(RateLimitStatus rateLimitStatus) {
		try {
			int secondsUntilReset = rateLimitStatus.getSecondsUntilReset();
			int remainingHits = rateLimitStatus.getRemainingHits();

			if (remainingHits == 0) {
				logger.debug("rate status limit service returned 0 for the remaining hits value");

				return false;
			}

			if (secondsUntilReset == 0) {
				logger.debug("rate status limit service returned 0 for the seconds until reset period value");

				return false;
			}

			int secondsUntilWeCanPullAgain = secondsUntilReset / remainingHits;
			long msUntilWeCanPullAgain = secondsUntilWeCanPullAgain * 1000;

			logger.debug("need to Thread.sleep() " + secondsUntilWeCanPullAgain + 
                          " seconds until the next timeline pull. Have " + remainingHits +
  			  " remaining pull this rate period. The period ends in " + secondsUntilReset);

			Thread.sleep(msUntilWeCanPullAgain);
		} catch (Throwable throwable) {
			logger.debug("encountered an error when" + " trying to refresh the timeline: " + ExceptionUtils.getFullStackTrace(throwable));
		}

		return true;
	}

	protected boolean waitUntilPullAvailable() throws Exception {
		return this.handleReceivingRateLimitStatus(this.twitter.getRateLimitStatus());
	}

	protected boolean hasMarkedStatus() {
		return markerId > -1;
	}

	@Override
	protected void onInit() throws Exception {
		messagingTemplate.afterPropertiesSet();
		Assert.notNull(this.configuration, "'configuration' can't be null");
		this.twitter = this.configuration.getTwitter();
		Assert.notNull(this.twitter, "'twitter' instance can't be null");
	}

	@Override
	protected void doStop() {
	}

	/**
	 * Hook for clients to run logic when the API rate limiting lets us
	 * <p/>
	 * Simply register your callback using #runAsAPIRateLimitsPermit
	 *
	 * @param <C>
	 */
	public static interface ApiCallback<C> {
		void run(C t, Twitter twitter) throws Exception;
	}

	public void setShouldTrack(boolean shouldTrack) {
		this.historyWritingPostProcessor.setShouldTrack(shouldTrack);
	}
}
