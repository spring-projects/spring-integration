package org.springframework.integration.file.remote;

import org.junit.jupiter.api.Test;
import org.springframework.integration.file.filters.FileListFilter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Gemela
 * @since 5.2
 *
 */
class RemoteFileStreamingMessageSourceTests {

	@Test
	void filterOutFilesNotAcceptedByFilter() {
		@SuppressWarnings("unchecked")
		RemoteFileTemplate<String> remoteFileTemplate = mock(RemoteFileTemplate.class);
		when(remoteFileTemplate.list("remoteDirectory")).thenReturn(new String[]{"file1", "file2"});

		@SuppressWarnings("unchecked")
		FileListFilter<String> fileListFilter = mock(FileListFilter.class);
		when(fileListFilter.supportsSingleFileFiltering()).thenReturn(true);
		when(fileListFilter.accept("file1")).thenReturn(false);
		when(fileListFilter.accept("file2")).thenReturn(false);

		@SuppressWarnings("unchecked")
		Comparator<String> comparator = mock(Comparator.class);
		TestRemoteFileStreamingMessageSource testRemoteFileStreamingMessageSource = new TestRemoteFileStreamingMessageSource(remoteFileTemplate, comparator);

		testRemoteFileStreamingMessageSource.setFilter(fileListFilter);
		testRemoteFileStreamingMessageSource.setRemoteDirectory("remoteDirectory");
		testRemoteFileStreamingMessageSource.start();

		assertThat(testRemoteFileStreamingMessageSource.doReceive()).isNull();
	}

	static class TestRemoteFileStreamingMessageSource extends AbstractRemoteFileStreamingMessageSource<String> {

		TestRemoteFileStreamingMessageSource(RemoteFileTemplate<String> template, Comparator<String> comparator) {
			super(template, comparator);
		}

		@Override
		protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files) {
			return files
					.stream()
					.map(TestFileInfo::new)
					.collect(Collectors.toList());
		}

		@Override
		protected boolean isDirectory(String file) {
			return false;
		}

		@Override
		public String getComponentType() {
			return null;
		}
	}

	static class TestFileInfo extends AbstractFileInfo<String> {

		TestFileInfo(String fileName) {
			this.fileName = fileName;
		}

		private final String fileName;

		@Override
		public boolean isDirectory() {
			return false;
		}

		@Override
		public boolean isLink() {
			return false;
		}

		@Override
		public long getSize() {
			return 0;
		}

		@Override
		public long getModified() {
			return 0;
		}

		@Override
		public String getFilename() {
			return fileName;
		}

		@Override
		public String getPermissions() {
			return null;
		}

		@Override
		public String getFileInfo() {
			return null;
		}
	}
}