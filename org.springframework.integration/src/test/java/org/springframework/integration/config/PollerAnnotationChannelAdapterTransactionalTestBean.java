/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Poller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Fisher
 */
@MessageEndpoint
public class PollerAnnotationChannelAdapterTransactionalTestBean {

	private volatile boolean shouldFail;

	private BlockingQueue<String> queue = new ArrayBlockingQueue<String>(1);


	public void setShouldFail(boolean shouldFail) {
		this.shouldFail = shouldFail;
	}

	public void setNextValue(String nextValue) {
		try {
			this.queue.put(nextValue);
		}
		catch (InterruptedException e) {
		}
	}

	@ChannelAdapter("output")
	@Poller(interval=100, maxMessagesPerPoll=1,
			transactionManager="testTransactionManager",
			transactionAttributes=@Transactional(propagation=Propagation.REQUIRES_NEW))
	public String testMethod() {
		String result = null;
		try {
			result = this.queue.take();
		}
		catch (InterruptedException e) {
		}
		if (this.shouldFail) {
			throw new IllegalArgumentException("intentional test failure");
		}
		return result;
	}

}
