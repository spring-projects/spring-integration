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

package org.springframework.integration.xml.util;

import org.w3c.dom.Node;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public abstract class XPathUtils {

	/**
	 * Return the given Message's payload as a Node if possible, else an Exception will be thrown.
	 */
	public static Node extractPayloadAsNode(Message<?> message) {
		if (!Node.class.isAssignableFrom(message.getPayload().getClass())) {
			throw new MessagingException(message, "payload is not assignable to [" + Node.class.getName() + "] so can not be evaluated");
		}
		return (Node) message.getPayload();
	}

}
