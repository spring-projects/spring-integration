/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats.dto;

import java.io.Serializable;

/**
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public class TestDTOStubV2 implements Serializable {

	private static final long serialVersionUID = -684979447075L;

	private String property;

	private String anyNewProperty;

	public TestDTOStubV2() {
	}

	public TestDTOStubV2(final String propertyValue, final String anyNewProperty) {
		this.property = propertyValue;
		this.anyNewProperty = anyNewProperty;
	}

	public String getProperty() {
		return this.property;
	}

	public String getAnyNewProperty() {
		return anyNewProperty;
	}
}
