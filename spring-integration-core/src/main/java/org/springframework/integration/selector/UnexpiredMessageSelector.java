/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.selector;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

/**
 * A {@link MessageSelector} that accepts {@link Message Messages} that are
 * <em>not</em> yet expired. If a Message's expiration date header is
 * <code>null</code>, that Message <em>never</em> expires.
 *
 * @author Mark Fisher
 */
public class UnexpiredMessageSelector implements MessageSelector {

	public boolean accept(Message<?> message) {
		Long expirationDate = new IntegrationMessageHeaderAccessor(message).getExpirationDate();
		if (expirationDate == null) {
			return true;
		}
		return expirationDate > System.currentTimeMillis();
	}

}
