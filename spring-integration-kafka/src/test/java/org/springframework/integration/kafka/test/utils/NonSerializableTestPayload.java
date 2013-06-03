/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.test.utils;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class NonSerializableTestPayload {
	private final String part1;
	private final String part2;

	public NonSerializableTestPayload(final String part1, final String part2) {
		this.part1 = part1;
		this.part2 = part2;
	}

	public String getPart1() {
		return part1;
	}

	public String getPart2() {
		return part2;
	}
}
