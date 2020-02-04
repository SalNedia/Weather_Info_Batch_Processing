package utils;

import scala.Tuple2;

import java.io.Serializable;
import java.util.*;

public class Stats implements Serializable {

    private static final long serialVersionUID = 1L;

    private double sum;
    private int num;
    private double min;
    private double max;
    private double sumSquare;
    private ArrayList<String> rank;

    public Stats(double sum, int num, double sumSquare, double min, double max) {
        this.sum = sum;
        this.num = num;
        this.min = min;
        this.max = max;
        this.sumSquare = sumSquare;
    }

    public Stats(double sum, int num) {
        this.sum = sum;
        this.num = num;
    }

    public Stats(ArrayList<String> rank) {
        this.rank = rank;
    }

    public double getVar() {

        return Math.sqrt((this.getSumSquare() - this.getNum() * Math.pow(this.getAvg(), 2)) / (this.getNum() - 1));

    }

    public double computeMin(double x) {

        this.setMin(Math.min(this.min, x));
        return this.min;
    }

    public double computeMax(double x) {

        this.setMax(Math.max(this.max, x));
        return this.max;
    }

    public String getValues() {

        return String.valueOf(this.getAvg()) + "," + String.valueOf(this.getVar()) + "," + String.valueOf(this.getMin()) + "," + String.valueOf(this.getMax());
    }

    public String computeRank(String key, List<Tuple2<String, Stats>> rank2016) {


        String[] rankArray = this.rank.toArray(new String[0]);
        HashMap<Double, String> hashMap2017 = new HashMap<>();
        HashMap<Double, String> hashMap2016 = new HashMap<>();


        for (int x = 0; x < rankArray.length; x++) {
            hashMap2017.put(Double.parseDouble(rankArray[x].split("_")[0]), rankArray[x].split("_")[1]);
        }
        Tuple2<String, Stats> rankCity = null;
        for (int x = 0; x < rank2016.size(); x++) {
            Tuple2<String, Stats> rankToCheck = rank2016.get(x);
            if (rankToCheck._1().split("_")[0].equals(key.split("_")[0])) {
                rankCity = rankToCheck;
            }

        }
        String[] rankCity2016 = rankCity._2().getRank().toArray(new String[0]);
        for (int x = 0; x < rankArray.length; x++) {
            hashMap2016.put(Double.parseDouble(rankCity2016[x].split("_")[0]), rankCity2016[x].split("_")[1]);
        }


        TreeMap map2016 = new TreeMap<>(Collections.reverseOrder());
        map2016.putAll(hashMap2016);
        List<String> cities2016 = new ArrayList<>(map2016.values());
        TreeMap map2017 = new TreeMap<>(Collections.reverseOrder());
        map2017.putAll(hashMap2017);
        List<String> cities2017 = new ArrayList<>(map2017.values());
        String result = "";

        for (int i = 0; i < cities2017.size(); i++) {

            result += cities2017.get(i) + "_" + String.valueOf(cities2016.indexOf(cities2017.get(i)) + 1) + ",";
            if (i == 2)
                return result;
        }
        return result;
    }

    public ArrayList<String> addElement(String diff) {
        if (this.rank == null) {
            this.rank = new ArrayList<>();
        }
        this.rank.add(diff);
        return this.rank;
    }

    public double getSumSquare() {
        return sumSquare;
    }

    public void setSumSquare(double sumSquare) {
        this.sumSquare = sumSquare;
    }

    public void setMin(double min) {
        this.min = min;
    }


    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }


    public double getAvg() {
        return (this.sum / this.num);
    }

    public double getSum() {
        return this.sum;
    }

    public int getNum() {
        return this.num;
    }

    public ArrayList<String> getRank() {
        return rank;
    }

    public void setRank(ArrayList<String> rank) {
        this.rank = rank;
    }
}





