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

package org.springframework.integration.message;

import java.util.Date;
import java.util.Set;

/**
 * A holder for Message metadata. This includes information that may be used by
 * the messaging system (such as <i>correlationId</i>) as well as information
 * that is relevant for specific messaging endpoints. For the latter, String
 * values may be stored as <i>properties</i> and Object values may be stored as
 * <i>attributes</i>.
 * 
 * @author Mark Fisher
 */
public interface MessageHeader {

	long getTimestamp();

	Date getExpiration();

	void setExpiration(Date expiration);

	Object getCorrelationId();

	void setCorrelationId(Object correlationId);

	Object getReturnAddress();

	void setReturnAddress(Object returnAddress);

	int getSequenceNumber();

	void setSequenceNumber(int sequenceNumber);

	int getSequenceSize();

	void setSequenceSize(int sequenceSize);

	MessagePriority getPriority();

	void setPriority(MessagePriority priority);

	String getProperty(String key);

	String setProperty(String key, String value);

	String removeProperty(String key);

	Set<String> getPropertyNames();

	Object getAttribute(String key);

	Object setAttribute(String key, Object value);

	Object setAttributeIfAbsent(String key, Object value);

	Object removeAttribute(String key);

	Set<String> getAttributeNames();

}
