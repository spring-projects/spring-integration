package org.springframework.integration.file;

import java.io.File;

/**
 * A callback for newly created files in {@link FileWritingMessageHandler}.
 *
 * @author Alen Turkovic
 */
public interface NewFileCallback {

	void handle(File file);

}
