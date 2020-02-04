import org.apache.hadoop.fs.FileSystem;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.io.*;
import java.util.*;

public class ConsumerKafka {

    public static void consume(String topic) throws Exception {


        Properties props = new Properties();

        props.put("bootstrap.servers", "52.28.225.114:9092");
        //props.put("group.id", group);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("auto.offset.reset", "earliest");
        props.put("key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", "test-consumer-group");

        boolean running = true;
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);

        consumer.subscribe(Arrays.asList(topic));
        FileSystem fs = HDFSUtils.init();
        System.out.println("Subscribed to topic " + topic);
        File f = new File(System.getProperty("user.dir") + "/" + topic + ".csv");
        BufferedWriter writer = null;
        try {
            f.createNewFile();
            writer = new BufferedWriter(new FileWriter(f));

        } catch (IOException e) {
            e.printStackTrace();

        }
        consumer.seekToBeginning(consumer.assignment());

        while (running) {

            ConsumerRecords<String, String> records = consumer.poll(100);


            for (ConsumerRecord<String, String> record : records) {


                if (record.value().equals("EOF")) {
                    try {
                        writer.close();
                        HDFSUtils.write2(fs, topic, record.value(), f);
                        running = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                } else {
                    try {
                        writer.write(record.value() + "\n");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


        }
        consumer.close();
    }


}
