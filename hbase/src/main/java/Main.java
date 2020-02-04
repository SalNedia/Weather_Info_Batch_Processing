import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static long startTime;
    public static final int RUN=5;

    public static void main(String[] args) throws IOException {

        String hdfs_uri = "hdfs://18.184.214.165:8020";

        File metricsFile = new File("metrics.txt");
        metricsFile.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(metricsFile, true));

        for (int j = 0; j < RUN; j++) {
            //starting time
            startTime = System.currentTimeMillis();
            if (Integer.parseInt(args[0]) == 4) {
                HBaseUtils.execute("/user/query" + Integer.parseInt(args[0]) + "/part-0000", Integer.parseInt(args[0]), -1, hdfs_uri, Integer.parseInt(args[1]));
            } else {

                if (Integer.parseInt(args[0]) == 2) {
                    for (int i = 0; i < 3; i++)
                        HBaseUtils.execute("/user/query2_" + i + "/part-0000", Integer.parseInt(args[0]), i, hdfs_uri, Integer.parseInt(args[1]));
                } else
                    HBaseUtils.execute("/user/query" + Integer.parseInt(args[0]) + "/part-0000", Integer.parseInt(args[0]), -1, hdfs_uri, Integer.parseInt(args[1]));
            }
            //stopping time
            TimeUtils.calculateTime(startTime, System.currentTimeMillis());

            //deleting old outputs (needed only for re-execute this code many times for metrics computations)
            if (j != RUN-1)
                HBaseUtils.delete(HBaseUtils.hbc, Integer.parseInt(args[0]));
        }
        //write times in metrics.txt
        TimeUtils.compute(writer);
        writer.close();


    }
}
