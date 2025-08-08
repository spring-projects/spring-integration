/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.remote.session;

/**
 * Factory for acquiring {@link Session} instances.
 *
 * @param <F> the target system file type.
 *
 * @author Mark Fisher
 *
 * @since 2.0
 */
@FunctionalInterface
public interface SessionFactory<F> {

	Session<F> getSession();

}
