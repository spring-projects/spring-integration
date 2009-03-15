package org.springframework.integration.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Strategy that will allow a http request response exchange a with a remote
 * server.
 * 
 * 
 * @author Iwein Fuld
 * 
 */
public interface HttpExchanger {

	InputStream exchange(ByteArrayOutputStream requestBody) throws IOException;

	void setContentType(String contentType);

}
