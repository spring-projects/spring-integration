package org.springframework.integration.endpoint.metadata;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.integration.context.IntegrationContextUtils;

import org.springframework.scheduling.TaskScheduler;

import org.springframework.util.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Implementation of {@link org.springframework.integration.endpoint.metadata.MetadataPersister} that knows how to write metadata
 * to a {@link java.util.Properties} instance.
 * <p/>
 * TODO could this perhaps participate or at least be aware of our transaction synchronization mechanism? IE: no guarantees, but we at least try to write on commit()s?
 *
 * @author Josh Long
 */
public class PropertiesBasedMetadataPersister implements MetadataPersister<String>, InitializingBean, BeanFactoryAware {
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
     * An executor (only useful for the background writes if async writes are supported)
     */
    private TaskScheduler taskScheduler;

    /**
     * Users can either provide a unique name and we can automatically setup #locationOfPropertiesOnDisk
     */
    private String uniqueName;

    /**
     * Or, a user can stipulate a {@link org.springframework.core.io.Resource} directly
     */
    private Resource locationOfPropertiesOnDisk;
    private BeanFactory beanFactory;
    private Set<Resource> bootstrapResources = new ConcurrentSkipListSet<Resource>();

    public PropertiesBasedMetadataPersister(Resource ultimateResourceToWhichToWriteFile) {
        this.locationOfPropertiesOnDisk = ultimateResourceToWhichToWriteFile;
    }

    public PropertiesBasedMetadataPersister(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public PropertiesBasedMetadataPersister() {
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
    public void setProperties(Properties properties) {
        this.propertiesFactoryBean.setProperties(properties);
    }

    public void write(String key, String value) {
        synchronized (monitor) {
            long now = System.nanoTime();
            this.properties.setProperty(key, value);

            if (this.supportAsyncWrites) {
                this.taskScheduler.schedule(new BackgroundWriterJob(now, key, value, this.properties), new Date(now));
            } else {
                doWriteToDisk(now, key, value, this.properties);
            }
        }
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    /**
     * This is required to ensure contiuity across restarts. It must be meaningful to a given application of a given component.
     *
     * @param uniqueName the unqiue name to use in constructing a {@link org.springframework.core.io.Resource} for the {@link java.util.Properties} file
     */
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    private void doWriteToDisk(long timestamp, String newKey, String newValue, Properties pro) {
        try {
            FileWriter fileWriter = new FileWriter(this.locationOfPropertiesOnDisk.getFile());
            pro.store(fileWriter, this.uniqueName);
        } catch (IOException e) {
            throw new RuntimeException("couldn't write " + this.properties + " on submission of  to disk at " + new Date(timestamp).toString());
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

            taskScheduler = (this.taskScheduler == null) ? IntegrationContextUtils.getTaskScheduler(this.beanFactory) : taskScheduler;

            if (this.supportAsyncWrites) {
                Assert.notNull(this.taskScheduler, "'taskScheduler' must be set on this bean or defined in the context");
            }

            if (this.locationOfPropertiesOnDisk.exists()) {
                this.bootstrapResources.add(locationOfPropertiesOnDisk);
            }

            // we take the existing Resources [] and use them to bootstrap a Property file when this component wakes up again 
            propertiesFactoryBean.afterPropertiesSet();
            properties = propertiesFactoryBean.getObject();
        }
    }

    public void setLocations(Resource[] locations) {
        for (int i = 0, locationsLength = locations.length; i < locationsLength; i++) {
            Resource r = locations[i];
            this.bootstrapResources.add(r);
        }
    }

    public void setLocation(Resource location) {
        this.bootstrapResources.add(location);
    }

    public void setBeanFactory(BeanFactory beanFactory)
        throws BeansException {
        this.beanFactory = beanFactory;
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
