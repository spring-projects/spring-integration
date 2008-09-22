/*
 * Copyright 2002-2008 the original author or authors.
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

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving Mail attributes
 * from/to integration Message Headers.
 * 
 * @author Mark Fisher
 */
public class MailHeaders {

	public static final String TRANSFPORT_PREFIX = "spring.integration.transport.mail.";

	public static final String SUBJECT = TRANSFPORT_PREFIX + "SUBJECT";

	public static final String TO = TRANSFPORT_PREFIX + "TO";

	public static final String CC = TRANSFPORT_PREFIX + "CC";

	public static final String BCC = TRANSFPORT_PREFIX + "BCC";

	public static final String FROM = TRANSFPORT_PREFIX + "FROM";

	public static final String REPLY_TO = TRANSFPORT_PREFIX+ "REPLY_TO";

}
