import org.apache.hadoop.fs.Path;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Main {

    public static int type_file;  //can be 0 for csv, 1 for avro and 2 for parquet
    public static long startTime;
    public static int RUN=5;

    public static void main(String[] args) throws Exception {

        String[] pathList = {"data/temperature.csv", "data/pressure.csv", "data/humidity.csv", "data/city_attributes.csv", "data/weather_description.csv"};
        String[] topic = {"temperature", "pressure", "humidity", "city_attributes", "weather_description"};

        type_file = Integer.parseInt(args[0]);

        File metricsFile = new File("metrics.txt");
        metricsFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(metricsFile));

        //executes this code RUN times for metrics
        for (int j = 0; j < RUN; j++) {
            startTime = System.currentTimeMillis();

            //produce topic
            ProducerKafka.produce(pathList, topic);
            //consume topic
            for (String aTopic : topic) {
                ConsumerKafka.consume(aTopic);
            }
            TimeUtils.calculateTime(startTime, System.currentTimeMillis());

            for (int i = 0; i < topic.length; i++) {
                String app = "";
                switch (type_file) {
                    case 0:
                        app = topic[i] + ".csv";
                        break;
                    case 1:
                        app = topic[i] + ".avro";
                        break;
                    case 2:
                        app = topic[i] + ".parquet";
                        break;

                }
                if (j != 4) {
                    Path path = new Path("/user/hdfs/" + app);
                    HDFSUtils.fs.delete(path, true);
                }
            }


        }
        TimeUtils.compute(writer);
        writer.close();
    }
}
