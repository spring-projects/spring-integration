/*
 * Copyright 2015-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
