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

package org.springframework.integration.core;

/**
 * Base channel interface defining common behavior for message sending and receiving.
 * 
 * @author Mark Fisher
 */
public interface MessageChannel {

	/**
	 * Return the name of this channel.
	 */
	String getName();

	/**
	 * Send a {@link Message} to this channel. May throw a RuntimeException for non-recoverable
	 * errors. Otherwise, if the Message cannot be sent for a non-fatal reason this method will
	 * return 'false', and if the Message is sent successfully, it will return 'true'. 
	 * 
	 * @param message the Message to send
	 * @return whether the Message has been sent successfully
	 */
	boolean send(Message<?> message);

}
