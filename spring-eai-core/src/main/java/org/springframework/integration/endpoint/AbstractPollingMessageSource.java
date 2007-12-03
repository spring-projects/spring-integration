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

import org.springframework.integration.MessageSource;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * A {@link MessageSource} adapter for any source that can be polled for
 * objects.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractPollingMessageSource implements MessageSource {

	private MessageMapper mapper = new SimplePayloadMessageMapper();


	public void setMapper(MessageMapper mapper) {
		Assert.notNull(mapper, "mapper must not be null");
		this.mapper = mapper;
	}

	public Message receive() {
		return this.receive(-1);
	}

	public Message receive(long timeout) {
		long start = System.currentTimeMillis();
		while (timeout <= 0 || System.currentTimeMillis() - start < timeout) {
			Object o = this.pollForObject();
			if (o != null) {
				return this.mapper.toMessage(o);
			}
			if (timeout == 0) {
				return null;
			}
		}
		return null;
	}


	/**
	 * Method for subclasses to implement. Returns an object to be mapped to a
	 * {@link Message} by the message mapper.
	 */
	protected abstract Object pollForObject();

}
