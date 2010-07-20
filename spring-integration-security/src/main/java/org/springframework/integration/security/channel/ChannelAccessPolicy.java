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

package org.springframework.integration.security.channel;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Creates the {@link ConfigAttributeDefinition}s for secured channel
 * send and receive operations based on simple String values.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ChannelAccessPolicy {

	private final ConfigAttribute configAttributeDefinitionForSend;

	private final ConfigAttribute configAttributeDefinitionForReceive;


	/**
	 * Create an access policy instance. The provided 'sendAccess' and 'receiveAccess'
	 * values may be a single String or a comma-delimited list of values. All whitespace
	 * will be trimmed. A <code>null</code> value indicates that the policy does not
	 * apply for either send or receive access type. At most one of the values may be null. 
	 */
	public ChannelAccessPolicy(String sendAccess, String receiveAccess) {
		Assert.isTrue(sendAccess != null || receiveAccess != null,
				"At least one of 'sendAccess' and 'receiveAccess' must not be null.");
		this.configAttributeDefinitionForSend = (StringUtils.hasText(sendAccess))
				? new SecurityConfig(sendAccess) : null;
		this.configAttributeDefinitionForReceive = (StringUtils.hasText(receiveAccess))
				? new SecurityConfig(receiveAccess) : null;
	}


	public ConfigAttribute getConfigAttributeDefinitionForSend() {
		return this.configAttributeDefinitionForSend;
	}

	public ConfigAttribute getConfigAttributeDefinitionForReceive() {
		return this.configAttributeDefinitionForReceive;
	}

}
