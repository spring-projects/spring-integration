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

package org.springframework.integration.xml.router;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.w3c.dom.Node;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class AbstractXPathChannelNameResolver {

	protected Node extractNode(Message<?> message) {
		if (!Node.class.isAssignableFrom(message.getPayload().getClass())) {
			throw new MessagingException(message, "Payload does not implement org.w3c.dom.Node so can not be evaluated");
		}
		return (Node) message.getPayload();
	}
}
