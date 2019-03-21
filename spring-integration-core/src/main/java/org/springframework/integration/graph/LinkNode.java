/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.graph;

/**
 * Represents a link between nodes.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class LinkNode {

	private final int from;

	private final int to;

	private final Type type;

	public LinkNode(int from, int to, Type type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public Type getType() {
		return this.type;
	}

	public enum Type {
		input, output, error, discard, route
	}

}
