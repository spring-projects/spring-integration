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

package org.springframework.integration.message;

import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A holder for Message metadata. This includes information that may be used by
 * the messaging system (such as <i>correlationId</i>) as well as information
 * that is relevant for specific messaging endpoints. For the latter, String
 * values may be stored as <i>properties</i> and Object values may be stored as
 * <i>attributes</i>.
 * 
 * @author Mark Fisher
 */
public class MessageHeader {

	private final Date timestamp = new Date();

	private volatile Date expiration;

	private volatile Object correlationId;

	private volatile Object returnAddress;

	private volatile int sequenceNumber = 1;

	private volatile int sequenceSize = 1;

	private volatile int priority = 0;

	private final Properties properties = new Properties();

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();


	/**
	 * Return the creation time of this message.  
	 */
	public Date getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Return the expiration date for this message or <code>null</code> to
	 * indicate 'never expire'.
	 */
	public Date getExpiration() {
		return this.expiration;
	}

	/**
	 * Set the expiration date for this message or <code>null</code> to
	 * indicate 'never expire'. The default is <code>null</code>.
	 */
	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public Object getCorrelationId() {
		return this.correlationId;
	}

	public void setCorrelationId(Object correlationId) {
		this.correlationId = correlationId;
	}

	public Object getReturnAddress() {
		return this.returnAddress;
	}

	public void setReturnAddress(Object returnAddress) {
		this.returnAddress = returnAddress;
	}

	public int getSequenceNumber() {
		return this.sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public int getSequenceSize() {
		return this.sequenceSize;
	}

	public void setSequenceSize(int sequenceSize) {
		this.sequenceSize = sequenceSize;
	}

	public int getPriority() {
		return this.priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getProperty(String key) {
		return this.properties.getProperty(key);
	}

	public String setProperty(String key, String value) {
		return (String) this.properties.setProperty(key, value);
	}

	public Set<String> getPropertyNames() {
		Set<String> propertyNames = new HashSet<String>();
		for (Object key : this.properties.keySet()) {
			propertyNames.add((String) key);
		}
		return propertyNames;
	}

	public Object getAttribute(String key) {
		return this.attributes.get(key);
	}

	public Object setAttribute(String key, Object value) {
		return this.attributes.put(key, value);
	}

	public Object setAttributeIfAbsent(String key, Object value) {
		return this.attributes.putIfAbsent(key, value);
	}

	public Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

	public String toString() {
		return "[Properties=" + this.properties + "][Attributes=" + this.attributes + "]";
	}

}
