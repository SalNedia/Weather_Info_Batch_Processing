package utils;

import org.apache.spark.sql.Row;
import scala.Tuple2;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class FileInfoParser {

    private static float MIN;
    private static float MAX;


    public static Iterator<Tuple2<String, Float>> parse(Row line, String[] cities, HashMap<String, String> pairs, List<ZoneId> zoneIds, int index, boolean query3) throws ParseException {

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

        List<Tuple2<String, Float>> results = new ArrayList<>();

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

                if (query3) {
                    country = (countries.get(i)).substring(1, countries.get(i).length() - 1) + "_" + cities[i];
                    if ((Integer.parseInt(datetime[2].split(" ")[1].split(":")[0]) >= 12 && (Integer.parseInt(datetime[2].split(" ")[1].split(":")[0]) <= 15))) {
                        if ((datetime[1].equals("06") || datetime[1].equals("07") || datetime[1].equals("08") || datetime[1].equals("09")))
                            country = country + "_S";
                        else if (datetime[1].equals("01") || datetime[1].equals("02") || datetime[1].equals("03") || datetime[1].equals("04"))
                            country = country + "_W";
                    } else
                        country = country + "_N";

                    key = datetime[0];
                } else {
                    key = datetime[0] + "-" + datetime[1];
                    country = (countries.get(i)).substring(1, countries.get(i).length() - 1);
                }
                Tuple2<String, Float> result = new Tuple2<>(country + "_" + key, values.get(i));
                results.add(result);

            }
        }

        return results.iterator();
    }


    public static boolean checkDate(Row x) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            LocalDateTime.parse(x.get(0).toString(), formatter);
        } catch (DateTimeParseException exception) {
            return false;
        }
        return true;

    }

    public static Iterator<Tuple2<String, Integer>> parseDescription(Row line, String[] cities, List<ZoneId> zoneIdList) throws ParseException {

        List<Tuple2<String, Integer>> results = new ArrayList<>();
        ArrayList<String> descriptions = new ArrayList<>();
        //Get all valid descriptions
        for (int i = 1; i < (line.length()); i++) {
            if (!line.isNullAt(i)) {
                if (line.get(i).toString().matches("[a-zA-Z\\s]+"))
                    descriptions.add(line.get(i).toString());
                else
                    descriptions.add(null);
            } else
                descriptions.add(null);
        }
        int count;
        for (int i = 0; i < cities.length; i++) {
            if (descriptions.get(i) != null) {
                //Convert all date to valid UTC
                String newdate = UTCUtils.convert(zoneIdList.get(i), line.get(0).toString());
                String[] datetime = newdate.split("-");
                String date = datetime[0] + "-" + datetime[1] + "-" + datetime[2].split(" ")[0];
                //Set count to one if the description is sky is clear
                if (descriptions.get(i).equals("sky is clear"))
                    count = 1;
                else count = 0;
                Tuple2<String, Integer> result = new Tuple2<>(cities[i] + "_" + date, count);
                results.add(result);
            }

        }
        return results.iterator();
    }

    //Get first map key
    public static String getKey(String value) {
        String[] stringValue = value.split("_");
        String[] datetime = stringValue[1].split("-");
        String key = stringValue[0] + "_" + datetime[0] + "-" + datetime[1];
        return key;
    }

    //Get second map key
    public static String getKey2(String value) {
        String[] stringValue = value.split("_");
        String[] datetime = stringValue[1].split("-");
        String key = stringValue[0] + "_" + datetime[0];
        return key;
    }


    public static String splitAvg(String x, String y) {

        String[] avgX = x.split("_");
        String[] avgY = y.split("_");
        return String.valueOf(Math.abs(Double.parseDouble(avgX[1]) - Double.parseDouble(avgY[1])));
    }

    public static Iterator<Tuple2<String, Float>> parse2(Row line, String[] cities, HashMap<String, String> pairs, List<ZoneId> zoneIds, int index) {

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

        List<Tuple2<String, Float>> results = new ArrayList<>();

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

        for (int i = 0; i < cities.length; i++) {
            if (values.get(i) != null) {
                String newdate = UTCUtils.convert(zoneIds.get(i), line.get(0).toString());
                String[] datetime = newdate.split("-");
                String key, country;
                key = datetime[0] + "-" + datetime[1] + "-" + datetime[2];
                country = (countries.get(i)).substring(1, countries.get(i).length() - 1);

                Tuple2<String, Float> result = new Tuple2<>(country + "_" + cities[i] + "_" + key, values.get(i));
                results.add(result);

            }
        }
        return results.iterator();


    }

    public static Tuple2<String, Float> format(Tuple2<String, Float> x, boolean query3) {
        String[] oldKey = x._1().split("_");
        String country = oldKey[0];
        String date = oldKey[2];
        String[] datetime = date.split("-");
        String key;

        if (query3) {
            country = country + "_" + oldKey[1];
            if ((Integer.parseInt(datetime[2].split(" ")[1].split(":")[0]) >= 12 && (Integer.parseInt(datetime[2].split(" ")[1].split(":")[0]) <= 15))) {
                if ((datetime[1].equals("06") || datetime[1].equals("07") || datetime[1].equals("08") || datetime[1].equals("09")))
                    country = country + "_S";
                else if (datetime[1].equals("01") || datetime[1].equals("02") || datetime[1].equals("03") || datetime[1].equals("04"))
                    country = country + "_W";
            } else
                country = country + "_N";
            key = datetime[0];
        } else {
            key = datetime[0] + "-" + datetime[1];

        }
        Tuple2<String, Float> result = new Tuple2<>(country + "_" + key, x._2());
        return result;
    }
}
