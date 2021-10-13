/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ftp.gateway;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.ClientCallbackWithoutResult;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.FtpFileInfo;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.messaging.Message;

/**
 * Outbound Gateway for performing remote file operations via FTP/FTPS.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class FtpOutboundGateway extends AbstractRemoteFileOutboundGateway<FTPFile> {

	private Expression workingDirExpression;

	private StandardEvaluationContext evaluationContext;

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public FtpOutboundGateway(SessionFactory<FTPFile> sessionFactory,
			MessageSessionCallback<FTPFile, ?> messageSessionCallback) {

		this(new FtpRemoteFileTemplate(sessionFactory), messageSessionCallback);
		((FtpRemoteFileTemplate) getRemoteFileTemplate()).setExistsMode(FtpRemoteFileTemplate.ExistsMode.NLST);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template and callback
	 * for performing operations on the session.
	 * @param remoteFileTemplate the remote file template.
	 * @param messageSessionCallback the callback.
	 */
	public FtpOutboundGateway(RemoteFileTemplate<FTPFile> remoteFileTemplate,
			MessageSessionCallback<FTPFile, ?> messageSessionCallback) {

		super(remoteFileTemplate, messageSessionCallback);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public FtpOutboundGateway(SessionFactory<FTPFile> sessionFactory, String command, String expression) {
		this(new FtpRemoteFileTemplate(sessionFactory), command, expression);
		((FtpRemoteFileTemplate) getRemoteFileTemplate()).setExistsMode(FtpRemoteFileTemplate.ExistsMode.NLST);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public FtpOutboundGateway(RemoteFileTemplate<FTPFile> remoteFileTemplate, String command, String expression) {
		super(remoteFileTemplate, command, expression);
	}

	/**
	 * Construct an instance with the supplied session factory
	 * and command ('ls', 'nlst', 'put' or 'mput').
	 * <p> The {@code remoteDirectory} expression is {@code null} assuming to use
	 * the {@code workingDirectory} from the FTP Client.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @since 4.3
	 */
	public FtpOutboundGateway(SessionFactory<FTPFile> sessionFactory, String command) {
		this(sessionFactory, command, null);
	}

	/**
	 * Construct an instance with the supplied remote file template
	 * and command ('ls', 'nlst', 'put' or 'mput').
	 * <p> The {@code remoteDirectory} expression is {@code null} assuming to use
	 * the {@code workingDirectory} from the FTP Client.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @since 4.3
	 */
	public FtpOutboundGateway(RemoteFileTemplate<FTPFile> remoteFileTemplate, String command) {
		this(remoteFileTemplate, command, null);
	}

	/**
	 * Specify an {@link Expression} to evaluate FTP client working directory
	 * against request message.
	 * @param workingDirExpression the expression to evaluate working directory
	 * @since 5.0
	 */
	public void setWorkingDirExpression(Expression workingDirExpression) {
		this.workingDirExpression = workingDirExpression;
	}

	/**
	 * Specify a SpEL {@link Expression} to evaluate FTP client working directory
	 * against request message.
	 * @param workingDirExpression the SpEL expression to evaluate working directory
	 * @since 5.0
	 */
	public void setWorkingDirExpressionString(String workingDirExpression) {
		setWorkingDirExpression(EXPRESSION_PARSER.parseExpression(workingDirExpression));
	}

	@Override
	public String getComponentType() {
		return "ftp:outbound-gateway";
	}

	@Override
	protected boolean isDirectory(FTPFile file) {
		return file.isDirectory();
	}

	@Override
	protected boolean isLink(FTPFile file) {
		return file.isSymbolicLink();
	}

	@Override
	protected String getFilename(FTPFile file) {
		return file.getName();
	}

	@Override
	protected String getFilename(AbstractFileInfo<FTPFile> file) {
		return file.getFilename();
	}

	@Override
	protected long getModified(FTPFile file) {
		return file.getTimestamp().getTimeInMillis();
	}

	@Override
	protected List<AbstractFileInfo<FTPFile>> asFileInfoList(Collection<FTPFile> files) {
		List<AbstractFileInfo<FTPFile>> canonicalFiles = new ArrayList<>();
		for (FTPFile file : files) {
			canonicalFiles.add(new FtpFileInfo(file));
		}
		return canonicalFiles;
	}

	@Override
	protected void doInit() {
		super.doInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected FTPFile enhanceNameWithSubDirectory(FTPFile file, String directory) {
		file.setName(directory + file.getName());
		return file;
	}

	@Override
	protected List<?> ls(Message<?> message, Session<FTPFile> session, String dir) throws IOException {
		return doInWorkingDirectory(message, session, () -> super.ls(message, session, dir));
	}

	@Override
	protected List<String> nlst(Message<?> message, Session<FTPFile> session, String dir) throws IOException {
		return doInWorkingDirectory(message, session, () -> super.nlst(message, session, dir));
	}

	@Override
	protected File get(Message<?> message, Session<FTPFile> session, String remoteDir, String remoteFilePath,
			String remoteFilename, FTPFile fileInfoParam) throws IOException {

		return doInWorkingDirectory(message, session,
				() -> super.get(message, session, remoteDir, remoteFilePath, remoteFilename, fileInfoParam));
	}

	@Override
	protected List<File> mGet(Message<?> message, Session<FTPFile> session, String remoteDirectory,
			String remoteFilename) throws IOException {

		return doInWorkingDirectory(message, session,
				() -> super.mGet(message, session, remoteDirectory, remoteFilename));
	}

	@Override
	protected boolean rm(Message<?> message, Session<FTPFile> session, String remoteFilePath) throws IOException {
		return doInWorkingDirectory(message, session, () -> super.rm(message, session, remoteFilePath));
	}

	@Override
	protected boolean mv(Message<?> message, Session<FTPFile> session, String remoteFilePath, String remoteFileNewPath)
			throws IOException {

		return doInWorkingDirectory(message, session,
				() -> super.mv(message, session, remoteFilePath, remoteFileNewPath));
	}

	@Override
	protected String put(Message<?> message, Session<FTPFile> session, String subDirectory) {
		try {
			return doInWorkingDirectory(message, session,
					() -> super.put(message, session, subDirectory));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	protected List<String> mPut(Message<?> message, Session<FTPFile> session, File localDir) {
		try {
			return doInWorkingDirectory(message, session,
					() -> super.mPut(message, session, localDir));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private <V> V doInWorkingDirectory(Message<?> message, Session<FTPFile> session, Callable<V> task)
			throws IOException {

		Expression workDirExpression = this.workingDirExpression;
		FTPClient ftpClient = (FTPClient) session.getClientInstance();
		String currentWorkingDirectory = null;
		boolean restoreWorkingDirectory = false;
		try {
			if (workDirExpression != null) {
				currentWorkingDirectory = ftpClient.printWorkingDirectory();
				String newWorkingDirectory =
						workDirExpression.getValue(this.evaluationContext, message, String.class);
				if (!Objects.equals(currentWorkingDirectory, newWorkingDirectory)) {
					ftpClient.changeWorkingDirectory(newWorkingDirectory);
					restoreWorkingDirectory = true;
				}
			}
			return task.call();
		}
		catch (Exception ex) {
			throw rethrowAsIoExceptionIfAny(ex);
		}
		finally {
			if (restoreWorkingDirectory) {
				ftpClient.changeWorkingDirectory(currentWorkingDirectory);
			}
		}
	}

	private IOException rethrowAsIoExceptionIfAny(Exception ex) {
		if (ex instanceof IOException) {
			return (IOException) ex;

		}
		else if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		else {
			return new IOException("Uncategorized IO exception", ex);
		}
	}

	@Override
	public boolean isChmodCapable() {
		return true;
	}

	@Override
	protected void doChmod(RemoteFileOperations<FTPFile> remoteFileOperations, final String path, final int chmod) {
		remoteFileOperations.executeWithClient((ClientCallbackWithoutResult<FTPClient>) client -> {
			String chModCommand = "chmod " + Integer.toOctalString(chmod) + " " + path;
			try {
				client.sendSiteCommand(chModCommand);
			}
			catch (IOException e) {
				throw new UncheckedIOException("Failed to execute '" + chModCommand + "'", e);
			}
		});
	}

}
