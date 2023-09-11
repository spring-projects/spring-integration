/*
 * Copyright 2023 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import jcifs.smb.SmbFile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Adama Sorho
 * @since 6.2
 */
public class SmbLastModifiedFileListFilterTests {

	@Test
	public void testAge() {
		SmbLastModifiedFileListFilter filter = new SmbLastModifiedFileListFilter();
		filter.setAge(80, TimeUnit.SECONDS);
		SmbFile smbFile1 = mock(SmbFile.class);
		when(smbFile1.getLastModified()).thenReturn(System.currentTimeMillis());
		SmbFile smbFile2 = mock(SmbFile.class);
		when(smbFile2.getLastModified()).thenReturn(System.currentTimeMillis());
		SmbFile smbFile3 = mock(SmbFile.class);

		// Make a file as of yesterday's
		when(smbFile3.getLastModified()).thenReturn(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		SmbFile[] files = new SmbFile[] {smbFile1, smbFile2, smbFile3};
		assertThat(filter.filterFiles(files)).hasSize(1);
		assertThat(filter.accept(smbFile1)).isFalse();
		assertThat(filter.accept(smbFile3)).isTrue();
	}

}
