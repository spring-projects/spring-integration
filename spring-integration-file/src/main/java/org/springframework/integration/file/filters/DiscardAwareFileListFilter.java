/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.util.function.Consumer;

import org.springframework.lang.Nullable;

/**
 * The {@link FileListFilter} modification which can accept a {@link Consumer}
 * which can be called when the filter discards the file.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0.5
 */
public interface DiscardAwareFileListFilter<F> extends FileListFilter<F> {

	void addDiscardCallback(@Nullable Consumer<F> discardCallback);

}
