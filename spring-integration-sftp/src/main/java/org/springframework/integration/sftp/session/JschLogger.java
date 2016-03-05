/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Logger;

/**
 * @author Oleg Zhurakousky
 * @since 2.0.1
 */
class JschLogger implements Logger {

	private final static Log logger = LogFactory.getLog("com.jcraft.jsch");

	public boolean isEnabled(int level) {
		switch (level) {
		case Logger.INFO:
			return logger.isInfoEnabled();
		case Logger.WARN:
			return logger.isWarnEnabled();
		case Logger.DEBUG:
			return logger.isDebugEnabled();
		case Logger.ERROR:
			return logger.isErrorEnabled();
		case Logger.FATAL:
			return logger.isFatalEnabled();
		default:
			return false;
		}
	}

	public void log(int level, String message) {

		switch (level) {
		case Logger.INFO:
			logger.info(message);
			break;
		case Logger.WARN:
			logger.warn(message);
			break;
		case Logger.DEBUG:
			logger.debug(message);
			break;
		case Logger.ERROR:
			logger.error(message);
			break;
		case Logger.FATAL:
			logger.fatal(message);
			break;
		default:
			break;
		}
	}
}
