/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Adama Sorho
 *
 * @since 4.2
 *
 */
public class LastModifiedFileListFilterTests {

	@TempDir
	public File folder;

	@Test
	public void testAge() throws Exception {
		LastModifiedFileListFilter filter = new LastModifiedFileListFilter();
		filter.setAge(60);
		File foo = new File(folder, "test.tmp");
		FileOutputStream fileOutputStream = new FileOutputStream(foo);
		fileOutputStream.write("x".getBytes());
		fileOutputStream.close();
		assertThat(filter.filterFiles(new File[] {foo})).hasSize(0);
		assertThat(filter.accept(foo)).isFalse();
		foo.setLastModified(Instant.now().minus(Duration.ofDays(1)).toEpochMilli());
		assertThat(filter.filterFiles(new File[] {foo})).hasSize(1);
		assertThat(filter.accept(foo)).isTrue();
	}

}
