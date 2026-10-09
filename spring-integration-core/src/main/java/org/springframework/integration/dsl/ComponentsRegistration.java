/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.Map;

import org.jspecify.annotations.Nullable;

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

	Map<Object, @Nullable String> getComponentsToRegister();

}
