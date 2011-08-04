/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store.messagegroupstore;

import java.util.Collection;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Josh Long
 * @since 2.1
 */
@Component
public class MessageGroupStoreActivator {

	@ServiceActivator
	public void activate(Message<Collection<Object>> msg) throws Throwable {
		Collection<Object> payloads = msg.getPayload();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < 100; i++) {
			buffer.append("-");
		}
		System.out.println(buffer.toString());
		System.out.println(StringUtils.collectionToCommaDelimitedString(payloads));
	}

}
