package org.springframework.integration.endpoint.metadata;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.io.*;


/**
 * Tests the functionality of {@link PropertiesBasedMetadataPersister}
 *
 * @author Josh Long
 */
public class PropertiesBasedMetadataPersisterTests {
    private FileSystemResource fileSystemResource;
    private PropertiesBasedMetadataPersister propertiesBasedMetadataPersister;

    @Before
    public void setUp() throws Throwable {
        File tmpFile = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + ".properties");
        fileSystemResource = new FileSystemResource(tmpFile);

        if (tmpFile.exists()) {
            tmpFile.delete();
        }
    }

    @After
    public void tearDown() throws Throwable {
        if ((this.fileSystemResource != null) && this.fileSystemResource.getFile().exists()) {
            this.fileSystemResource.getFile().delete();
        }
    }

    @Test
    public void testMetadataPersistenceRecovery() throws Throwable {
        propertiesBasedMetadataPersister = new PropertiesBasedMetadataPersister(fileSystemResource);
        propertiesBasedMetadataPersister.afterPropertiesSet();

        String timeString = System.currentTimeMillis() + "";
        propertiesBasedMetadataPersister.write("time", timeString);

        propertiesBasedMetadataPersister = new PropertiesBasedMetadataPersister(fileSystemResource);
        propertiesBasedMetadataPersister.afterPropertiesSet();
        Assert.assertEquals(propertiesBasedMetadataPersister.read("time"), timeString);
    }

    @Test
    public void testAsyncMetadataPersistence() throws Throwable {
        propertiesBasedMetadataPersister = new PropertiesBasedMetadataPersister(fileSystemResource);
        propertiesBasedMetadataPersister.setSupportAsyncWrites(true);
        propertiesBasedMetadataPersister.setExecutor(new SimpleAsyncTaskExecutor());
        propertiesBasedMetadataPersister.afterPropertiesSet();

        for (int i = 1; i <= 30; i++) {
            propertiesBasedMetadataPersister.write("sinceId", i + "");
            System.out.println("value written " + i + ", value retreived " + propertiesBasedMetadataPersister.read("sinceId"));
        }

        Thread.sleep(1000);
        Assert.assertTrue(contentsOfFile(fileSystemResource.getFile()).contains("sinceId=30"));
    }

    private String contentsOfFile(File f) {
        String txt = null;
        int width = 300;
        Reader reader = null;

        try {
            StringBuffer stringBuffer = new StringBuffer(width);
            reader = new FileReader(f);

            char[] values = new char[width];

            while (reader.read(values) != -1) {
                stringBuffer.append(values);
            }

            txt = stringBuffer.toString().trim();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // eat it
            }
        }

        return txt;
    }

    @Test
    public void testSyncMetadataPersistence() throws Throwable {
        propertiesBasedMetadataPersister = new PropertiesBasedMetadataPersister(fileSystemResource);
        propertiesBasedMetadataPersister.afterPropertiesSet();

        for (int i = 1; i <= 30; i++) {
            propertiesBasedMetadataPersister.write("sinceId", i + "");
            System.out.println("value written " + i + ", value retreived " + propertiesBasedMetadataPersister.read("sinceId"));
        }

        Assert.assertTrue(contentsOfFile(fileSystemResource.getFile()).contains("sinceId=30"));
    }
}
