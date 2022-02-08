# CustomLog4j2RollingAppender
This custom log4j2 rolling appender can be used identify any latencies in logging I/O operations when writing the wso2carbon.log file.

### How to use.
1. Build the CustomLog4j2RollingAppender source using ```mvn clean install``` command.
2. Copy the JAR file into __<APIM_HOME>/repository/components/dropins__ directory.
3. Set the CustomAppender as the implementation of the CARBON_LOGFILE appender in __<APIM_HOME>/repository/conf/log4j2.properties__ file.

   ```properties
      appender.CARBON_LOGFILE.type = CustomAppender
      appender.CARBON_LOGFILE.name = CARBON_LOGFILE
              .
              .
   ```
5. Start the server with __-DEventArraySize=10__ system parameter. Value of this system parameter define the number of log event to be used when calculating log event latency related attributes.
6. This appender will to generates the __<APIM_HOME>/repository/logs/logeventlatency.csv__ file as below.

   ```properties
    LogEventStartTimeStamp|LogEventEndTimeStamp|Maxtime|Mintime|Avgtime|Median
    2022-02-07 22:42:03.618|2022-02-07 22:42:04.872|5|0|0.5|0
    2022-02-07 22:42:04.872|2022-02-07 22:42:09.501|1|0|0.1|0
    2022-02-07 22:42:09.501|2022-02-07 22:42:16.704|1|0|0.2|0
   ```
