package org.springframework.integration.file.monitors;

import org.springframework.core.task.SimpleAsyncTaskExecutor;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.file.entries.*;

import org.springframework.util.Assert;

import java.io.File;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This component will support event-based (not poller based) notifications of files from a file system.
 * Immediate implementations will center around supporting other adapters's delivery of files once they've been synced from a remote system.
 * Potential future expansions for consumer consumption might be a push / event-based file adapter based on either native code
 * or Java 7's NIO.2 WaterService or other third party implementations.
 * <p/>
 * In the meantime, this provides us with a base class for building event driven file adapters quickly. The two cases I see are:
 * 
 *
 * @author Josh Long
 */
public abstract class AbstractEventDrivenFileMonitor extends IntegrationObjectSupport implements EventDrivenDirectoryMonitor {
    /**
     * when this component starts up, we can perform a scan of the folder this first time and to pre-seed the #additions queue
     */
    private boolean scanDirectoryOnLoad;

    /**
     * How many files we'll support in the backlog at a time
     */
    private volatile int maxQueueSize = 100;

    /**
     * the backlog
     */
    private volatile LinkedBlockingQueue<File> additions;

    /**
     * An {@link java.util.concurrent.Executor} implementation. Default is {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
     */
    private volatile Executor executor;

    /**
     * Should the director be automatically created?
     */
    private volatile boolean autoCreateDirectory;

    /**
     * The directory to monitor (a {@link java.io.File})
     */
    private volatile File directoryToMonitorCached;

    /**
     * A {@link org.springframework.integration.file.entries.EntryListFilter} reference
     */
    private volatile SingleEntryAdaptingEntryListFilter<File> filter;

    /**
     * state guard (extra for post-init state)
     */
    private final Object guard = new Object();

    public void setScanDirectoryOnLoad(boolean scanDirectoryOnLoad) {
        this.scanDirectoryOnLoad = scanDirectoryOnLoad;
    }

    /**
     * installs directory, and then kicks of an event pump
     *
     * @param directory         the directory to start watching from. Unspecified if this implies recursion or not.
     * @param fileAdditionListener the callback
     * @throws Exception
     */
    public void monitor(File directory, FileAdditionListener fileAdditionListener)
        throws Exception {
        this.installDirectoryIfRequired(directory);
        this.prescan();
        this.executor.execute(new FileDeliveryPump(fileAdditionListener));
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public void setFilter(EntryListFilter<File> filter) {
        this.filter = new SingleEntryAdaptingEntryListFilter<File>(filter);
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    protected void publishNewFileReceived(String path) {
        this.publishNewFileReceived(new File(path));
    }

    protected void publishNewFileReceived(File file) {
        this.additions.add(file);
    }

    /**
     * Obviously, we're trying to keep away from scanning, but this may be necessary at startup to catch up the backlog
     */
    protected void prescan() {
        synchronized (this.guard) {
            if (this.scanDirectoryOnLoad) {
                for (File f : this.directoryToMonitorCached.listFiles())
                    this.publishNewFileReceived(f);
            }
        }
    }

    /**
     * Handles ensuring that the directory we're monitroring exists or can be created
     *
     * @param directoryToMonitor the directory to monitor
     * @throws Exception
     */
    protected void installDirectoryIfRequired(File directoryToMonitor)
        throws Exception {
        synchronized (this.guard) {
            this.directoryToMonitorCached = directoryToMonitor;
            Assert.state(null != this.directoryToMonitorCached, "the directory to monitor can't be null");

            boolean directoryIsReady = this.directoryToMonitorCached.exists();

            if (!directoryIsReady) {
                if (!directoryToMonitorCached.exists()) {
                    if (this.autoCreateDirectory) {
                        Assert.state(directoryToMonitorCached.mkdirs() && directoryToMonitorCached.exists(),
                            String.format("Couldn't create the directory %s", directoryToMonitorCached.getAbsolutePath()));
                    }
                }
            }
        }
    }

    /**
     * Custom initialization hook - override at your discretion
     *
     * @throws Exception
     */
    protected void start() throws Exception{
        // noop
    }

    @Override
    protected void onInit() throws Exception {
        additions = new LinkedBlockingQueue<File>(this.maxQueueSize);

        if (this.executor == null) {
            this.executor = new SimpleAsyncTaskExecutor();
        }

        if (this.filter == null) {
            this.filter = new SingleEntryAdaptingEntryListFilter<File>(new AcceptAllEntryListFilter<File>());
        }

        Assert.notNull(this.filter, "the filter can't be null");

        this.start();
    }

    /**
     * a way to remove the responsibility of reacting to the file system from implementations while still being thread safe and handling backlog
     *
     * @author Josh Long
     */
    class FileDeliveryPump implements Runnable {
        private volatile FileAdditionListener fileAdditionListener;

        public FileDeliveryPump(FileAdditionListener fileAdditionListener) {
            this.fileAdditionListener = fileAdditionListener;
            Assert.notNull(this.fileAdditionListener, "the FileAdditionListener can't be null");
        }

        public void run() {
            do {
                try {
                    File taken = additions.take();

                    if (filter.accept(taken)) {
                        fileAdditionListener.fileAdded(taken);
                    }
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            } while (true);
        }
    }
}
/*


class MyEDFRM extends AbstractEventDrivenFileMonitor {
    @Override
    protected void start() throws Exception {
        System.out.println("start()");
    }

    public void addToHeap(File file) {
        this.publishNewFileReceived(file);
    }

    public static void main(String[] args) throws Throwable {
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();

        final MyEDFRM myEDFRM = new MyEDFRM();
        myEDFRM.setExecutor(simpleAsyncTaskExecutor);
        myEDFRM.setAutoCreateDirectory(true);
        myEDFRM.afterPropertiesSet();

        AcceptOnceEntryFileListFilter <File> acceptOnceEntryFileListFilter=new AcceptOnceEntryFileListFilter<File>() ;
        acceptOnceEntryFileListFilter.afterPropertiesSet();
        PatternMatchingEntryListFilter <File> patternMatchingEntryListFilter=
                new PatternMatchingEntryListFilter<File>(new FileEntryNamer(), ".*?jpg");
        patternMatchingEntryListFilter.afterPropertiesSet();

        Collection<? extends EntryListFilter<File>> l=Arrays.asList( acceptOnceEntryFileListFilter, patternMatchingEntryListFilter);
        CompositeEntryListFilter compositeEntryListFilter = new CompositeEntryListFilter<File>(l );


        myEDFRM.setFilter(compositeEntryListFilter);

        final File desktop = new File(System.getProperty("user.home"), "Desktop");
        myEDFRM.monitor(desktop, new FileAdditionListener() {
                public void fileAdded(File f) {
                    System.out.println("Got one! " + f.getAbsolutePath());
                }
            });
        System.out.println("enjoying the world");
        simpleAsyncTaskExecutor.execute(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000 * 10);

                            for (File f : desktop.listFiles())
                                myEDFRM.addToHeap(f);
                        } catch (InterruptedException e) {
                            //
                        }
                    }
                }
            });
    }
}
*/
