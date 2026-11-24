/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.filters;

import java.util.function.Consumer;

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

	void addDiscardCallback(Consumer<F> discardCallback);

}
