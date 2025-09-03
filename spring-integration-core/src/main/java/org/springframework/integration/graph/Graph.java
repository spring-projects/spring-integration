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

package org.springframework.integration.graph;

import java.util.Collection;
import java.util.Map;

/**
 * This object can be exposed, for example, as a JSON object over HTTP.
 *
 * @param contentDescriptor the Spring Integration application attributes.
 * @param nodes the Spring Integration application endpoints.
 * @param links the Spring Integration application message channels between endpoints.
 *
 * @author Andy Clement
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public record Graph(
		Map<String, Object> contentDescriptor,
		Collection<IntegrationNode> nodes,
		Collection<LinkNode> links) {

}
