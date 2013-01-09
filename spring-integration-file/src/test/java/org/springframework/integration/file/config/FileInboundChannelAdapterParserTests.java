/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterParserTests {

    @Autowired(required = true)
    private ApplicationContext context;

    @Autowired
    private FileReadingMessageSource source;

    private DirectFieldAccessor accessor;

    @Before
    public void init() {
        accessor = new DirectFieldAccessor(source);
    }

    @Test
    public void channelName() throws Exception {
    	context.getBean("inputDirPoller");
        AbstractMessageChannel channel = context.getBean("inputDirPoller", AbstractMessageChannel.class);
        assertEquals("Channel should be available under specified id", "inputDirPoller", channel.getComponentName());
    }

    @Test
    public void inputDirectory() {
        File expected = new File(System.getProperty("java.io.tmpdir"));
        File actual = (File) accessor.getPropertyValue("directory");
        assertEquals("'directory' should be set", expected, actual);
    }

    @Test
    public void filter() throws Exception {
        DefaultDirectoryScanner scanner = (DefaultDirectoryScanner) accessor.getPropertyValue("scanner");
        DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(scanner);
        Object filter = scannerAccessor.getPropertyValue("filter");
        assertTrue("'filter' should be set",
                filter instanceof AcceptOnceFileListFilter);
    }

    @Test
    public void comparator() throws Exception {
        Object priorityQueue = accessor.getPropertyValue("toBeReceived");
        assertEquals(PriorityBlockingQueue.class, priorityQueue.getClass());
        Object expected = context.getBean("testComparator");
        DirectFieldAccessor queueAccessor = new DirectFieldAccessor(priorityQueue);
        Object innerQueue = queueAccessor.getPropertyValue("q");
        Object actual;
        if (innerQueue != null) {
            actual = new DirectFieldAccessor(innerQueue).getPropertyValue("comparator");
        }
        else {
            // probably running under JDK 7
            actual = queueAccessor.getPropertyValue("comparator");
        }
        assertSame("comparator reference not set, ", expected, actual);
    }

    static class TestComparator implements Comparator<File> {

        public int compare(File f1, File f2) {
            return 0;
        }
	}
}
