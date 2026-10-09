/*
 * Copyright 2016-present the original author or authors.
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
