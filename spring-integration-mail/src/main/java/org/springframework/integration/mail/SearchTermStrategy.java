/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.mail;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.search.SearchTerm;

/**
 * Strategy to be used to generate a {@link SearchTerm}
 *
 * See {@link ImapMailReceiver}
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public interface SearchTermStrategy {

	/**
	 * Will generate an instance of the {@link SearchTerm}
	 *
	 * @param supportedFlags The supported flags.
	 * @param folder The folder.
	 * @return The search term.
	 */
	SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder);

}
