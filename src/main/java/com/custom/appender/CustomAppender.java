package com.custom.appender;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.DirectFileRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.DirectWriteRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * An appender that writes to files and can roll over at intervals.
 */
@Plugin(name = CustomAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class CustomAppender extends AbstractOutputStreamAppender<RollingFileManager> {

    public static final String PLUGIN_NAME = "CustomAppender";
    /**
     * Builds FileAppender instances.
     *
     * @param <B>
     *            The type to build
     * @since 2.7
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<CustomAppender> {

        @PluginBuilderAttribute
        private String fileName;

        @PluginBuilderAttribute
        @Required
        private String filePattern;

        @PluginBuilderAttribute
        private boolean append = true;

        @PluginBuilderAttribute
        private boolean locking;

        @PluginElement("Policy")
        @Required
        private TriggeringPolicy policy;

        @PluginElement("Strategy")
        private RolloverStrategy strategy;

        @PluginBuilderAttribute
        private boolean advertise;

        @PluginBuilderAttribute
        private String advertiseUri;

        @PluginBuilderAttribute
        private boolean createOnDemand;

        @PluginBuilderAttribute
        private String filePermissions;

        @PluginBuilderAttribute
        private String fileOwner;

        @PluginBuilderAttribute
        private String fileGroup;

        @Override
        public CustomAppender build() {
            // Even though some variables may be annotated with @Required, we must still perform validation here for
            // call sites that build builders programmatically.
            final boolean isBufferedIo = isBufferedIo();
            final int bufferSize = getBufferSize();
            if (getName() == null) {
                LOGGER.error("CustomAppender '{}': No name provided.", getName());
                return null;
            }

            if (!isBufferedIo && bufferSize > 0) {
                LOGGER.warn("CustomAppender '{}': The bufferSize is set to {} but bufferedIO is not true", getName(), bufferSize);
            }

            if (filePattern == null) {
                LOGGER.error("CustomAppender '{}': No file name pattern provided.", getName());
                return null;
            }

            if (policy == null) {
                LOGGER.error("CustomAppender '{}': No TriggeringPolicy provided.", getName());
                return null;
            }

            if (strategy == null) {
                if (fileName != null) {
                    strategy = DefaultRolloverStrategy.newBuilder()
                            .withCompressionLevelStr(String.valueOf(Deflater.DEFAULT_COMPRESSION))
                            .withConfig(getConfiguration())
                            .build();
                } else {
                    strategy = DirectWriteRolloverStrategy.newBuilder()
                            .withCompressionLevelStr(String.valueOf(Deflater.DEFAULT_COMPRESSION))
                            .withConfig(getConfiguration())
                            .build();
                }
            } else if (fileName == null && !(strategy instanceof DirectFileRolloverStrategy)) {
                LOGGER.error("RollingFileAppender '{}': When no file name is provided a DirectFilenameRolloverStrategy must be configured");
                return null;
            }

            final Layout<? extends Serializable> layout = getOrCreateLayout();
            final RollingFileManager manager = RollingFileManager.getFileManager(fileName, filePattern, append,
                    isBufferedIo, policy, strategy, advertiseUri, layout, bufferSize, isImmediateFlush(),
                    createOnDemand, filePermissions, fileOwner, fileGroup, getConfiguration());
            if (manager == null) {
                return null;
            }

            manager.initialize();

            return new CustomAppender(getName(), layout, getFilter(), manager, fileName, filePattern,
                    isIgnoreExceptions(), isImmediateFlush(), advertise ? getConfiguration().getAdvertiser() : null,
                    getPropertyArray());
        }

        public String getAdvertiseUri() {
            return advertiseUri;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isAdvertise() {
            return advertise;
        }

        public boolean isAppend() {
            return append;
        }

        public boolean isCreateOnDemand() {
            return createOnDemand;
        }

        public boolean isLocking() {
            return locking;
        }

        public String getFilePermissions() {
            return filePermissions;
        }

        public String getFileOwner() {
            return fileOwner;
        }

        public String getFileGroup() {
            return fileGroup;
        }

        public B withAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        public B withAdvertiseUri(final String advertiseUri) {
            this.advertiseUri = advertiseUri;
            return asBuilder();
        }

        public B withAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        public B withFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        public B withCreateOnDemand(final boolean createOnDemand) {
            this.createOnDemand = createOnDemand;
            return asBuilder();
        }

        public B withLocking(final boolean locking) {
            this.locking = locking;
            return asBuilder();
        }

        public String getFilePattern() {
            return filePattern;
        }

        public TriggeringPolicy getPolicy() {
            return policy;
        }

        public RolloverStrategy getStrategy() {
            return strategy;
        }

        public B withFilePattern(final String filePattern) {
            this.filePattern = filePattern;
            return asBuilder();
        }

        public B withPolicy(final TriggeringPolicy policy) {
            this.policy = policy;
            return asBuilder();
        }

        public B withStrategy(final RolloverStrategy strategy) {
            this.strategy = strategy;
            return asBuilder();
        }

        public B withFilePermissions(final String filePermissions) {
            this.filePermissions = filePermissions;
            return asBuilder();
        }

        public B withFileOwner(final String fileOwner) {
            this.fileOwner = fileOwner;
            return asBuilder();
        }

        public B withFileGroup(final String fileGroup) {
            this.fileGroup = fileGroup;
            return asBuilder();
        }

    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final String fileName;
    private final String filePattern;
    private Object advertisement;
    private final Advertiser advertiser;
    private ArrayList<Long> log_event_time = new ArrayList<Long>();
    private int EventArraySize;
    private String LogEventLatencyOut;
    private final ExecutorService threadExecutor;
    private Timestamp logEventStart;

    private CustomAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                           final RollingFileManager manager, final String fileName, final String filePattern,
                           final boolean ignoreExceptions, final boolean immediateFlush, final Advertiser advertiser,
                           final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, properties, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            advertisement = advertiser.advertise(configuration);
        }
        this.fileName = fileName;
        this.filePattern = filePattern;
        this.advertiser = advertiser;
        if(System.getProperty("EventArraySize") != null)  {
             this.EventArraySize =   Integer.parseInt(System.getProperty("EventArraySize"));
        }else {
             this.EventArraySize = 100;
        }
        this.LogEventLatencyOut =  System.getProperty("carbon.home")+File.separator+"repository"+File.separator+"logs"+File.separator+"logeventlatency.csv";
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Log-event-latency-writer-thread-%d").build();
        this.threadExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);
        this.logEventStart = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        final boolean stopped = super.stop(timeout, timeUnit, false);
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
        setStopped();
        return stopped;
    }

    /**
     * Writes the log entry rolling over the file when required.
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        Long time_t1 = System.currentTimeMillis();
        try{
             getManager().checkRollover(event);
             super.append(event);
        }finally {
            Long time_t2 = System.currentTimeMillis();
            Long t3 = time_t2 - time_t1;
            if (log_event_time.size() == EventArraySize) {
                this.threadExecutor.execute(new LogEventLatency(new ArrayList<>(log_event_time),logEventStart,new Timestamp(System.currentTimeMillis())));
                log_event_time.clear();
            }else if(log_event_time.isEmpty()) {
                logEventStart = new Timestamp(System.currentTimeMillis());
            }
            log_event_time.add(t3);
        }
    }

    class LogEventLatency implements Runnable{

        private ArrayList<Long> copyList;
        private Timestamp eventStart;
        private Timestamp eventEnd;

        public LogEventLatency(ArrayList<Long> arrlist,Timestamp eventStart,Timestamp eventEnd){
            this.copyList = arrlist;
            this.eventStart = eventStart;
            this.eventEnd = eventEnd;
        }
        @Override
        public void run() {
            try {
                writeToCSV(logEventtime(copyList));
            } catch (IOException e) {
                LOGGER.error("Error occurred in the custom appender",e);
            }
        }
        /**
         * Returns the Event time log.
         * @retun The log event.
         */
        protected String[] logEventtime(ArrayList<Long> Arrlist){
            Long max = Collections.max(Arrlist);
            Long min = Collections.min(Arrlist);
            Double avg = calculateAverage(Arrlist);
            Long middle = getMedian(Arrlist);
            String[] logEventLatency = {this.eventStart.toString(),this.eventEnd.toString(),Long.toString(max),Long.toString(min),Double.toString(avg),Long.toString(middle)};

            return logEventLatency;
        }

        /**
         * Returns the avarage log event time
         * @return avg log event
         */
        private double calculateAverage(ArrayList<Long> Arrlist) {
            double sum = 0;
            if(!Arrlist.isEmpty()) {
                for (Long mark : Arrlist) {
                    sum += mark;
                }
                return sum / Arrlist.size();
            }
            return sum;
        }

        /**
         * Return the median of log event time
         * @return median log event
         */
        public Long getMedian(ArrayList<Long> Arrlist){
            Collections.sort(Arrlist);
            Long middle = Arrlist.get(Arrlist.size() / 2);
            if (Arrlist.size()%2 == 0) {
                middle = (Arrlist.get(Arrlist.size()/2) + Arrlist.get(Arrlist.size()/2 - 1))/2;
            } else {
                middle = Arrlist.get(Arrlist.size() / 2);
            }
            return middle;
        }
        /**
         * Write log event latency to the csv file
         */
        public void writeToCSV(String[] eventLatency) throws IOException {
            File file = new File(LogEventLatencyOut);
            if(file.createNewFile()){
                try (BufferedWriter br = new BufferedWriter(new FileWriter(file,true))) {
                    br.write("LogEventStartTimeStamp|LogEventEndTimeStamp|Maxtime|Mintime|Avgtime|Median\n");
                    br.flush();
                    String appender = "";
                    for(String s : eventLatency){
                        br.write(appender + s);
                        appender = "|";
                    }
                    br.write("\n");
                    br.flush();
                }
            }else{
                try (BufferedWriter br = new BufferedWriter(new FileWriter(file,true))) {
                    String appender = "";
                    for(String s : eventLatency){
                        br.write(appender + s);
                        appender = "|";
                    }
                    br.write("\n");
                    br.flush();
                }
            }

        }
    }

    /**
     * Returns the File name for the Appender.
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file pattern used when rolling over.
     * @return The file pattern.
     */
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * Returns the triggering policy.
     * @param <T> TriggeringPolicy type
     * @return The TriggeringPolicy
     */
    public <T extends TriggeringPolicy> T getTriggeringPolicy() {
        return getManager().getTriggeringPolicy();
    }

    /**
     * Creates a RollingFileAppender.
     * @param fileName The name of the file that is actively written to. (required).
     * @param filePattern The pattern of the file name to use on rollover. (required).
     * @param append If true, events are appended to the file. If false, the file
     * is overwritten when opened. Defaults to "true"
     * @param name The name of the Appender (required).
     * @param bufferedIO When true, I/O will be buffered. Defaults to "true".
     * @param bufferSizeStr buffer size for buffered IO (default is 8192).
     * @param immediateFlush When true, events are immediately flushed. Defaults to "true".
     * @param policy The triggering policy. (required).
     * @param strategy The rollover strategy. Defaults to DefaultRolloverStrategy.
     * @param layout The layout to use (defaults to the default PatternLayout).
     * @param filter The Filter or null.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseUri The advertised URI which can be used to retrieve the file contents.
     * @param config The Configuration.
     * @return A RollingFileAppender.
     * @deprecated Use {@link #newBuilder()}.
     */
    @Deprecated
    public static <B extends Builder<B>> CustomAppender createAppender(
            // @formatter:off
            final String fileName,
            final String filePattern,
            final String append,
            final String name,
            final String bufferedIO,
            final String bufferSizeStr,
            final String immediateFlush,
            final TriggeringPolicy policy,
            final RolloverStrategy strategy,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String ignore,
            final String advertise,
            final String advertiseUri,
            final Configuration config) {
        // @formatter:on
        final int bufferSize = Integers.parseInt(bufferSizeStr, DEFAULT_BUFFER_SIZE);
        // @formatter:off
        return CustomAppender.<B>newBuilder()
                .withAdvertise(Boolean.parseBoolean(advertise))
                .withAdvertiseUri(advertiseUri)
                .withAppend(Booleans.parseBoolean(append, true))
                .withBufferedIo(Booleans.parseBoolean(bufferedIO, true))
                .withBufferSize(bufferSize)
                .setConfiguration(config)
                .withFileName(fileName)
                .withFilePattern(filePattern).setFilter(filter).setIgnoreExceptions(Booleans.parseBoolean(ignore, true))
                .withImmediateFlush(Booleans.parseBoolean(immediateFlush, true)).setLayout(layout)
                .withCreateOnDemand(false)
                .withLocking(false).setName(name)
                .withPolicy(policy)
                .withStrategy(strategy)
                .build();
        // @formatter:on
    }

    /**
     * Creates a new Builder.
     *
     * @return a new Builder.
     * @since 2.7
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

}
