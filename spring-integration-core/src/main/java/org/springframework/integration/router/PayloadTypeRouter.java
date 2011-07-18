/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.util.CollectionUtils;

/**
 * A Message Router that resolves the {@link MessageChannel} based on the
 * {@link Message Message's} payload type.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PayloadTypeRouter extends AbstractMessageRouter {

	private static final String ARRAY_SUFFIX = "[]";

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
		if (CollectionUtils.isEmpty(this.channelIdentifierMap)) {
			return null;
		}
		Class<?> type = message.getPayload().getClass();
		boolean isArray = type.isArray();
		if (isArray) {
			type = type.getComponentType();
		}
		return this.findClosestMatch(type, isArray);
	}

	private String findClosestMatch(Class<?> type, boolean isArray) {
		int minTypeDiffWeight = Integer.MAX_VALUE;
		String closestMatch = null;
		int ambiguityAtClosestMatchLevel = 0;
		Map<Integer, List<String>> weightedMatches = new HashMap<Integer, List<String>>();
		
		for (String candidate : this.channelIdentifierMap.keySet()) {
			if (isArray) {
				if (!candidate.endsWith(ARRAY_SUFFIX)) {
					continue;
				}
				// trim the suffix
				candidate = candidate.substring(0, candidate.length() - ARRAY_SUFFIX.length());
			}
			else if (candidate.endsWith(ARRAY_SUFFIX)) {
				continue;
			}
			
			int typeDiffWeight = determineTypeDifferenceWeight(candidate, type, 0);
			List<String> weightedMatch = weightedMatches.get(typeDiffWeight);
			if (weightedMatch == null){
				weightedMatch = new ArrayList<String>();
				weightedMatches.put(typeDiffWeight, weightedMatch);
			}
			weightedMatch.add(candidate);
			
			if (typeDiffWeight < minTypeDiffWeight) {
				minTypeDiffWeight = typeDiffWeight;
				closestMatch = (isArray) ? candidate + ARRAY_SUFFIX : candidate;
				ambiguityAtClosestMatchLevel = 0;
			}
			else if (typeDiffWeight == minTypeDiffWeight && typeDiffWeight != Integer.MAX_VALUE) {
				ambiguityAtClosestMatchLevel = typeDiffWeight;
			}
		}
		if (ambiguityAtClosestMatchLevel > 0) {
			throw new IllegalStateException(
					"Unresolvable ambiguity while attempting to find closest match for [" + type.getName() + "]." + weightedMatches.get(ambiguityAtClosestMatchLevel));
		}
		if (closestMatch == null) {
			return null;
		}
		return this.channelIdentifierMap.get(closestMatch);		
	}

	private int determineTypeDifferenceWeight(String candidate, Class<?> type, int level) {
		if (type.getName().equals(candidate)) {
			return level;
		}

		for (Class<?> iface : type.getInterfaces()) {
	
			if (iface.getName().equals(candidate)) {
				return (level % 2 == 1) ? level + 2 : level + 1; 
			}
			// no match at this level, continue up the hierarchy
			for (Class<?> superInterface : iface.getInterfaces()) {
				int weight = this.determineTypeDifferenceWeight(candidate, superInterface, level + 3);
				if (weight < Integer.MAX_VALUE) {
					return weight;
				}
			}
		}
		if (type.getSuperclass() == null) {
			// exhausted hierarchy and found no match
			return Integer.MAX_VALUE;
		}
		return this.determineTypeDifferenceWeight(candidate, type.getSuperclass(), level + 2);
	}
}
