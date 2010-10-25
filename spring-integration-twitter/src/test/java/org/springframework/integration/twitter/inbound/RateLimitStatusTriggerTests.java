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

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Test;
import org.springframework.scheduling.TriggerContext;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 *
 */
public class RateLimitStatusTriggerTests {

	@Test
	public void testTriggerImediateAndSubsequentExecutionTime() throws Exception{
		Twitter twitter = mock(Twitter.class);
		RateLimitStatusTrigger trigger = new RateLimitStatusTrigger(twitter);
		TriggerContext context = mock(TriggerContext.class);
		Date currentDate = new Date(System.currentTimeMillis());
		Date nextDate = trigger.nextExecutionTime(context);
		// as long as its within 1 msec we can consider it right away for the purpose of testing
		assertTrue(nextDate.getTime() - currentDate.getTime() < 100);
		
		
		RateLimitStatus rateLimitStatis = mock(RateLimitStatus.class);
		when(twitter.getRateLimitStatus()).thenReturn(rateLimitStatis);
		when(rateLimitStatis.getRemainingHits()).thenReturn(2000);
		when(rateLimitStatis.getSecondsUntilReset()).thenReturn(4000);
		
		when(context.lastCompletionTime()).thenReturn(nextDate);
		// based on the above values the next execution time should be at least 2000 msec
		assertTrue(trigger.nextExecutionTime(context).getTime() - nextDate.getTime() > 2000);
	}
}
