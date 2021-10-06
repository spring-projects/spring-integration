/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.mqtt;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 *
 * @author Artem Bilan
 *
 * @since 5.5.5
 */
@Testcontainers(disabledWithoutDocker = true)
public interface MosquittoContainerTest {

	@Container
	GenericContainer<?> MOSQUITTO_CONTAINER =
			new GenericContainer<>("eclipse-mosquitto:2.0.12")
					.withCommand("mosquitto -c /mosquitto-no-auth.conf")
					.withReuse(true)
					.withExposedPorts(1883);

}
