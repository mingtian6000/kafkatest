package alice.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogProducer2Kafka {
    private static final Logger logger = LoggerFactory.getLogger(LogProducer2Kafka.class);


    public static void main(String[] args) {

        logger.info("This is a log message sent via custom Kafka appender.");
        logger.info("Another log message sent via custom Kafka appender.");
    }


}
