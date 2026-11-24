/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mail;

import jakarta.mail.search.SearchTerm;

/**
 * Strategy to be used to generate a {@link SearchTerm}.
 *
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 * @see org.springframework.integration.mail.inbound.ImapMailReceiver
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.SearchTermStrategy}
 */
@Deprecated(forRemoval = true, since = "7.0")
public interface SearchTermStrategy extends org.springframework.integration.mail.inbound.SearchTermStrategy {

}
