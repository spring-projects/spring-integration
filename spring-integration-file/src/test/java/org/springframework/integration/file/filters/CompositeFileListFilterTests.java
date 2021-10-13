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

package org.springframework.integration.file.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Aaron Grant
 * @author Artem Bilan
 */
public class CompositeFileListFilterTests {

	@SuppressWarnings("unchecked")
	private final FileListFilter<File> fileFilterMock1 = mock(FileListFilter.class);

	@SuppressWarnings("unchecked")
	private final FileListFilter<File> fileFilterMock2 = mock(FileListFilter.class);

	private final File fileMock = mock(File.class);

	@Test
	public void forwardedToFilters() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new CompositeFileListFilter<>();
		compositeFileFilter.addFilter(fileFilterMock1);
		compositeFileFilter.addFilter(fileFilterMock2);
		List<File> returnedFiles = Collections.singletonList(fileMock);
		when(fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		when(fileFilterMock2.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		assertThat(compositeFileFilter.filterFiles(new File[]{ fileMock })).isEqualTo(returnedFiles);
		verify(fileFilterMock1).filterFiles(isA(File[].class));
		verify(fileFilterMock2).filterFiles(isA(File[].class));
		compositeFileFilter.close();
	}

	@Test
	public void forwardedToAddedFilters() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new CompositeFileListFilter<>();
		compositeFileFilter.addFilter(fileFilterMock1);
		compositeFileFilter.addFilter(fileFilterMock2);
		List<File> returnedFiles = Collections.singletonList(fileMock);
		when(fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		when(fileFilterMock2.filterFiles(isA(File[].class))).thenReturn(returnedFiles);
		assertThat(compositeFileFilter.filterFiles(new File[]{ fileMock })).isEqualTo(returnedFiles);
		verify(fileFilterMock1).filterFiles(isA(File[].class));
		verify(fileFilterMock2).filterFiles(isA(File[].class));
		compositeFileFilter.close();
	}

	@Test
	public void negative() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new CompositeFileListFilter<>();
		compositeFileFilter.addFilter(fileFilterMock1);
		compositeFileFilter.addFilter(fileFilterMock2);

		when(fileFilterMock2.filterFiles(isA(File[].class))).thenReturn(new ArrayList<>());
		when(fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(new ArrayList<>());
		assertThat(compositeFileFilter.filterFiles(new File[]{ fileMock }).isEmpty()).isTrue();
		compositeFileFilter.close();
	}

	@Test
	public void excludeFromLaterFilters() throws Exception {
		CompositeFileListFilter<File> compositeFileFilter = new ChainFileListFilter<>();
		compositeFileFilter.addFilter(this.fileFilterMock1);
		compositeFileFilter.addFilter(this.fileFilterMock2);
		List<File> noFiles = new ArrayList<>();
		when(this.fileFilterMock1.filterFiles(isA(File[].class))).thenReturn(noFiles);
		assertThat(compositeFileFilter.filterFiles(new File[]{ this.fileMock })).isEqualTo(noFiles);

		verify(fileFilterMock1).filterFiles(isA(File[].class));
		verify(fileFilterMock2, never()).filterFiles(isA(File[].class));

		compositeFileFilter.close();
	}

	@Test
	public void singleFileCapableUO() throws IOException {
		CompositeFileListFilter<String> compo =
				new CompositeFileListFilter<>(Collections.singletonList(new FileListFilter<String>() {

					@Override
					public List<String> filterFiles(String[] files) {
						return Collections.emptyList();
					}

					@Override
					public boolean supportsSingleFileFiltering() {
						return true;
					}

				}));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> compo.accept("foo"));
		compo.close();
	}

	@Test
	public void singleFileCapable() throws IOException {
		CompositeFileListFilter<String> compo =
				new CompositeFileListFilter<>(Collections.singletonList(new FileListFilter<String>() {

					@Override
					public List<String> filterFiles(String[] files) {
						return Collections.emptyList();
					}

					@Override
					public boolean supportsSingleFileFiltering() {
						return true;
					}


					@Override
					public boolean isForRecursion() {
						return true;
					}

					@Override
					public boolean accept(String file) {
						return true;
					}

				}));
		assertThat(compo.accept("foo")).isTrue();
		compo.addFilter(s -> null);
		assertThat(compo.supportsSingleFileFiltering()).isFalse();
		assertThat(compo.isForRecursion()).isTrue();
		compo.close();
	}

	@Test
	public void notSingleFileCapable() throws IOException {
		CompositeFileListFilter<String> compo =
				new CompositeFileListFilter<>(Collections.singletonList(s -> null));
		assertThat(compo.supportsSingleFileFiltering()).isFalse();
		assertThat(compo.isForRecursion()).isFalse();
		compo.close();
	}

}
