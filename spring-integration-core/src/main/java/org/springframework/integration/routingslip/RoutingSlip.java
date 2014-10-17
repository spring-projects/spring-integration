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

package org.springframework.integration.routingslip;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code Routing Slip} holder. Uses as a message header to keep the path and current index.
 *
 * @author Artem Bilan
 * @since 4.1
 */
@SuppressWarnings("serial")
public class RoutingSlip implements Serializable {

	private final List<String> path;

	private final AtomicInteger index = new AtomicInteger();

	public RoutingSlip(List<String> path) {
		this.path = path;
	}

	public String get() {
		return this.path.get(this.index.get());
	}

	public void move() {
		this.index.incrementAndGet();
	}

	public boolean end() {
		return this.path.size() == this.index.get();
	}

	@Override
	public String toString() {
		return "RoutingSlip{" +
				"path=" + path +
				", index=" + index +
				", end=" + end() +
				'}';
	}

}
