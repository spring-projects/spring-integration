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

package org.springframework.integration.message;

/**
 * Interface for any target to which {@link Message Messages} can be sent.
 * 
 * @author Mark Fisher
 */
public interface MessageTarget {

	/**
	 * Send a {@link Message} to this target. May throw a RuntimeException for non-recoverable
	 * errors. Otherwise, if the Message cannot be sent for a non-fatal reason such as timeout,
	 * then this method will return 'false', and if the Message is sent successfully, it will
	 * return 'true'. 
	 * 
	 * @param message the Message to send
	 * 
	 * @return whether the Message has been sent successfully
	 * 
	 * @throws MessageRejectedException if this particular Message is not accepted by the target
	 * (e.g. after consulting a {@link org.springframework.integration.message.selector.MessageSelector})
	 * @throws MessageDeliveryException if this target is unable to send the Message due
	 * to a transport error.
	 */
	boolean send(Message<?> message) throws MessageRejectedException, MessageDeliveryException;

}
