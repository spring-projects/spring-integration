/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.inbound;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.search.SearchTerm;
import org.jspecify.annotations.Nullable;

/**
 * Strategy to be used to generate a {@link SearchTerm}.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 7.0
 *
 * @see ImapMailReceiver
 */
@FunctionalInterface
public interface SearchTermStrategy {

	/**
	 * Generate an instance of the {@link SearchTerm}.
	 * @param supportedFlags The supported flags.
	 * @param folder The folder.
	 * @return The search term.
	 */
	@Nullable
	SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder);

}
