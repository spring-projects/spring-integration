/* Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.samples.errorhandling;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;

public class UnwrappingErrorMessageTransformer implements Transformer {

	public Message<?> transform(Message<?> message) {
		Assert.isAssignable(ErrorMessage.class, message.getClass());
		return ((MessagingException) ((ErrorMessage) message).getPayload()).getFailedMessage();
	}
}
