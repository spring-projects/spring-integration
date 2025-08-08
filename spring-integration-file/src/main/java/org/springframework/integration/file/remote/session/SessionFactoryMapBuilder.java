/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.file.remote.session;

import org.springframework.integration.support.MapBuilder;

/**
 * A {@link MapBuilder} to producer a map that maps objects to {@link SessionFactory}s.
 *
 * @param <T> the target system file type.
 *
 * @author Gary Russell
 *
 * @since 5.0.7
 *
 */
public class SessionFactoryMapBuilder<T> extends MapBuilder<SessionFactoryMapBuilder<T>, Object, SessionFactory<T>> {

}
