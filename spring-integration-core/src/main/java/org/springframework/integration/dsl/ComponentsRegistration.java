/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Map;

/**
 * The marker interface for the {@link IntegrationComponentSpec} implementation,
 * when there is need to register as beans not only the target spec's components,
 * but some additional components, e.g. {@code subflows} from
 * {@link org.springframework.integration.dsl.RouterSpec}.
 * <p>
 * For internal use only.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
@FunctionalInterface
public interface ComponentsRegistration {

	Map<Object, String> getComponentsToRegister();

}
