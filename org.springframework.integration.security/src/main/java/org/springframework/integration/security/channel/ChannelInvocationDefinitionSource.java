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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.integration.core.MessageChannel;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.intercept.ObjectDefinitionSource;
import org.springframework.util.Assert;

/**
 * The {@link ObjectDefinitionSource} implementation for secured {@link MessageChannel}s.
 * 
 * @author Mark Fisher
 */
public class ChannelInvocationDefinitionSource implements ObjectDefinitionSource {

	private final Map<Pattern, ChannelAccessPolicy> patternMappings =
			new LinkedHashMap<Pattern, ChannelAccessPolicy>();


	public void addPatternMapping(Pattern pattern, ChannelAccessPolicy accessPolicy) {
		this.patternMappings.put(pattern, accessPolicy);
	}

	public Set<Pattern> getPatterns() {
		return this.patternMappings.keySet();
	}

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return ChannelInvocation.class.isAssignableFrom(clazz);
	}

	@SuppressWarnings("unchecked")
	public ConfigAttributeDefinition getAttributes(Object object) throws IllegalArgumentException {
		Assert.isAssignable(ChannelInvocation.class, object.getClass());
		ChannelInvocation invocation = (ChannelInvocation) object;
		String channelName = invocation.getChannel().getName();
		List<ConfigAttribute> attributes = new ArrayList<ConfigAttribute>();
		for (Map.Entry<Pattern, ChannelAccessPolicy> mapping : this.patternMappings.entrySet()) {
			Pattern pattern = mapping.getKey();
			ChannelAccessPolicy accessPolicy = mapping.getValue();
			if (pattern.matcher(channelName).matches()) {
				if (invocation.isSend()) {
					ConfigAttributeDefinition definition = accessPolicy.getConfigAttributeDefinitionForSend();
					if (definition != null) {
						attributes.addAll(definition.getConfigAttributes());
					}
				}
				else if (invocation.isReceive()) {
					ConfigAttributeDefinition definition = accessPolicy.getConfigAttributeDefinitionForReceive();
					if (definition != null) {
						attributes.addAll(definition.getConfigAttributes());
					}
				}
			}
		}
		return new ConfigAttributeDefinition(attributes);
	}

	public Collection<?> getConfigAttributeDefinitions() {
		Set<ConfigAttributeDefinition> definitions = new HashSet<ConfigAttributeDefinition>();
		for (ChannelAccessPolicy accessPolicy : this.patternMappings.values()) {
			ConfigAttributeDefinition sendDefinition = accessPolicy.getConfigAttributeDefinitionForSend();
			if (sendDefinition != null) {
				definitions.add(sendDefinition);
			}
			ConfigAttributeDefinition receiveDefinition = accessPolicy.getConfigAttributeDefinitionForReceive();
			if (receiveDefinition != null) {
				definitions.add(receiveDefinition);
			}
		}
		return definitions;
	}

}
