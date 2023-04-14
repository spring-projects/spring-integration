/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.file.dsl;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageHandlerSpec} for the {@link FileTransferringMessageHandler}.
 *
 * @param <F> the target file type.
 * @param <S> the target {@link FileTransferringMessageHandlerSpec} implementation type.
 *
 * @author Artem Bilan
 * @author Joaquin Santana
 *
 * @since 5.0
 */
public abstract class FileTransferringMessageHandlerSpec<F, S extends FileTransferringMessageHandlerSpec<F, S>>
		extends MessageHandlerSpec<S, FileTransferringMessageHandler<F>>
		implements ComponentsRegistration {

	@Nullable
	private FileNameGenerator fileNameGenerator;

	@Nullable
	private DefaultFileNameGenerator defaultFileNameGenerator;

	// TODO: should be refactored using generics in next release (breaking change), see PR-3080.
	protected FileTransferringMessageHandlerSpec() {
	}

	protected FileTransferringMessageHandlerSpec(SessionFactory<F> sessionFactory) {
		this.target = new FileTransferringMessageHandler<>(sessionFactory);
	}

	protected FileTransferringMessageHandlerSpec(RemoteFileTemplate<F> remoteFileTemplate) {
		this.target = new FileTransferringMessageHandler<>(remoteFileTemplate);
	}

	protected FileTransferringMessageHandlerSpec(RemoteFileTemplate<F> remoteFileTemplate,
			FileExistsMode fileExistsMode) {

		this.target = new FileTransferringMessageHandler<>(remoteFileTemplate, fileExistsMode);
	}

	/**
	 * A {@code boolean} flag to indicate automatically create the directory or not.
	 * @param autoCreateDirectory true to automatically create the directory.
	 * @return the current Spec
	 */
	public S autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	/**
	 * Specify a remote file separator symbol.
	 * @param remoteFileSeparator the remote file separator.
	 * @return the current Spec
	 */
	public S remoteFileSeparator(String remoteFileSeparator) {
		this.target.setRemoteFileSeparator(remoteFileSeparator);
		return _this();
	}

	/**
	 * Specify a remote directory path.
	 * @param remoteDirectory the remote directory path.
	 * @return the current Spec
	 */
	public S remoteDirectory(String remoteDirectory) {
		this.target.setRemoteDirectoryExpression(new LiteralExpression(remoteDirectory));
		return _this();
	}

	/**
	 * Specify a remote directory path SpEL expression.
	 * @param remoteDirectoryExpression the remote directory expression
	 * @return the current Spec
	 */
	public S remoteDirectoryExpression(String remoteDirectoryExpression) {
		this.target.setRemoteDirectoryExpression(PARSER.parseExpression(remoteDirectoryExpression));
		return _this();
	}

	/**
	 * Specify a remote directory path {@link Function}.
	 * @param remoteDirectoryFunction the remote directory {@link Function}
	 * @param <P> the expected payload type.
	 * @return the current Spec
	 */
	public <P> S remoteDirectory(Function<Message<P>, String> remoteDirectoryFunction) {
		this.target.setRemoteDirectoryExpression(new FunctionExpression<>(remoteDirectoryFunction));
		return _this();
	}

	/**
	 * Specify a remote directory path.
	 * @param temporaryRemoteDirectory the temporary remote directory path
	 * @return the current Spec
	 */
	public S temporaryRemoteDirectory(String temporaryRemoteDirectory) {
		this.target.setTemporaryRemoteDirectoryExpression(new LiteralExpression(temporaryRemoteDirectory));
		return _this();
	}

	/**
	 * Specify a remote directory path SpEL expression.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory path SpEL expression
	 * @return the current Spec
	 */
	public S temporaryRemoteDirectoryExpression(String temporaryRemoteDirectoryExpression) {
		this.target.setTemporaryRemoteDirectoryExpression(PARSER.parseExpression(temporaryRemoteDirectoryExpression));
		return _this();
	}

	/**
	 * Specify a remote temporary directory path {@link Function}.
	 * @param temporaryRemoteDirectoryFunction the temporary remote directory {@link Function}
	 * @param <P> the expected payload type.
	 * @return the current Spec
	 */
	public <P> S temporaryRemoteDirectory(Function<Message<P>, String> temporaryRemoteDirectoryFunction) {
		this.target.setTemporaryRemoteDirectoryExpression(new FunctionExpression<>(temporaryRemoteDirectoryFunction));
		return _this();
	}

	/**
	 * A {@code boolean} flag to use temporary files names or not.
	 * Defaults to {@code true}.
	 * @param useTemporaryFileName true to use a temporary file name.
	 * @return the current Spec
	 */
	public S useTemporaryFileName(boolean useTemporaryFileName) {
		this.target.setUseTemporaryFileName(useTemporaryFileName);
		return _this();
	}

	/**
	 * Set the file name generator used to generate the remote filename to be used when transferring
	 * files to the remote system. Default {@link DefaultFileNameGenerator}.
	 * @param fileNameGenerator the file name generator.
	 * @return the current Spec
	 */
	public S fileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
		this.target.setFileNameGenerator(fileNameGenerator);
		return _this();
	}

	/**
	 * Set the {@link DefaultFileNameGenerator} based on the provided SpEL expression.
	 * @param fileNameGeneratorExpression the SpEL expression for file names generation.
	 * @return the current Spec
	 */
	public S fileNameExpression(String fileNameGeneratorExpression) {
		Assert.isNull(this.fileNameGenerator,
				"'fileNameGenerator' and 'fileNameGeneratorExpression' are mutually exclusive.");
		this.defaultFileNameGenerator = new DefaultFileNameGenerator();
		this.defaultFileNameGenerator.setExpression(fileNameGeneratorExpression);
		return fileNameGenerator(this.defaultFileNameGenerator);
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the
	 * remote file. Default {@code UTF-8}.
	 * @param charset the charset.
	 * @return the current Spec
	 */
	public S charset(String charset) {
		this.target.setCharset(charset);
		return _this();
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the
	 * remote file. Default {@code UTF-8}.
	 * @param charset the charset.
	 * @return the current Spec
	 */
	public S charset(Charset charset) {
		Assert.notNull(charset, "'charset' must not be null.");
		return charset(charset.name());
	}

	/**
	 * Set the temporary suffix to use when transferring files to the remote system.
	 * Default ".writing".
	 * @param temporaryFileSuffix the suffix
	 * @return the current Spec
	 */
	public S temporaryFileSuffix(String temporaryFileSuffix) {
		this.target.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	/**
	 * Set the file permissions after uploading, e.g. 0600 for
	 * owner read/write.
	 * @param chmod the permissions.
	 * @return the current Spec
	 */
	public S chmod(int chmod) {
		this.target.setChmod(chmod);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		if (this.defaultFileNameGenerator != null) {
			return Collections.singletonMap(this.defaultFileNameGenerator, null);
		}
		return Collections.emptyMap();
	}

}
