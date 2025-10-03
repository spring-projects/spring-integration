/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file;

import java.io.File;

import org.springframework.expression.Expression;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.Message;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation
 * that writes the Message payload to a
 * file. If the payload is a File object, it will copy the File to the specified
 * destination directory. If the payload is a byte array, a String or an
 * InputStream it will be written directly. Otherwise, the payload type is
 * unsupported, and an Exception will be thrown.
 * <p>
 * To append a new-line after each write, set the
 * {@link #setAppendNewLine(boolean) appendNewLine} flag to 'true'. It is 'false' by default.
 * <p>
 * If the 'deleteSourceFiles' flag is set to true, the original Files will be
 * deleted. The default value for that flag is <em>false</em>. See the
 * {@link #setDeleteSourceFiles(boolean)} method javadoc for more information.
 * <p>
 * Other transformers may be useful to precede this handler. For example, any
 * Serializable object payload can be converted into a byte array by the
 * {@link org.springframework.integration.transformer.PayloadSerializingTransformer}.
 * Likewise, any Object can be converted to a String based on its
 * <code>toString()</code> method by the
 * {@link org.springframework.integration.transformer.ObjectToStringTransformer}.
 * <p>
 * {@link FileExistsMode#APPEND} adds content to an existing file; the file is closed after
 * each write.
 * {@link FileExistsMode#APPEND_NO_FLUSH} adds content to an existing file and the file
 * is left open without flushing any data. Data will be flushed based on the
 * {@link #setFlushInterval(long) flushInterval} or when a message is sent to the
 * {@link #trigger(Message)} method, or a
 * {@link #flushIfNeeded(MessageFlushPredicate, Message) flushIfNeeded}
 * method is called.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Tony Falabella
 * @author Alen Turkovic
 * @author Trung Pham
 * @author Christian Tzolov
 * @author Ngoc Nhan
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.file.outbound.FileWritingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class FileWritingMessageHandler extends org.springframework.integration.file.outbound.FileWritingMessageHandler {

	/**
	 * Constructor which sets the directory for target files to store.
	 * @param destinationDirectory must not be null
	 * @see #FileWritingMessageHandler(Expression)
	 */
	public FileWritingMessageHandler(File destinationDirectory) {
		super(destinationDirectory);
	}

	/**
	 * Constructor which sets the SpEL directory for target files to store.
	 * @param destinationDirectoryExpression must not be null
	 * @see #FileWritingMessageHandler(File)
	 */
	public FileWritingMessageHandler(Expression destinationDirectoryExpression) {
		super(destinationDirectoryExpression);
	}

}
