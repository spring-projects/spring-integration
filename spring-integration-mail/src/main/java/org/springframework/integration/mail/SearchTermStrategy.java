/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.search.SearchTerm;

/**
 * Strategy to be used to generate a {@link SearchTerm}.
 *
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 * @see ImapMailReceiver
 */
@FunctionalInterface
public interface SearchTermStrategy {

	/**
	 * Will generate an instance of the {@link SearchTerm}.
	 * @param supportedFlags The supported flags.
	 * @param folder The folder.
	 * @return The search term.
	 */
	SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder);

}
