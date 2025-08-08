/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.mongodb.store;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
public final class MessageDocumentFields {

	public static final String MESSAGE_ID = "messageId";

	public static final String PRIORITY = "priority";

	public static final String GROUP_ID = "groupId";

	public static final String LAST_MODIFIED_TIME = "lastModifiedTime";

	public static final String SEQUENCE = "sequence";

	public static final String LAST_RELEASED_SEQUENCE = "lastReleasedSequence";

	public static final String COMPLETE = "complete";

	private MessageDocumentFields() {
	}

}
