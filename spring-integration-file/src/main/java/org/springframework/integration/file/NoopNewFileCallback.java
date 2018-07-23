package org.springframework.integration.file;

import java.io.File;

/**
 * A {@link NewFileCallback} which does nothing for new files.
 *
 * @author Alen Turkovic
 */
public class NoopNewFileCallback implements NewFileCallback {

	@Override
	public void handle(final File file) {
		// do nothing...
	}

}
