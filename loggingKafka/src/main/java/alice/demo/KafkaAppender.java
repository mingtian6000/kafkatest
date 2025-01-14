package alice.demo;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaAppender extends AppenderBase<ILoggingEvent> {
    private KafkaProducer<String, String> producer;
    private String topic;
    Layout<ILoggingEvent> layout;

    public Layout<ILoggingEvent> getLayout() {
        return layout;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    private String bootstrapServers = "localhost:9092";

    @Override
    public void start() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(props);
        super.start();
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer.close();
        }
        super.stop();
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    protected void append(ILoggingEvent event) {
        //String logMessage = event.getFormattedMessage();
        String logMessage = event.getMessage(); // cannot get real log message..
        System.out.println("Sending message to Kafka topic: " + topic);
        System.out.println(logMessage);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, logMessage);
        producer.send(record);
        //producer.flush();
        System.out.println("Message sent to Kafka topic: " + topic);
    }
}