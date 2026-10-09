/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.inbound;

import jakarta.mail.MessagingException;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for receiving mail {@link jakarta.mail.Message Messages}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 7.0
 */
public interface MailReceiver {

	Object @Nullable [] receive() throws MessagingException;

}
