/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mail.inbound;

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

	Object @Nullable [] receive() throws jakarta.mail.MessagingException;

}
