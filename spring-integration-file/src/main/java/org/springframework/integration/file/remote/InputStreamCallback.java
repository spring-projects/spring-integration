/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.remote;

import java.io.IOException;
import java.io.InputStream;

/**
 * Callback for stream-based file retrieval using a RemoteFileOperations.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
@FunctionalInterface
public interface InputStreamCallback {

	/**
	 * Called with the InputStream for the remote file. The caller will
	 * take care of closing the stream and finalizing the file retrieval operation after
	 * this method exits.
	 * @param stream The InputStream.
	 * @throws IOException Any IOException.
	 */
	void doWithInputStream(InputStream stream) throws IOException;

}
