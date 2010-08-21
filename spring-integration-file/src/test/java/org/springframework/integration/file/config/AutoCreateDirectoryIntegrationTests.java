/*
 * Copyright 2002-2009 the original author or authors.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AutoCreateDirectoryIntegrationTests {

    private static final String BASE_PATH =
            System.getProperty("java.io.tmpdir") + File.separator + AutoCreateDirectoryIntegrationTests.class.getSimpleName();


    @Autowired
    private ApplicationContext context;


    @BeforeClass
    public static void setupNonAutoCreatedDirectories() {
        new File(BASE_PATH).delete();
        new File(BASE_PATH + File.separator + "customInbound").mkdirs();
        new File(BASE_PATH + File.separator + "customOutbound").mkdirs();
        new File(BASE_PATH + File.separator + "customOutboundGateway").mkdirs();
    }

    @AfterClass
    public static void deleteBaseDirectory() {
        new File(BASE_PATH).delete();
    }


    @Test
    public void defaultInbound() throws Exception {
        Object adapter = context.getBean("defaultInbound");
        DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
        FileReadingMessageSource source = (FileReadingMessageSource)
                adapterAccessor.getPropertyValue("source");
        assertEquals(Boolean.TRUE,
                new DirectFieldAccessor(source).getPropertyValue("autoCreateDirectory"));
        assertTrue(new File(BASE_PATH + File.separator + "defaultInbound").exists());
    }

    @Test
    public void customInbound() throws Exception {
        Object adapter = context.getBean("customInbound");
        DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
        FileReadingMessageSource source = (FileReadingMessageSource)
                adapterAccessor.getPropertyValue("source");
        assertTrue(new File(BASE_PATH + File.separator + "customInbound").exists());
        assertEquals(Boolean.FALSE,
                new DirectFieldAccessor(source).getPropertyValue("autoCreateDirectory"));
    }

    @Test
    public void defaultOutbound() throws Exception {
        Object adapter = context.getBean("defaultOutbound");
        DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
        FileWritingMessageHandler handler = (FileWritingMessageHandler)
                adapterAccessor.getPropertyValue("handler");
        assertEquals(Boolean.TRUE,
                new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory"));
        assertTrue(new File(BASE_PATH + File.separator + "defaultOutbound").exists());
    }

    @Test
    public void customOutbound() throws Exception {
        Object adapter = context.getBean("customOutbound");
        DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
        FileWritingMessageHandler handler = (FileWritingMessageHandler)
                adapterAccessor.getPropertyValue("handler");
        assertTrue(new File(BASE_PATH + File.separator + "customOutbound").exists());
        assertEquals(Boolean.FALSE,
                new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory"));
    }

    @Test
    public void defaultOutboundGateway() throws Exception {
        Object gateway = context.getBean("defaultOutboundGateway");
        DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
        FileWritingMessageHandler handler = (FileWritingMessageHandler)
                gatewayAccessor.getPropertyValue("handler");
        assertEquals(Boolean.TRUE,
                new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory"));
        assertTrue(new File(BASE_PATH + File.separator + "defaultOutboundGateway").exists());
    }

    @Test
    public void customOutboundGateway() throws Exception {
        Object gateway = context.getBean("customOutboundGateway");
        DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(gateway);
        FileWritingMessageHandler handler = (FileWritingMessageHandler)
                gatewayAccessor.getPropertyValue("handler");
        assertTrue(new File(BASE_PATH + File.separator + "customOutboundGateway").exists());
        assertEquals(Boolean.FALSE,
                new DirectFieldAccessor(handler).getPropertyValue("autoCreateDirectory"));
    }

}
