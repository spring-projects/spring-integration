/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

/**
 * Strategy interface for receiving mail {@link jakarta.mail.Message Messages}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public interface MailReceiver {

	Object[] receive() throws jakarta.mail.MessagingException;

}
