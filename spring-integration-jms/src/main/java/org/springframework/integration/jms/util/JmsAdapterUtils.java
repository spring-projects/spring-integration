/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.jms.util;

import org.springframework.util.StringUtils;

/**
 * @author Liujiong
 * @since 4.1
 *
 */
public class JmsAdapterUtils {
	

	public static final int SESSION_TRANSACTED = 0;

	public static final int AUTO_ACKNOWLEDGE = 1;

	public static final int CLIENT_ACKNOWLEDGE = 2;

	public static final int DUPS_OK_ACKNOWLEDGE = 3;

	public static Integer parseAcknowledgeMode(String acknowledge) {
		if (StringUtils.hasText(acknowledge)) {
			int acknowledgeMode = AUTO_ACKNOWLEDGE;
			if ("transacted".equals(acknowledge)) {
				acknowledgeMode = SESSION_TRANSACTED;
			}
			else if ("dups-ok".equals(acknowledge)) {
				acknowledgeMode = DUPS_OK_ACKNOWLEDGE;
			}
			else if ("client".equals(acknowledge)) {
				acknowledgeMode = CLIENT_ACKNOWLEDGE;
			}
			else if (!"auto".equals(acknowledge)) {
				throw new IllegalStateException("Invalid JMS 'acknowledge' setting: " +
						"only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.");
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}
	
	
}
