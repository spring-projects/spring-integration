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
package org.springframework.integration.controlbus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ControlBusMessageHandler implements MessageHandler {
	private static final Log log = LogFactory.getLog(ControlBusMessageHandler.class);
	/**
	 * 
	 */
	public void handleMessage(Message<?> message)
			throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		log.debug("Handling control message: " + message);
	}

}
