/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.gateway.futures;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.mapping.InboundMessageMapper;

/**
 * @author Oleg Zhurakousky
 *
 */
public class ExceptionMapper implements InboundMessageMapper<MessageHandlingException>{

	@Override
	public Message<?> toMessage(MessageHandlingException messageHandlingException)
			throws Exception {
		return messageHandlingException.getFailedMessage();
	}

}
