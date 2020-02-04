import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;


public class HBaseUtils {

    public static HBaseClient hbc;

    public static void execute(String pathHDFS, int query, int idFile, String hdfsuri, int replication) throws IOException {

        hbc = new HBaseClient();
        createTable(hbc, query);

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsuri);

        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        System.setProperty("HADOOP_USER_NAME", "hdfs");
        System.setProperty("hadoop.home.dir", "/");

        //Get the filesystem - HDFS
        FileSystem fs = FileSystem.get(URI.create(hdfsuri), conf);

        if (query == 4) {
            for (int j = 1; j < query; j++) {
                for (int i = 0; i < replication; i++) {
                    if (j != 2) {
                        pathHDFS = "/user/query" + j + "/part-0000" + i;
                        put(pathHDFS, fs, j, -1);
                    } else {
                        for (int x = 0; x < 3; x++) {
                            pathHDFS = "/user/query" + j + "_" + x + "/part-0000" + i;
                            put(pathHDFS, fs, j, x);
                        }
                    }


                }
            }
        } else {
            for (int i = 0; i < replication; i++) {
                pathHDFS = pathHDFS + i;
                put(pathHDFS, fs, query, -1);
            }
        }


    }

    public static void put(String pathHDFS, FileSystem fs, int j, int idFile) {
        Path hdfsreadpath = new Path(pathHDFS);
        //Init input stream
        FSDataInputStream inputStream = null;
        try {
            inputStream = fs.open(hdfsreadpath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String st;
            while ((st = reader.readLine()) != null) {
                putData(hbc, st, j, idFile);
            }
        } catch (IOException e) {
            return;
        }

    }


    public static void delete(HBaseClient hbc, int query) {
        switch (query) {
            case 1:
                if (hbc.exists("query1")) {

                    hbc.dropTable("query1");
                }
                break;
            case 2:
                if (hbc.exists("query2")) {

                    hbc.dropTable("query2");
                }
                break;
            case 3:
                if (hbc.exists("query3")) {

                    hbc.dropTable("query3");
                }
                break;
            case 4:
                if (hbc.exists("query1")) {

                    hbc.dropTable("query1");
                }
                if (hbc.exists("query2")) {

                    hbc.dropTable("query2");
                }
                if (hbc.exists("query3")) {

                    hbc.dropTable("query3");
                }
                break;
            default:
                break;


        }

    }


    private static String getFamilyById(int index) {

        String result = null;
        switch (index) {
            case 0:
                result = "temp";
                break;
            case 1:
                result = "press";
                break;

            case 2:
                result = "hum";
                break;

            default:
                break;
        }
        return result;

    }

    private static void putData(HBaseClient hbc, String line, int query, int idFile) {

        String[] result;
        String v1;
        String familyName;
        switch (query) {
            case 1:
                result = line.split(",", 2);
                computeQuery1(result);
                break;

            case 2:
                familyName = getFamilyById(idFile);
                result = line.split(",", 2);
                computeQuery2(result, familyName);
                break;
            case 3:
                result = line.split(",", 2);
                computeQuery3(result);
                break;
        }


    }

    private static void computeQuery1(String[] result) {
        hbc.put("query1", result[0].substring(1), "fam1", "city_list", result[1].substring(0, result[1].length() - 1));
    }

    private static void computeQuery2(String[] result, String familyName) {
        String[] value = result[1].split(",");

        hbc.put("query2", result[0].substring(1), familyName, "mean", value[0]);
        hbc.put("query2", result[0].substring(1), familyName, "std dev", value[1]);
        hbc.put("query2", result[0].substring(1), familyName, "min", value[2]);
        hbc.put("query2", result[0].substring(1), familyName, "max", value[3].substring(0, value[3].length() - 1));
    }

    private static void computeQuery3(String[] result) {
        hbc.put("query3", result[0].substring(1), "pos1", "city", result[1].split(",")[0].split("_")[0]);
        hbc.put("query3", result[0].substring(1), "pos1", "pos2016", result[1].split(",")[0].split("_")[1]);
        hbc.put("query3", result[0].substring(1), "pos2", "city", result[1].split(",")[1].split("_")[0]);
        hbc.put("query3", result[0].substring(1), "pos2", "pos2016", result[1].split(",")[1].split("_")[1]);
        hbc.put("query3", result[0].substring(1), "pos3", "city", result[1].split(",")[2].split("_")[0]);
        hbc.put("query3", result[0].substring(1), "pos3", "pos2016", result[1].split(",")[2].split("_")[1]);
    }

    private static void createTable(HBaseClient hbc, int query) {
        switch (query) {
            case 1:
                if (!hbc.exists("query1")) {

                    hbc.createTable("query1", "fam1");
                }
                break;
            case 2:
                if (!hbc.exists("query2")) {

                    hbc.createTable("query2", "temp", "press", "hum");
                }
                break;
            case 3:
                if (!hbc.exists("query3")) {

                    hbc.createTable("query3", "pos1", "pos2", "pos3");
                }
                break;
            case 4:
                if (!hbc.exists("query1")) {

                    hbc.createTable("query1", "fam1");
                }
                if (!hbc.exists("query2")) {

                    hbc.createTable("query2", "temp", "press", "hum");
                }
                if (!hbc.exists("query3")) {

                    hbc.createTable("query3", "pos1", "pos2", "pos3");
                }

            default:
                break;
        }

    }


}







