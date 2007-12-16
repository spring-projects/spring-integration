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

package org.springframework.integration.channel;

import org.springframework.integration.message.Message;

/**
 * Base channel interface defining common behavior for message reception and sending.
 * 
 * @author Mark Fisher
 */
public interface MessageChannel {

	void setName(String name);

	String getName();

	/**
	 * Send a message, blocking indefinitely if necessary.
	 * 
	 * @param message the {@link Message} to send
	 * 
	 * @return <code>true</code> if the message is sent
	 * successfully, <code>false</false> if interrupted
	 */
	boolean send(Message message);

	/**
	 * Send a message, blocking until either the message is
	 * accepted or the specified timeout period elapses.
	 * 
	 * @param message the {@link Message} to send
	 * @param timeout the timeout in milliseconds
	 * 
	 * @return <code>true</code> if the message is sent
	 * successfully, <code>false</false> if the specified
	 * timeout period elapses or the send is interrupted
	 */
	boolean send(Message message, long timeout);

	/**
	 * Receive a message, blocking indefinitely if necessary.
	 * 
	 * @return the next available {@link Message} or
	 * <code>null</code> if interrupted
	 */
	Message receive();

	/**
	 * Receive a message, blocking until either a message is
	 * available or the specified timeout period elapses.
	 * 
	 * @param timeout the timeout in milliseconds
	 * 
	 * @return the next available {@link Message} or
	 * <code>null</code> if the specified timeout period
	 * elapses or the message reception is interrupted
	 */
	Message receive(long timeout);

}
