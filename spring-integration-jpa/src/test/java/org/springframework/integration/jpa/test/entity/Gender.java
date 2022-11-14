/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jpa.test.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the gender of the person
 *
 * @author Amol Nayak
 *
 */
public enum Gender {

	MALE("M"), FEMALE("F");

	private String identifier;

	private static Map<String, Gender> identifierMap;

	Gender(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	static {
		EnumSet<Gender> all = EnumSet.allOf(Gender.class);
		identifierMap = new HashMap<String, Gender>();
		for (Gender gender : all) {
			identifierMap.put(gender.getIdentifier(), gender);
		}
	}

	public static Gender getGenderFromIdentifier(String identifier) {
		return identifierMap.get(identifier);
	}
}
