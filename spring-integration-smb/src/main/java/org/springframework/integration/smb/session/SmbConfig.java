/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.integration.smb.session;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jcifs.DialectVersion;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Data holder class for an SMB share configuration.
 *<p>
 * SmbFile URLs syntax:
 *      smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]
 *
 * @author Markus Spann
 * @author Prafull Kumar Soni
 * @author Artem Bilan
 * @author Gregory Bragg
 * @author Jelle Smits
 *
 * @since 6.0
 */
public class SmbConfig {

	private String host;

	private int port;

	private String domain;

	private String username;

	private String password;

	private String shareAndDir;

	/**
	 * Defaults to and follows the jCIFS library default of 'SMB1'.
	 */
	private DialectVersion smbMinVersion = DialectVersion.SMB1;

	/**
	 * Defaults to and follows the jCIFS library default of 'SMB210'.
	 */
	private DialectVersion smbMaxVersion = DialectVersion.SMB210;

	public SmbConfig() {
	}

	public SmbConfig(String _host, int _port, String _domain, String _username, String _password, String _shareAndDir) {
		setHost(_host);
		setPort(_port);
		setDomain(_domain);
		setUsername(_username);
		setPassword(_password);
		setShareAndDir(_shareAndDir);
	}

	public final void setHost(String _host) {
		Assert.hasText(_host, "host must not be empty");
		this.host = _host;
	}

	public String getHost() {
		return this.host;
	}

	public final void setPort(int _port) {
		Assert.isTrue(_port >= 0, "port must be >= 0");
		this.port = _port;
	}

	public int getPort() {
		return this.port;
	}

	public final void setDomain(String _domain) {
		Assert.notNull(_domain, "_domain can't be null");
		this.domain = _domain;
	}

	public String getDomain() {
		return this.domain;
	}

	public final void setUsername(String _username) {
		Assert.hasText(_username, "username should be a non-empty string");
		this.username = _username;
	}

	public String getUsername() {
		return this.username;
	}

	public final void setPassword(String _password) {
		Assert.notNull(_password, "password should not be null");
		this.password = _password;
	}

	public String getPassword() {
		return this.password;
	}

	public final void setShareAndDir(String _shareAndDir) {
		Assert.notNull(_shareAndDir, "shareAndDir should not be null");
		this.shareAndDir = _shareAndDir;
	}

	public String getShareAndDir() {
		return this.shareAndDir;
	}

	/**
	 * Gets the desired minimum SMB version value for what the Windows server will allow
	 * during protocol transport negotiation.
	 * @return one of SMB1, SMB202, SMB210, SMB300, SMB302 or SMB311
	 */
	public DialectVersion getSmbMinVersion() {
		return this.smbMinVersion;
	}

	/**
	 * Sets the desired minimum SMB version value for what the Windows server will allow
	 * during protocol transport negotiation.
	 * @param _smbMinVersion one of SMB1, SMB202, SMB210, SMB300, SMB302 or SMB311
	 */
	public void setSmbMinVersion(DialectVersion _smbMinVersion) {
		this.smbMinVersion = _smbMinVersion;
	}

	/**
	 * Gets the desired maximum SMB version value for what the Windows server will allow
	 * during protocol transport negotiation.
	 * @return one of SMB1, SMB202, SMB210, SMB300, SMB302 or SMB311
	 */
	public DialectVersion getSmbMaxVersion() {
		return this.smbMaxVersion;
	}

	/**
	 * Sets the desired maximum SMB version value for what the Windows server will allow
	 * during protocol transport negotiation.
	 * @param _smbMaxVersion one of SMB1, SMB202, SMB210, SMB300, SMB302 or SMB311
	 */
	public void setSmbMaxVersion(DialectVersion _smbMaxVersion) {
		this.smbMaxVersion = _smbMaxVersion;
	}

	String getDomainUserPass(boolean _includePassword, boolean _urlEncode) {
		String domainUserPass;
		String username = _urlEncode ? URLEncoder.encode(this.username, StandardCharsets.UTF_8) : this.username;
		String password = _urlEncode ? URLEncoder.encode(this.password, StandardCharsets.UTF_8) : this.password;
		if (StringUtils.hasText(this.domain)) {
			String domain = _urlEncode ? URLEncoder.encode(this.domain, StandardCharsets.UTF_8) : this.domain;
			domainUserPass = String.format("%s;%s", domain, username);
		}
		else {
			domainUserPass = username;
		}
		if (StringUtils.hasText(password)) {
			domainUserPass += ":" + (_includePassword ? password : "********");
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
		Assert.hasText(getHost(), () -> "host must not be empty in " + this);
		Assert.isTrue(getPort() >= 0, () -> "port must be >= 0 in " + this);
		Assert.hasText(getShareAndDir(), () -> "share must not be empty in " + this);
		return this;
	}

	public final String getUrl() {
		return getUrl(true);
	}

	public final String getUrl(boolean _includePassword) {
		return createUri(_includePassword).toASCIIString();
	}

	/**
	 * Return the url string for the share connection without encoding.
	 * Used in the {@link SmbShare} constructor delegation.
	 * @return the url string for the share connection without encoding.
	 * @since 6.3.8
	 */
	public final String rawUrl() {
		return rawUrl(true);
	}

	/**
	 * Return the url string for the share connection without encoding
	 * the host and path. The {@code domainUserPass} is encoded, as
	 * {@link java.net.URL} requires them to be encoded otherwise its parsing fails.
	 * Used in the {@link SmbShare} constructor delegation.
	 * @param _includePassword whether password has to be masked in credentials of URL.
	 * @return the url string for the share connection without encoding.
	 * @since 6.3.8
	 */
	public final String rawUrl(boolean _includePassword) {
		String domainUserPass = getDomainUserPass(_includePassword, true);
		String path = cleanPath();
		return "smb://%s@%s%s".formatted(domainUserPass, getHostPort(), path);
	}

	private URI createUri(boolean _includePassword) {
		String domainUserPass = getDomainUserPass(_includePassword, false);
		String path = cleanPath();
		try {
			return new URI("smb", domainUserPass, this.host, this.port, path, null, null);
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private String cleanPath() {
		String path = StringUtils.cleanPath(this.shareAndDir);

		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return path;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "[url=" + getUrl(false)
				+ "]";
	}

}
