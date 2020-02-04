package utils;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple3;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class FileUtils {

    private static float MIN;
    private static float MAX;

    public static boolean check(Row x) {

        return checkDate(x);

    }

    private static boolean checkDate(Row x) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            LocalDateTime.parse(x.get(0).toString(), formatter);
        } catch (DateTimeParseException exception) {
            return false;
        }
        return true;

    }

    public static Iterator<FileInfo> parse(Row line, String[] cities, List<ZoneId> zoneIds, HashMap<String, String> pairs, int index) throws ParseException {

        switch (index) {
            case 0:
                MIN = 191.15f;
                MAX = 346.15f;
                break;
            case 1:
                MIN = 955;
                MAX = 1065;
                break;
            case 2:
                MIN = 0;
                MAX = 100;
                break;
            default:
                break;
        }

        List<FileInfo> results = new ArrayList<>();

        ArrayList<Float> values = new ArrayList<>();
        //Extract descriptions

        for (int i = 1; i < (line.length()); i++) {
            if (!line.isNullAt(i)) {
                try {
                    if (Float.parseFloat(line.get(i).toString()) < MIN || Float.parseFloat(line.get(i).toString()) > MAX)
                        values.add(null);
                    else
                        values.add(Float.parseFloat(line.get(i).toString()));
                } catch (NumberFormatException nfe) {
                    values.add(null);
                }
            } else
                values.add(null);
        }
        ArrayList<String> countries = new ArrayList<>();

        for (int i = 0; i < cities.length; i++) {

            countries.add(pairs.get(cities[i]));
        }

        //Create object
        for (int i = 0; i < cities.length; i++) {
            if (values.get(i) != null) {
                String newdate = UTCUtils.convert(zoneIds.get(i), line.get(0).toString());
                String[] datetime = newdate.split("-");
                String key, country;
                key = datetime[0] + "-" + datetime[1];
                country = (countries.get(i)).substring(1, countries.get(i).length() - 1);
                results.add(new FileInfo(country,key,values.get(i)));
            }
        }
        return results.iterator();


    }

    public static JavaRDD<FileInfo> mapper(JavaSparkContext sc, SparkSession spark, String path, List<ZoneId> zoneIdList, HashMap<String, String> hmapCities, String format, String[] citiesName, int index) {
        Dataset<Row> fileCity = spark.read().format(format).load(path);
        JavaRDD<Row> fileRDD = fileCity.toJavaRDD();
        JavaRDD<Row> filterRDD = fileRDD.filter(x -> FileUtils.check(x)).cache();
        JavaRDD<FileInfo> mapRDD = filterRDD.flatMap(x -> FileUtils.parse(x, citiesName, zoneIdList, hmapCities, index));
        return mapRDD;
    }
}
