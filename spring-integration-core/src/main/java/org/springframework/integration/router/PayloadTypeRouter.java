/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A Message Router that resolves the {@link MessageChannel} based on the
 * {@link Message Message's} payload type.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PayloadTypeRouter extends AbstractMessageRouter {

	/**
	 * Selects the most appropriate channel name matching channel identifiers which are the
	 * fully qualified class names encountered while traversing the payload type hierarchy.
	 * To resolve ties and conflicts (e.g., Serializable and String) it will match:
	 * 1. Type name to channel identifier else...
	 * 2. Name of the subclass of the type to channel identifier else...
	 * 3. Name of the Interface of the type to channel identifier while also
	 *    preferring direct interface over indirect subclass
	 */
	@Override
	protected List<Object> getChannelIdentifiers(Message<?> message) {
		String channelName = this.getChannelName(message);
		return (channelName != null) ? Collections.<Object>singletonList(channelName) : null;
	}

	private String getChannelName(Message<?> message) {
		Class<?> type = message.getPayload().getClass();
		boolean array = type.isArray();
		if (array){
			type = type.getComponentType();
		}
		while (type != null && !CollectionUtils.isEmpty(this.channelIdentifierMap)) {

			String mappingName = null;
			if (array){		
				mappingName = type.getName()+"[]";
			}
			else {
				mappingName = type.getName();
			}
			String channelName = this.channelIdentifierMap.get(mappingName);
			if (StringUtils.hasText(channelName)) {
				return channelName;
			}
			// next, check for interfaces (super inluded) of this type 
		
			channelName = this.introspectInterfaces(type, message);
			
			if (channelName != null) {
				return channelName;
			} 
			
			// finally, continue up the hierarchy
			type = type.getSuperclass();
		}
		return null;
	}
	
	private String introspectInterfaces(Class<?> type, Message<?> message){
		Class<?>[] interfaces = type.getInterfaces();
		List<String> matchedInterfaces = new ArrayList<String>();
		
		this.doInitrospect(interfaces, matchedInterfaces);
		
		if (matchedInterfaces.isEmpty()){
			return null;
		}
		else {
			if (matchedInterfaces.size() > 1){
				throw new MessageHandlingException(message,
						"Unresolvable ambiguity while attempting to find closest match for [" + type.getName() +
						"]. Candidate types " + matchedInterfaces +  " have equal weight.");
			}
			else {
				return matchedInterfaces.get(0);
			}			
		}
	}
	
	public void doInitrospect(Class<?>[] interfaces, List<String> matchedInterfaces){
		for (Class<?> extendedInterface : interfaces) {
			String currentChannelName = this.channelIdentifierMap.get(extendedInterface.getName());
			if (StringUtils.hasText(currentChannelName)) {
				matchedInterfaces.add(extendedInterface.getName());
			}
			Class<?>[] extendedInterfaces = extendedInterface.getInterfaces();
			this.doInitrospect(extendedInterfaces, matchedInterfaces);
		}
	}

}
