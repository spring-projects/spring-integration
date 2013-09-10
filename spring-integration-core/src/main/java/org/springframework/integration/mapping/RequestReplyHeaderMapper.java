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
package org.springframework.integration.mapping;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;

/**
 * Request/Reply strategy interface for mapping {@link MessageHeaders} to and from other
 * types of objects. This would typically be used by adapters where the "other type"
 * has a concept of headers or properties (HTTP, JMS, AMQP, etc).
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public interface RequestReplyHeaderMapper<T> {
	
	void fromHeadersToRequest(MessageHeaders headers, T target);
	
	void fromHeadersToReply(MessageHeaders headers, T target);
	
	Map<String, Object> toHeadersFromRequest(T source);

	Map<String, Object> toHeadersFromReply(T source);
	
}
