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

package org.springframework.integration.adapter.mail;

import org.springframework.integration.message.Message;

/**
 * Mail header generator implementation that populates a mail message header
 * from statically configured properties.
 * 
 * @author Marius Bogoevici
 */
public class StaticMailHeaderGenerator extends AbstractMailHeaderGenerator {

	private String subject;

	private String[] to;

	private String[] cc;

	private String[] bcc;

	private String from;

	private String replyTo;


	public void setSubject(String subject) {
		this.subject = subject;
	}

	protected String getSubject(Message<?> message) {
		return this.subject;
	}

	public void setTo(String[] to) {
		this.to = to;
	}

	protected String[] getTo(Message<?> message) {
		return this.to;
	}

	public void setCc(String[] cc) {
		this.cc = cc;
	}

	protected String[] getCc(Message<?> message) {
		return this.cc;
	}

	public void setBcc(String[] bcc) {
		this.bcc = bcc;
	}

	protected String[] getBcc(Message<?> message) {
		return this.bcc;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	protected String getFrom(Message<?> message) {
		return this.from;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	protected String getReplyTo(Message<?> message) {
		return this.replyTo;
	}

}
