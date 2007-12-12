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

package org.springframework.integration.message;

import org.springframework.integration.util.RandomGuidUidGenerator;
import org.springframework.integration.util.UidGenerator;
import org.springframework.util.Assert;

/**
 * A {@link MessageMapper} implementation that simply wraps and unwraps a
 * payload object in a {@link DocumentMessage}.
 * 
 * @author Mark Fisher
 */
public class SimplePayloadMessageMapper<T> implements MessageMapper<T,T> {

	private UidGenerator uidGenerator;


	/**
	 * Create a mapper with the provided id generation strategy.
	 * 
	 * @param uidGenerator the generator to use for message ids
	 */
	public SimplePayloadMessageMapper(UidGenerator uidGenerator) {
		Assert.notNull(uidGenerator, "uidGenerator must not be null");
		this.uidGenerator = uidGenerator;
	}

	/**
	 * Create a mapper with the default id generation strategy.
	 * 
	 * @see RandomGuidUidGenerator
	 */
	public SimplePayloadMessageMapper() {
		this.uidGenerator = new RandomGuidUidGenerator();
	}


	/**
	 * Return the payload of the given Message.
	 */
	public T fromMessage(Message<T> message) {
		return message.getPayload();
	}

	/**
	 * Return a {@link DocumentMessage} with the given object as its payload.
	 */
	public Message<T> toMessage(T source) {
		return new GenericMessage<T>(uidGenerator.generateUid(), source);
	}

}
