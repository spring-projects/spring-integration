/**
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.smb.session;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Data holder class for a SMB share configuration.
 *
 * SmbFile URLs syntax:
 *      smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]
 *
 * @author Markus Spann
 * @since 2.1.1
 */
public class SmbConfig {

	private String  host;
	private int     port;
	private String  domain;
	private String  username;
	private String  password;
	private String  shareAndDir;

	private boolean replaceFile = false;
	private boolean useTempFile = false;

	public SmbConfig() {
	}

	public SmbConfig(String _host, int _port, String _domain, String _username, String _password, String _shareAndDir) throws UnsupportedEncodingException {
		this();

		setHost(_host);
		setPort(_port);
		setDomain(_domain);
		setUsername(_username);
		setPassword(_password);
		setShareAndDir(_shareAndDir);
	}

	public void setHost(String _host) {
		Assert.hasText(_host, "host must not be empty");
		this.host = _host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(int _port) {
		Assert.isTrue(_port >= 0, "port must be >= 0");
		this.port = _port;
	}

	public int getPort() {
		return port;
	}

	public void setDomain(String _domain) {
		Assert.notNull(_domain);
		this.domain = _domain;
	}

	public String getDomain() {
		return domain;
	}

	public void setUsername(String _username) {
		Assert.hasText(_username, "username should be a non-empty string");
		this.username = _username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String _password) {
		Assert.notNull(_password, "password should not be null");
		this.password = _password;
	}

	public String getPassword() {
		return password;
	}

	public void setShareAndDir(String _shareAndDir) {
		Assert.notNull(_shareAndDir, "shareAndDir should not be null");
		this.shareAndDir = _shareAndDir;
	}

	public String getShareAndDir() {
		return shareAndDir;
	}

	public void setReplaceFile(boolean _replaceFile) {
		this.replaceFile = _replaceFile;
	}

	public boolean isReplaceFile() {
		return replaceFile;
	}

	void setUseTempFile(boolean _useTempFile) {
		this.useTempFile = _useTempFile;
	}

	public boolean isUseTempFile() {
		return useTempFile;
	}

	String getDomainUserPass(boolean _includePassword) {
		String domainUserPass;
		String user = _includePassword ? this.username : "********";
		if (StringUtils.hasText(this.domain)) {
			domainUserPass = String.format("%s;%s", this.domain, user);
		} else {
			domainUserPass = user;
		}
		if (StringUtils.hasText(this.password)) {
			domainUserPass += ":" + this.password;
		}
		return domainUserPass;
	}

	String getHostPort() {
		return this.host + (this.port > 0 ? String.format(":%d", this.port) : "");
	}

	/**
	 * Validates the object. Throws run-time exception if found to be invalid.
	 * @return the object
	 */
	public final SmbConfig validate() {
        Assert.hasText(getHost(), "host must not be empty in " + this);
        Assert.isTrue(getPort() >= 0, "port must be >= 0 in " + this);
		Assert.hasText(getShareAndDir(), "share must not be empty in " + this);
		return this;
	}

	public final String getUrl() {
		return getUrl(true);
	}

	public final String getUrl(boolean _includePassword) {
	    String domainUserPass = getDomainUserPass(_includePassword);
		if (domainUserPass != null) {
			try {
				domainUserPass = URLEncoder.encode(domainUserPass, "UTF8");
				// CHECKSTYLE:OFF
			} catch (UnsupportedEncodingException _ex) {
				// CHECKSTYLE:ON
			}
		}
		return String.format("smb://%s@%s/%s", domainUserPass, getHostPort(), StringUtils.cleanPath(this.shareAndDir));
    }

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[url=" + getUrl(false)
				+ ", replaceFile=" + replaceFile
				+ "]";
	}

}
