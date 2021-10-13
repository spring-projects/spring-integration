/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.router;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * A Message Router that resolves the {@link org.springframework.messaging.MessageChannel}
 * based on the {@link Message Message's} payload type.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PayloadTypeRouter extends AbstractMappingMessageRouter {

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
	protected List<Object> getChannelKeys(Message<?> message) {
		if (CollectionUtils.isEmpty(getChannelMappings())) {
			return null;
		}
		Class<?> type = message.getPayload().getClass();
		boolean isArray = type.isArray();
		if (isArray) {
			type = type.getComponentType();
		}
		String closestMatch = findClosestMatch(type, isArray);
		return (closestMatch != null) ? Collections.singletonList(closestMatch) : null;
	}

	private String findClosestMatch(Class<?> type, boolean isArray) { // NOSONAR
		int minTypeDiffWeight = Integer.MAX_VALUE;
		List<String> matches = new LinkedList<>();
		for (String candidate : getChannelMappings().keySet()) {
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
			if (typeDiffWeight < minTypeDiffWeight) {
				minTypeDiffWeight = typeDiffWeight;
				// new winner, start accumulating matches from scratch
				matches.clear();
				matches.add((isArray) ? candidate + ARRAY_SUFFIX : candidate);
			}
			else if (typeDiffWeight == minTypeDiffWeight && typeDiffWeight != Integer.MAX_VALUE) {
				// candidate tied with current winner, keep track
				matches.add(candidate);
			}
		}
		if (matches.size() > 1) { // ambiguity
			throw new IllegalStateException(
					"Unresolvable ambiguity while attempting to find closest match for [" + type.getName() + "]. " +
							"Found: " + matches);
		}
		if (CollectionUtils.isEmpty(matches)) { // no match
			return null;
		}
		// we have a winner
		return matches.get(0);
	}

	private int determineTypeDifferenceWeight(String candidate, Class<?> type, int level) {
		if (type.getName().equals(candidate)) {
			return level;
		}
		for (Class<?> iface : type.getInterfaces()) {
			if (iface.getName().equals(candidate)) {
				return (level % 2 != 0) ? level + 2 : level + 1;
			}
			// no match at this level, continue up the hierarchy
			for (Class<?> superInterface : iface.getInterfaces()) {
				int weight = this.determineTypeDifferenceWeight(candidate, superInterface, level + 3); // NOSONAR
				if (weight < Integer.MAX_VALUE) {
					return weight;
				}
			}
		}
		if (type.getSuperclass() == null) {
			// exhausted hierarchy and found no match
			return Integer.MAX_VALUE;
		}
		return determineTypeDifferenceWeight(candidate, type.getSuperclass(), level + 2);
	}

}
