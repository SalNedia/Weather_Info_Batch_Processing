import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimeUtils {
    private static ArrayList<Long> ingestiontime = new ArrayList<Long>();
    private static Float mean;

    public static void calculateTime(long startTime, long endTime) {
        long diff = (endTime - startTime) / 1000;
        ingestiontime.add(diff);
    }

    private static float mean(List<Long> list) {
        long sum = 0;
        for (long time : list) {
            sum += time;
        }
        float temp = (float) sum / list.size();
        mean = temp;

        return temp;
    }

    public static void compute(BufferedWriter writer) throws IOException {

        writer.write("Hbase mean time(s): " + mean(ingestiontime) + "\n");
        writer.write("******************\n");
        writer.write("Hbase dev time(s): " + dev(0, ingestiontime) + "\n");
    }

    private static float dev(int i, ArrayList<Long> arrayList) {
        double sum = 0;
        for (long x : arrayList) {
            sum += Math.pow(x - mean, 2);
        }
        return (float) Math.sqrt(sum / arrayList.size());
    }
}
