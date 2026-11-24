/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
