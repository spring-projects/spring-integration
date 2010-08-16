package org.springframework.integration.endpoint.metadata;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;


/**
 * Implementation of {@link org.springframework.integration.endpoint.metadata.MetadataPersister} that knows how to write metadata
 * to a {@link java.util.Properties} instance.
 * <p/>
 * TODO could this perhaps participate or at least be aware of our transaction synchronization mechanism? IE: no guarantees, but we at least try to write on commit()s?
 *
 * @author Josh Long
 */
public class PropertiesBasedMetadataPersister implements MetadataPersister<String>, InitializingBean {
    /**
     * Used to queue the writes asynchronously
     */
    private Executor executor = new SimpleAsyncTaskExecutor();

    /**
     * Used to encapsulate acquisition of a {@link java.util.Properties} instance if it's prefered that we handled it on the client's behalf
     */
    private PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();

    /**
     * guard for initialization and writes
     */
    private final Object monitor = new Object();

    /**
     * This would enable a background thread that would write as possible, but not block #write calls
     */
    private volatile boolean supportAsyncWrites;

    /**
     * An existing {@link java.util.Properties} file that we can read in at startup. This is utlimately forwarded to {@link org.springframework.beans.factory.config.PropertiesFactoryBean} on startup
     */
    private Properties properties;

    /**
     * Users can either provide a unique name and we can automatically setup #locationOfPropertiesOnDisk
     */
    private String uniqueName;

    /**
     * Or, a user can stipulate a {@link org.springframework.core.io.Resource} directly
     */
    private Resource locationOfPropertiesOnDisk;
    private Set<Resource> bootstrapResources = new HashSet<Resource>();
    private volatile File cachedLocationOfPropertiesFile;

    public PropertiesBasedMetadataPersister(Resource ultimateResourceToWhichToWriteFile) {
        setLocationOfPropertiesOnDisk(ultimateResourceToWhichToWriteFile);
    }

    @SuppressWarnings("unused")
    public PropertiesBasedMetadataPersister(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    @SuppressWarnings("unused")
    public PropertiesBasedMetadataPersister() {
    }

    @SuppressWarnings("unused")
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setLocationOfPropertiesOnDisk(Resource locationOfPropertiesOnDisk) {
        this.locationOfPropertiesOnDisk = locationOfPropertiesOnDisk;
    }

    private File buildFileFromUniqueName() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        if ((this.uniqueName == null) || this.uniqueName.trim().equals("")) {
            this.uniqueName = UUID.randomUUID().toString();
        }

        String un = this.uniqueName + ".properties";

        return new File(tmpDir, un);
    }

    /**
     * Optional - if there's already a {@link java.util.Properties} instance in play than we can simply use that one.
     *
     * @param properties existing properties, just in case
     */
    @SuppressWarnings("unused")
    public void setProperties(Properties properties) {
        this.propertiesFactoryBean.setProperties(properties);
    }

    public void write(String key, String value) {
        Assert.notNull( key != null , "key can't be null");
        Assert.notNull( value != null , "value can't be null");
        synchronized (monitor) {
            long now = System.nanoTime();
            this.properties.setProperty(key, value);

            if (this.supportAsyncWrites) {
                this.executor.execute(new BackgroundWriterJob(now, key, value, this.properties));
            } else {
                doWriteToDisk(now, key, value, this.properties);
            }
        }
    }

    /**
     * This is required to ensure contiuity across restarts. It must be meaningful to a given application of a given component.
     *
     * @param uniqueName the unqiue name to use in constructing a {@link org.springframework.core.io.Resource} for the {@link java.util.Properties} file
     */
    @SuppressWarnings("unused")
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    private void doWriteToDisk(long timestamp, String newKey, String newValue, Properties pro) {
        try {
            FileWriter fileWriter = null;

            try {
                fileWriter = new FileWriter(cachedLocationOfPropertiesFile);
                pro.store(fileWriter, (this.uniqueName == null) ? "" : this.uniqueName);
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("couldn't write " + this.properties + " on submission of " + newKey + "=" + newValue + "  to disk at " + new Date(timestamp).toString());
        }
    }

    public String read(String key) {
        return this.properties.getProperty(key);
    }

    public void setSupportAsyncWrites(boolean supportAsyncWrites) {
        this.supportAsyncWrites = supportAsyncWrites;
    }

    public void afterPropertiesSet() throws Exception {
        synchronized (this.monitor) {
            if ((this.locationOfPropertiesOnDisk == null) && (this.uniqueName == null)) {
                throw new RuntimeException("you must either specify a property file Resource or a uniqueName that can be used in generated a path that will be input into creating a Resource");
            }

            if ((this.locationOfPropertiesOnDisk == null)) {
                File pathOfPropertiesFileOnDisk = buildFileFromUniqueName();
                this.locationOfPropertiesOnDisk = new FileSystemResource(pathOfPropertiesFileOnDisk);
            }

            if (this.supportAsyncWrites) {
                Assert.notNull(this.executor, "'executorService' must be set on this bean or defined in the context");
            }

            if (this.locationOfPropertiesOnDisk.exists()) {
                this.bootstrapResources.add(locationOfPropertiesOnDisk);
            }

            this.cachedLocationOfPropertiesFile = this.locationOfPropertiesOnDisk.getFile();

            propertiesFactoryBean.setLocations(this.bootstrapResources.toArray(new Resource[bootstrapResources.size()]));
            // we take the existing Resources [] and use them to bootstrap a Property file when this component wakes up again 
            propertiesFactoryBean.afterPropertiesSet();
            properties = propertiesFactoryBean.getObject();
        }
    }

    @SuppressWarnings("unused")
    public void setLocations(Resource[] locations) {
        for (int i = 0, locationsLength = locations.length; i < locationsLength; i++) {
            Resource r = locations[i];
            this.bootstrapResources.add(r);
        }
    }

    @SuppressWarnings("unused")
    public void setLocation(Resource location) {
        this.bootstrapResources.add(location);
    }

    

    /**
     * This class is used to ensure that the properies are persisted to the right place as soon as capacity / the task Scheduler allows
     */
    private class BackgroundWriterJob implements Runnable {
        private volatile Properties properties;
        private String key;
        private String value;
        private long now;

        public BackgroundWriterJob(long now, String key, String value, Properties properties) {
            this.properties = properties;
            this.now = now;
            this.key = key;
            this.value = value;
        }

        public void run() {
            synchronized (monitor) {
                doWriteToDisk(this.now, this.key, this.value, this.properties);
            }
        }
    }
}
