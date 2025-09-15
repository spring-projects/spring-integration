/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.ftp.session;

import java.io.Serial;
import java.time.Instant;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.jspecify.annotations.Nullable;

/**
 * The {@link FTPFile} extension to provide additional information,
 * e.g., long file name with directory included.
 * The instance of this class is based on the original {@link FTPFile}
 * with delegation from all the methods.
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class EnhancedFTPFile extends FTPFile {

	@Serial
	private static final long serialVersionUID = 9010790363003271996L;

	private final FTPFile delegate;

	private @Nullable String longFileName;

	public EnhancedFTPFile(FTPFile delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getGroup() {
		return this.delegate.getGroup();
	}

	@Override
	public int getHardLinkCount() {
		return this.delegate.getHardLinkCount();
	}

	@Override
	public String getLink() {
		return this.delegate.getLink();
	}

	@Override
	public String getName() {
		return this.delegate.getName();
	}

	@Override
	public String getRawListing() {
		return this.delegate.getRawListing();
	}

	@Override
	public long getSize() {
		return this.delegate.getSize();
	}

	@Override
	public Calendar getTimestamp() {
		return this.delegate.getTimestamp();
	}

	@Override
	public Instant getTimestampInstant() {
		return this.delegate.getTimestampInstant();
	}

	@Override
	public int getType() {
		return this.delegate.getType();
	}

	@Override
	public String getUser() {
		return this.delegate.getUser();
	}

	@Override
	public boolean hasPermission(int access, int permission) {
		return this.delegate.hasPermission(access, permission);
	}

	@Override
	public boolean isDirectory() {
		return this.delegate.isDirectory();
	}

	@Override
	public boolean isFile() {
		return this.delegate.isFile();
	}

	@Override
	public boolean isSymbolicLink() {
		return this.delegate.isSymbolicLink();
	}

	@Override
	public boolean isUnknown() {
		return this.delegate.isUnknown();
	}

	@Override
	public boolean isValid() {
		return this.delegate.isValid();
	}

	@Override
	public void setGroup(String group) {
		this.delegate.setGroup(group);
	}

	@Override
	public void setHardLinkCount(int hardLinkCount) {
		this.delegate.setHardLinkCount(hardLinkCount);
	}

	@Override
	public void setLink(String link) {
		this.delegate.setLink(link);
	}

	@Override
	public void setName(String name) {
		this.delegate.setName(name);
	}

	@Override
	public void setPermission(int access, int permission, boolean value) {
		this.delegate.setPermission(access, permission, value);
	}

	@Override
	public void setRawListing(String rawListing) {
		this.delegate.setRawListing(rawListing);
	}

	@Override
	public void setSize(long size) {
		this.delegate.setSize(size);
	}

	@Override
	public void setTimestamp(Calendar calendar) {
		this.delegate.setTimestamp(calendar);
	}

	@Override
	public void setType(int type) {
		this.delegate.setType(type);
	}

	@Override
	public void setUser(String user) {
		this.delegate.setUser(user);
	}

	@Override
	public String toFormattedString() {
		return this.delegate.toFormattedString();
	}

	@Override
	public String toFormattedString(String timezone) {
		return this.delegate.toFormattedString(timezone);
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	public @Nullable String getLongFileName() {
		return this.longFileName;
	}

	public void setLongFileName(@Nullable String longFileName) {
		this.longFileName = longFileName;
	}

}
