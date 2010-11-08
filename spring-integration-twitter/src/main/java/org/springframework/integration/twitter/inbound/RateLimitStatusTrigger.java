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
package org.springframework.integration.twitter.inbound;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class RateLimitStatusTrigger implements Trigger {
	protected final Log logger = LogFactory.getLog(getClass());
	private Twitter twitter;

	public RateLimitStatusTrigger(Twitter twitter){
		Assert.notNull(twitter, "'twitter' must not be null");
		this.twitter = twitter;
	}

	/* (non-Javadoc)
	 * @see org.springframework.scheduling.Trigger#nextExecutionTime(org.springframework.scheduling.TriggerContext)
	 */
	public Date nextExecutionTime(TriggerContext triggerContext) {
		if (triggerContext.lastCompletionTime() == null){
			return new Date(System.currentTimeMillis());
		}
		try {
			RateLimitStatus rateLimitStatus = twitter.getRateLimitStatus();
			int secondsUntilReset = rateLimitStatus.getSecondsUntilReset();
			int remainingHits = rateLimitStatus.getRemainingHits();
			 if (remainingHits == 0) {
                logger.debug(
                    "rate status limit service returned 0 for the remaining hits value");
                return null;
             }
	         if (secondsUntilReset == 0) {
	        	 logger.debug(
                 	"rate status limit service returned 0 for the seconds until reset period value");
	        	 return null;
	         }
	         int secondsUntilWeCanPullAgain = secondsUntilReset / remainingHits;
	         long msUntilWeCanPullAgain = secondsUntilWeCanPullAgain * 1000;
	         logger.debug("Waiting for " + secondsUntilWeCanPullAgain +
	        		 " seconds until the next timeline pull. Have " + remainingHits +
	        		 " remaining pull this rate period. The period ends in " +
	        		 secondsUntilReset);
	         return new Date(System.currentTimeMillis() + msUntilWeCanPullAgain);
		} catch (TwitterException e) {
			throw new SchedulingException("Failed to schedule the next Twitter update", e);
		}
	}
}
