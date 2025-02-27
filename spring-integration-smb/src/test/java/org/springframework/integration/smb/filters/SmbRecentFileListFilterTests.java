/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.integration.smb.filters;

import java.time.Duration;
import java.time.Instant;

import jcifs.smb.SmbFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class SmbRecentFileListFilterTests {

	@Test
	public void testAge() {
		SmbRecentFileListFilter filter = new SmbRecentFileListFilter(Duration.ofHours(20));
		SmbFile smbFile1 = mock();
		when(smbFile1.getLastModified()).thenReturn(System.currentTimeMillis());
		SmbFile smbFile2 = mock();
		when(smbFile2.getLastModified()).thenReturn(System.currentTimeMillis());
		SmbFile smbFile3 = mock();

		when(smbFile3.getLastModified())
				.thenReturn(Instant.now().minus(Duration.ofDays(1)).toEpochMilli());

		SmbFile[] files = new SmbFile[] {smbFile1, smbFile2, smbFile3};

		assertThat(filter.filterFiles(files)).hasSize(2);
		assertThat(filter.accept(smbFile1)).isTrue();
		assertThat(filter.accept(smbFile2)).isTrue();
		assertThat(filter.accept(smbFile3)).isFalse();
	}

}
