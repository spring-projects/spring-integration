/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.integration.MessageSource;
import org.springframework.integration.message.Message;

/**
 * A {@link MessageSource} adapter for any source that can be polled for
 * objects. This version allows for pre-fetching multiple results so that
 * subsequent calls to {@link #pollForObject()} may be more efficient.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractPrefetchingMessageSource extends AbstractPollingMessageSource {

	private BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();


	public Object pollForObject() {
		Object o = queue.poll();
		if (o == null) {
			this.prefetch();
			o = queue.poll();
		}
		return o;
	}

	private void prefetch() {
		Object[] results = this.pollForObjects();
		if (results != null) {
			try {
				for (Object o : results) {
					queue.put(o);
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}


	/**
	 * Method for subclasses to implement. Returns objects to be mapped to
	 * {@link Message Messages} by the message mapper.
	 */
	protected abstract Object[] pollForObjects();

}
