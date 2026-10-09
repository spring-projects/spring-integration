/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

/**
 * Strategy interface for receiving mail {@link jakarta.mail.Message Messages}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.MailReceiver}
 */
@Deprecated(forRemoval = true, since = "7.0")
public interface MailReceiver extends org.springframework.integration.mail.inbound.MailReceiver {

}
