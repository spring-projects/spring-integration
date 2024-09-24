/*
 * Copyright 2017-2024 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.TestExecutionListeners;

/**
 * Annotation that can be specified on a test class that runs Spring Integration based tests.
 * Provides the following features over and above the regular <em>Spring TestContext
 * Framework</em>:
 * <ul>
 * <li>Registers a {@link MockIntegrationContext} bean with the
 * {@link MockIntegrationContext#MOCK_INTEGRATION_CONTEXT_BEAN_NAME} which can be used
 * in tests for mocking and verifying integration flows.
 * </li>
 * <li>Registers an {@link SpringIntegrationTestExecutionListener} which is used
 * to customize {@link org.springframework.integration.endpoint.AbstractEndpoint}
 * beans with provided options on this annotation before/after the test class.
 * </li>
 * </ul>
 * <p>
 * The typical usage of this annotation is like:
 * <pre class="code">
 * &#064;SpringJUnitConfig
 * &#064;SpringIntegrationTest
 * public class MyIntegrationTests {
 *
 *    &#064;Autowired
 *    private MockIntegrationContext mockIntegrationContext;
 *
 * }
 * </pre>
 * <p>
 * Honors the {@link org.springframework.test.context.NestedTestConfiguration} semantics.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see MockIntegrationContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@TestExecutionListeners(listeners = SpringIntegrationTestExecutionListener.class,
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface SpringIntegrationTest {

	/**
	 * Specify a simple matching patterns ("xxx*", "*xxx", "*xxx*" or "xxx*yyy") for
	 * {@link org.springframework.integration.endpoint.AbstractEndpoint}
	 * bean names to mark them as {@code autoStartup = false}
	 * during context initialization.
	 * @return the endpoints name patterns to stop during context initialization
	 * @see SpringIntegrationTestExecutionListener
	 * @see org.springframework.util.PatternMatchUtils
	 */
	String[] noAutoStartup() default {};

}
