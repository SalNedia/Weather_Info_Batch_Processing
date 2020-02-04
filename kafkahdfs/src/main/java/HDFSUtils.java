import avroUtils.CsvToAvroConverter;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import parquetUtils.ConvertUtils;

import java.io.*;
import java.net.URI;

public class HDFSUtils {
    public static Configuration conf;
    public static FileSystem fs;


    public static FileSystem init() throws IOException {
        String hdfsuri = "hdfs://18.184.214.165:8020";


        // ====== Init HDFS File System Object
        conf = new Configuration();
        conf.set("fs.defaultFS", hdfsuri);
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        fs = FileSystem.get(URI.create(hdfsuri), conf);
        return fs;
    }


    public static void write2(FileSystem fs, String topic, String value, File file) throws Exception {

        String fileName = "";
        switch (Main.type_file) {
            case 0:
                fileName = topic + ".csv";
                break;
            case 1:
                fileName = topic + ".avro";
                break;
            case 2:
                fileName = topic + ".parquet";
                break;
            default:
                break;
        }

        //Write file
        String path = "/user/hdfs/";
        Path newFolderPath = new Path(path);
        if (!fs.exists(newFolderPath)) {
            // Create new Directory
            fs.mkdirs(newFolderPath);
            System.out.println("Path " + path + " created.");
        }
        //Create a path
        Path hdfswritepath = new Path(newFolderPath + "/" + fileName);

        //Init output stream
        FSDataOutputStream outputStream = fs.create(hdfswritepath);


        switch (Main.type_file) {
            case 0:
                break;

            case 1:
                CsvToAvroConverter.converter(topic);
                file = new File("avro/" + fileName);
                break;
            case 2:
                ConvertUtils.createSchema(topic);
                File file_csv = new File("data/" + topic + ".csv");
                file = new File("parquet/" + topic + ".parquet");
                ConvertUtils.convertCsvToParquet(file_csv, file);


        }
        byte[] bFile = new byte[(int) file.length()];
        FileInputStream stream = new FileInputStream(file);
        stream.read(bFile);
        stream.close();
        outputStream.write(bFile);
        outputStream.close();
        if(Main.type_file==2)
            FileUtils.forceDelete(new File("/home/hadoop/parquet/"+topic+".parquet"));


    }

}
