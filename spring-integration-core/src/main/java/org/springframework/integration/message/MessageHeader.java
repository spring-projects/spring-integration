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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.channel.MessageChannel;

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

	private Date timestamp = new Date();

	private Date expiration;

	private Object correlationId;

	private MessageChannel replyChannel;

	private String replyChannelName;

	private int sequenceNumber = 1;

	private int sequenceSize = 1;

	private Properties properties = new Properties();

	private Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();


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

	public MessageChannel getReplyChannel() {
		return this.replyChannel;
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	public String getReplyChannelName() {
		return this.replyChannelName;
	}

	public void setReplyChannelName(String replyChannelName) {
		this.replyChannelName = replyChannelName;
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

	public String getProperty(String key) {
		return this.properties.getProperty(key);
	}

	public void setProperty(String key, String value) {
		this.properties.setProperty(key, value);
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

	public void setAttribute(String key, Object value) {
		this.attributes.put(key, value);
	}

	public Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

}
