/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.smb;

import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.smb.outbound.SmbMessageHandler;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.messaging.support.GenericMessage;

import jcifs.DialectVersion;

/**
 * Starts the Spring Context and will initialize the Spring Integration routes.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gregory Bragg
 *
 * @since 1.0
 *
 */
public final class Main {

	private static final Log LOGGER = LogFactory.getLog(Main.class);

	private Main() { }

	/**
	 * Load the Spring Integration Application Context
	 *
	 * @param args - command line arguments
	 */
	public static void main(final String... args) {

		final Scanner scanner = new Scanner(System.in);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("\n================================================================="
					+ "\n                                                                 "
					+ "\n    Welcome to the Spring Integration SMB Test Client            "
					+ "\n                                                                 "
					+ "\n    For more information please visit:                           "
					+ "\n  https://github.com/SpringSource/spring-integration-extensions  "
					+ "\n                                                                 "
					+ "\n=================================================================");
		}

		final GenericXmlApplicationContext context = new GenericXmlApplicationContext();

		LOGGER.info("Please enter the: ");
		LOGGER.info("\t- SMB Host");
		LOGGER.info("\t- SMB Share and Directory");
		LOGGER.info("\t- SMB Username");
		LOGGER.info("\t- SMB Password");

		LOGGER.info("Host: ");
		final String host = scanner.nextLine();

		LOGGER.info("Share and Directory (e.g. myFile/path/to/): ");
		final String shareAndDir = scanner.nextLine();

		LOGGER.info("Username (e.g. guest): ");
		final String username = scanner.nextLine();

		LOGGER.info("Password (can be empty): ");
		final String password = scanner.nextLine();

		context.getEnvironment().getSystemProperties().put("host", host);
		context.getEnvironment().getSystemProperties().put("shareAndDir", shareAndDir);
		context.getEnvironment().getSystemProperties().put("username", username);
		context.getEnvironment().getSystemProperties().put("password", password);

		context.load("classpath:META-INF/spring/integration/*-context.xml");
		context.registerShutdownHook();
		context.refresh();

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("\n========================================================="
					+ "\n                                                         "
					+ "\n    Please press 'q + Enter' to quit the application.    "
					+ "\n                                                         "
					+ "\n=========================================================");
		}

		SmbSessionFactory smbSessionFactory = context.getBean("smbSession", SmbSessionFactory.class);
		smbSessionFactory.setSmbMinVersion(DialectVersion.SMB210);
		smbSessionFactory.setSmbMaxVersion(DialectVersion.SMB311);

		LOGGER.info("Polling from Share: " + smbSessionFactory.getUrl());

		// Create a test text file on the SMB file share
		SmbMessageHandler handlerTxt = new SmbMessageHandler(smbSessionFactory);
		handlerTxt.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handlerTxt.setFileNameGenerator(message -> "handlerContent.txt");
		handlerTxt.setAutoCreateDirectory(true);
		handlerTxt.setUseTemporaryFileName(false);
		handlerTxt.setBeanFactory(context.getBeanFactory());
		handlerTxt.afterPropertiesSet();
		handlerTxt.handleMessage(new GenericMessage<String>("hello, my text"));

		// Create a test binary file on the SMB file share using a temporary filename
		SmbMessageHandler handlerBin = new SmbMessageHandler(smbSessionFactory);
		handlerBin.setRemoteDirectoryExpression(new LiteralExpression("remote-target-dir"));
		handlerBin.setFileNameGenerator(message -> "handlerContent.bin");
		handlerBin.setAutoCreateDirectory(true);
		handlerBin.setUseTemporaryFileName(true);
		handlerBin.setBeanFactory(context.getBeanFactory());
		handlerBin.afterPropertiesSet();
		handlerBin.handleMessage(new GenericMessage<byte[]>("hello, my bytes".getBytes()));

		while (true) {
			final String input = scanner.nextLine();

			if ("q".equals(input.trim())) {
				scanner.close();
				context.close();
				break;
			}
		}

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Exiting application...bye.");
		}

		System.exit(0);
	}

}
