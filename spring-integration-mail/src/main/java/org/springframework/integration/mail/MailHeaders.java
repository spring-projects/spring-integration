/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

/**
 * Pre-defined header names to be used for setting and/or retrieving Mail
 * Message attributes from/to integration Message Headers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public final class MailHeaders {

	public static final String PREFIX = "mail_";

	public static final String SUBJECT = PREFIX + "subject";

	public static final String TO = PREFIX + "to";

	public static final String CC = PREFIX + "cc";

	public static final String BCC = PREFIX + "bcc";

	public static final String FROM = PREFIX + "from";

	public static final String REPLY_TO = PREFIX + "replyTo";

	public static final String MULTIPART_MODE = PREFIX + "multipartMode";

	public static final String ATTACHMENT_FILENAME = PREFIX + "attachmentFilename";

	public static final String CONTENT_TYPE = PREFIX + "contentType";

	public static final String RAW_HEADERS = PREFIX + "raw";

	public static final String FLAGS = PREFIX + "flags";

	public static final String LINE_COUNT = PREFIX + "lineCount";

	public static final String RECEIVED_DATE = PREFIX + "receivedDate";

	public static final String SIZE = PREFIX + "size";

	public static final String EXPUNGED = PREFIX + "expunged";

	private MailHeaders() {
		// empty
	}

}
