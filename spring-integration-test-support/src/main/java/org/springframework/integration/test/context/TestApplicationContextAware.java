/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.test.context;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.integration.test.util.TestUtils;

public interface TestApplicationContextAware {
	TestUtils.TestApplicationContext CONTEXT = TestUtils.createTestApplicationContext();

	@BeforeAll
	static void beforeAll() {
		try {
			CONTEXT.refresh();
		}
		catch (IllegalStateException ex) {
			if (!ex.getMessage().contains("just call 'refresh' once")) {
				throw new RuntimeException(ex);
			}

		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@AfterAll
	static void tearDown() {
		CONTEXT.close();
	}

}
