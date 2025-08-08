/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.messaging.simp.stomp.StompHeaders;

/**
 * The STOMP headers with Integration-friendly {@code stomp_} prefix.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 *
 * @see StompHeaders
 */
public abstract class IntegrationStompHeaders {

	public static final String PREFIX = "stomp_";

	public static final String RECEIPT = PREFIX + StompHeaders.RECEIPT;

	public static final String HOST = PREFIX + StompHeaders.HOST;

	public static final String LOGIN = PREFIX + StompHeaders.LOGIN;

	public static final String PASSCODE = PREFIX + StompHeaders.PASSCODE;

	public static final String HEARTBEAT = PREFIX + StompHeaders.HEARTBEAT;

	public static final String SESSION = PREFIX + StompHeaders.SESSION;

	public static final String SERVER = PREFIX + StompHeaders.SERVER;

	public static final String DESTINATION = PREFIX + StompHeaders.DESTINATION;

	public static final String ID = PREFIX + StompHeaders.ID;

	public static final String ACK = PREFIX + StompHeaders.ACK;

	public static final String SUBSCRIPTION = PREFIX + StompHeaders.SUBSCRIPTION;

	public static final String MESSAGE_ID = PREFIX + StompHeaders.MESSAGE_ID;

	public static final String RECEIPT_ID = PREFIX + StompHeaders.RECEIPT_ID;

	static final Collection<String> HEADERS =
			Collections.unmodifiableList(Arrays.asList(StompHeaders.RECEIPT, StompHeaders.HOST, StompHeaders.LOGIN,
					StompHeaders.PASSCODE, StompHeaders.HEARTBEAT, StompHeaders.SESSION, StompHeaders.SERVER,
					StompHeaders.DESTINATION, StompHeaders.ID, StompHeaders.ACK, StompHeaders.SUBSCRIPTION,
					StompHeaders.MESSAGE_ID, StompHeaders.RECEIPT_ID));

}
