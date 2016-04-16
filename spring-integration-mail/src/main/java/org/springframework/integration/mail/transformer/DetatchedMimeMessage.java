/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.mail.transformer;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

import org.boon.core.reflection.BeanUtils;

import org.springframework.beans.DirectFieldAccessor;

/**
 * MimeMessage with no ties to session, store etc. Most fields (headers etc) are
 * delegated to the super class, with the exception of those that do not have setters.
 *
 * @author Gary Russell
 * @since 4.2.6
 *
 */
public final class DetatchedMimeMessage extends MimeMessage {

	private String encoding;

	private String contendID;

	private int size;

	private String contentType;

	private int lineCount;

	private Folder detachedFolder;

	private Address[] recipients;

	private Flags flags;

	private Address[] from;

	private String messageID;

	private Date receivedDate;

	private DetatchedMimeMessage(Session session) {
		super(session);
		DirectFieldAccessor dfa = new DirectFieldAccessor(this);
		dfa.setPropertyValue("session", null);
	}

	public static DetatchedMimeMessage newInstance() {
		return new DetatchedMimeMessage(Session.getDefaultInstance(new Properties()));
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	public String getEncoding() throws MessagingException {
		return this.encoding;
	}

	public void setContendID(String contendID) {
		this.contendID = contendID;
	}

	@Override
	public String getContentID() throws MessagingException {
		return this.contendID;
	}

	public void setSize(int size) {
		this.size = size;
	}

	@Override
	public int getSize() throws MessagingException {
		return this.size;
	}

	public void setLineCount(int lineCount) {
		this.lineCount = lineCount;
	}

	@Override
	public int getLineCount() throws MessagingException {
		return this.lineCount;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public String getContentType() throws MessagingException {
		return this.contentType;
	}

	@Override
	public Folder getFolder() {
		return this.detachedFolder;
	}

	public void setFolder(Folder folder) {
		DetatchedFolder detatchedFolder = new DetatchedFolder();
		BeanUtils.copyProperties(folder, detatchedFolder);
		this.detachedFolder = detatchedFolder;
	}

	public void setAllRecipients(Address[] recipients) {
		this.recipients = recipients;
	}

	@Override
	public Address[] getAllRecipients() {
		return this.recipients;
	}

	public void setFlags(Flags flags) {
		this.flags = flags;
	}

	@Override
	public Flags getFlags() {
		return this.flags;
	}

	@Override
	public boolean isSet(Flag flag) {
		return this.flags.contains(flag);
	}

	public void setFrom(Address[] from) {
		this.from = from;
	}

	@Override
	public Address[] getFrom() {
		return this.from;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	@Override
	public String getMessageID() {
		return this.messageID;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

	@Override
	public Date getReceivedDate() {
		return this.receivedDate;
	}

	@Override
	public void setContentLanguage(String[] languages) throws MessagingException {
		if (languages != null) {
			super.setContentLanguage(languages);
		}
	}

	@Override
	public void setFileName(String filename) throws MessagingException {
		if (filename != null) {
			super.setFileName(filename);
		}
	}

	private static final class DetatchedFolder extends Folder {

		private DetatchedFolder() {
			super(new DetatchedStore(Session.getDefaultInstance(new Properties()), null));
			new DirectFieldAccessor(this).setPropertyValue("store", null);
		}

		private String name;

		private String fullName;

		private Folder parent;

		private boolean exists;

		private char separator;

		private int type;

		private boolean hasNewMessages;

		private Flags permanentFlags;

		private int messageCount;

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getFullName() {
			return this.fullName;
		}

		@Override
		public Folder getParent() throws MessagingException {
			return this.parent;
		}

		@Override
		public boolean exists() throws MessagingException {
			return this.exists;
		}

		@Override
		public Folder[] list(String pattern) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public char getSeparator() throws MessagingException {
			return this.separator;
		}

		@Override
		public int getType() throws MessagingException {
			return this.type;
		}

		@Override
		public boolean create(int type) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNewMessages() throws MessagingException {
			return this.hasNewMessages;
		}

		@Override
		public Folder getFolder(String name) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean delete(boolean recurse) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean renameTo(Folder f) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void open(int mode) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close(boolean expunge) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public Flags getPermanentFlags() {
			return this.permanentFlags;
		}

		@Override
		public int getMessageCount() throws MessagingException {
			return this.messageCount;
		}

		@Override
		public Message getMessage(int msgnum) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void appendMessages(Message[] msgs) throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Message[] expunge() throws MessagingException {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		@SuppressWarnings("unused")
		public void setParent(Folder parent) {
			DetatchedFolder serializableFolder = new DetatchedFolder();
			BeanUtils.copyProperties(parent, serializableFolder);
			this.parent = serializableFolder;
		}

		@SuppressWarnings("unused")
		public void setExists(boolean exists) {
			this.exists = exists;
		}

		@SuppressWarnings("unused")
		public void setSeparator(char separator) {
			this.separator = separator;
		}

		@SuppressWarnings("unused")
		public void setType(int type) {
			this.type = type;
		}

		@SuppressWarnings("unused")
		public void setHasNewMessages(boolean hasNewMessages) {
			this.hasNewMessages = hasNewMessages;
		}

		@SuppressWarnings("unused")
		public void setPermanentFlags(Flags permanentFlags) {
			this.permanentFlags = permanentFlags;
		}

		@SuppressWarnings("unused")
		public void setMessageCount(int messageCount) {
			this.messageCount = messageCount;
		}

	}

	private static final class DetatchedStore extends Store {

		protected DetatchedStore(Session session, URLName urlname) {
			super(session, urlname);
		}

		@Override
		public Folder getDefaultFolder() throws MessagingException {
			return null;
		}

		@Override
		public Folder getFolder(String name) throws MessagingException {
			return null;
		}

		@Override
		public Folder getFolder(URLName url) throws MessagingException {
			return null;
		}

	}

}

