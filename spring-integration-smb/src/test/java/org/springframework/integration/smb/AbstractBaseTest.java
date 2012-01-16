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
package org.springframework.integration.smb;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Assorted test utils for the library.
 * @author Markus Spann
 */
public abstract class AbstractBaseTest {

	/** Instance logger. */
	private final Log     logger         = LogFactory.getLog(this.getClass());

	protected final Log getLogger() {
		return logger;
	}

	// CHECKSTYLE:OFF
	@Rule // requires JUnit 4.7 or later
	public final TestName testMethodName = new TestName();
	// CHECKSTYLE:ON

	private String getTestMethodName() {
	    return getClass().getSimpleName() + '.' + testMethodName.getMethodName() + "()";
    }

	@Before
	public final void logTestBegin() {
		getLogger().info("BGN - Test " + getTestMethodName());
	}

	@After
	public final void logTestEnd() {
		getLogger().info("END - Test " + getTestMethodName());
	}

	/**
	 * Constructs the Spring application context XML file name from simple class name and suffix '-context.xml'.
	 * @param _suffix optional suffix
	 * @return application context XML file name
	 */
	protected final String getApplicationContextXmlFile(String _suffix) {
		String fn = getClass().getSimpleName();
		if (StringUtils.hasText(_suffix)) {
			fn += _suffix;
		}
		fn += "-context.xml";
		getLogger().debug("Returning application context xml file [" + fn + "] for class [" + getClass().getName() + "].");
		return fn;
	}

	/**
	 * Constructs the Spring application context XML file name from simple class name.
	 * @return application context XML file name
	 */
	protected final String getApplicationContextXmlFile() {
		return getApplicationContextXmlFile(null);
	}

	protected final ClassPathXmlApplicationContext getApplicationContext() {
		return new ClassPathXmlApplicationContext(getApplicationContextXmlFile(), getClass());
	}

	/**
	 * Writes the specified input stream to file.
	 * @param _inputStream input stream
	 * @param _path output file path
	 * @throws IOException in case of I/O errors 
	 */
	public static void writeToFile(InputStream _inputStream, String _path) throws IOException {
        FileOutputStream fos = new FileOutputStream(_path);
		try {
	        FileCopyUtils.copy(_inputStream, fos);
        } finally {
    		fos.close();
        }
    }

	/**
	 * Writes the specified byte array to the output stream.
	 * @param _bytes byte array
	 * @param _outputStream output stream
	 * @throws IOException in case of I/O errors 
	 */
	public static void writeToFile(byte[] _bytes, OutputStream _outputStream) throws IOException {
		FileCopyUtils.copy(_bytes, _outputStream);
    }

	/**
	 * Writes the specified byte array to the an output file.
	 * @param _bytes byte array
	 * @param _fileName output file
	 * @throws IOException in case of I/O errors 
	 */
    public static void writeToFile(byte[] _bytes, String _fileName) throws IOException {
    	FileOutputStream fos = new FileOutputStream(_fileName);
    	writeToFile(_bytes, fos);
		fos.close();
    }

	/**
	 * Creates a new file of the given name.
	 * If the file exists, it will be deleted.
	 * The file will be deleted on exit of the JVM.
	 * @param _fileName file name
	 * @return file object
	 */
	protected File createNewFile(String _fileName) {
		File file = new File(_fileName);
		file.deleteOnExit();
		if (file.exists()) {
			file.delete();
		}

		getLogger().debug("File object [" + _fileName + "] created: " + file.getAbsolutePath());

		assertFileNotExists(file);
		return file;
	}

	/**
	 * Deletes one or more files or directories.
	 * @param _files file or directories
	 */
	protected void delete(String... _files) {
		for (String fileName : _files) {
			if (fileName == null) {
	            continue;
            }
			File file = new File(fileName);
			if (file.exists()) {
				getLogger().debug("Deleting file [" + fileName + "].");
				if (!file.delete()) {
					file.deleteOnExit();
                }
            }
        }
    }

	/**
	 * Checks if a directory exists, if not creates it and adds it to the DeleteOnExit hook.
	 * @param _dir directory
	 */
	protected void ensureExists(String _dir) {
	    File dir = new File(_dir);
		if (!dir.exists()) {
			dir.mkdirs();
			dir.deleteOnExit();
		}
    }

	/**
	 * Retrieves class name and method name at the specified stacktrace index.
	 * @param _index stacktrace index
	 * @return fully qualified method name
	 */
	private static String getStackTraceString(int _index) {
		StackTraceElement[] arrStackTraceElems = new Throwable().fillInStackTrace().getStackTrace();
		final int lIndex = Math.min(arrStackTraceElems.length - 1, Math.max(0, _index));
		return arrStackTraceElems[lIndex].getClassName() + "." + arrStackTraceElems[lIndex].getMethodName();
	}

	/**
	 * Gets the current method name.
	 * @return method name
	 */
	public static String getMethodName() {
		return getStackTraceString(2);
	}

	/**
	 * Gets the calling method name.
	 * @return method name
	 */
	public static String getCallingMethodName() {
		return getStackTraceString(3);
	}

	/**
	 * Asserts that the specified file exists.
	 * @param _file file object
	 * @return the file object
	 */
	public static final File assertFileExists(File _file) {
		return assertFileExists(_file, true);
    }

	public static final File assertFileNotExists(File _file) {
		return assertFileExists(_file, false);
    }

	/**
	 * Asserts that the specified file exists or does not exists.
	 * @param _file file object
	 * @param _exists true if should exist, false otherwise
	 * @return the file object
	 */
	private static File assertFileExists(File _file, boolean _exists) {
		assertNotNull("File object is null.", _file);
		if (_exists) {
			assertTrue("File [" + _file.getAbsolutePath() + "] does not exist.", _file.exists());
        } else {
    		assertTrue("File [" + _file.getAbsolutePath() + "] exists.", !_file.exists());
        }
		return _file;
    }

	public static final File assertFileExists(String _file) {
		return assertFileExists(new File(_file));
    }

	/**
	 * Invokes one or more test methods on the specified test class.
	 * Catches exceptions during test setup (using reflection) and test invocation.
	 * @param _testClass test class object
	 * @param _methodNames String method names to invoke in order, no parameters expected
	 */
	protected static void runTests(Class<? extends AbstractBaseTest> _testClass, String... _methodNames) {
		AbstractBaseTest test;
        Method[] methods = new Method[_methodNames.length];
        String methodName = null;

        try {
	        test = _testClass.newInstance();
	        for (int i = 0; i < _methodNames.length; i++) {
				methodName = _methodNames[i];
				methods[i] = _testClass.getMethod(methodName, (Class<?>[]) null);
            }
        } catch (Exception _ex) {
			System.err.println("Test setup failed for " + _testClass + "." + methodName + "().");
			_ex.printStackTrace();
			return;
        }

		Method method = null;
		try {

	        for (int i = 0; i < methods.length; i++) {
				method = methods[i];
				method.invoke(test, (Object[]) null);
            }

    	} catch (Exception _ex) {
			System.err.println("Test execution failed for " + _testClass + "." + method.getName() + ".");
    		_ex.printStackTrace();
    	}

	}

}
