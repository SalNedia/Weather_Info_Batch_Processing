package parquetUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.Log;
import org.apache.parquet.Preconditions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ConvertUtils {
    private static final Log LOG = Log.getLog(ConvertUtils.class);

    public static final String CSV_DELIMITER= ",";

    private static String readFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        StringBuilder stringBuilder = new StringBuilder();

        try {
            String line = null;
            String ls = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null ) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } finally {
            Utils.closeQuietly(reader);
        }

        return stringBuilder.toString();
    }

    public static String getSchema(File csvFile) throws IOException {
        String fileName = csvFile.getName().substring(
                0, csvFile.getName().length() - ".csv".length()) + ".schema";
        File schemaFile = new File(csvFile.getParentFile(), fileName);
        return readFile(schemaFile.getAbsolutePath());
    }

    public static void convertCsvToParquet(File csvFile, File outputParquetFile) throws IOException {
        convertCsvToParquet(csvFile, outputParquetFile, false);
    }

    public static void convertCsvToParquet(File csvFile, File outputParquetFile, boolean enableDictionary) throws IOException {
        LOG.info("Converting " + csvFile.getName() + " to " + outputParquetFile.getName());
        String rawSchema = getSchema(csvFile);
        if(outputParquetFile.exists()) {
            throw new IOException("Output file " + outputParquetFile.getAbsolutePath() +
                    " already exists");
        }

        Path path = new Path(outputParquetFile.toURI());

        MessageType schema = MessageTypeParser.parseMessageType(rawSchema);
        CsvParquetWriter writer = new CsvParquetWriter(path, schema, enableDictionary);

        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        String line;
        int lineNumber = 0;
        try {
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(Pattern.quote(CSV_DELIMITER),-1);
                String[] app=new String[fields.length];

                int i=0;
                for(String x:fields ){

                    if(x.equals("")){
                        app[i]="_";
                    }
                    else
                        app[i]=x;
                    i++;
                }
                writer.write(Arrays.asList(app));
                ++lineNumber;
            }

            writer.close();
        } finally {
            LOG.info("Number of lines: " + lineNumber);
            Utils.closeQuietly(br);
        }
    }

    public static void convertParquetToCSV(File parquetFile, File csvOutputFile) throws IOException {
        Preconditions.checkArgument(parquetFile.getName().endsWith(".parquet"),
                "parquet file should have .parquet extension");
        Preconditions.checkArgument(csvOutputFile.getName().endsWith(".csv"),
                "csv file should have .csv extension");
        Preconditions.checkArgument(!csvOutputFile.exists(),
                "Output file " + csvOutputFile.getAbsolutePath() + " already exists");

        LOG.info("Converting " + parquetFile.getName() + " to " + csvOutputFile.getName());


        Path parquetFilePath = new Path(parquetFile.toURI());

        Configuration configuration = new Configuration(true);

        GroupReadSupport readSupport = new GroupReadSupport();
        ParquetMetadata readFooter = ParquetFileReader.readFooter(configuration, parquetFilePath);
        MessageType schema = readFooter.getFileMetaData().getSchema();

        readSupport.init(configuration, null, schema);
        BufferedWriter w = new BufferedWriter(new FileWriter(csvOutputFile));
        ParquetReader<Group> reader = new ParquetReader<Group>(parquetFilePath, readSupport);
        try{
            Group g = null;
            while( (g = reader.read())!= null) {
                writeGroup(w, g, schema);
            }
            reader.close();
        }
        finally {
            Utils.closeQuietly(w);
        }
    }

    private static void writeGroup(BufferedWriter w, Group g, MessageType schema)
            throws IOException{
        for (int j = 0; j < schema.getFieldCount(); j++) {
            if (j > 0) {
                w.write(CSV_DELIMITER);
            }
            String valueToString = g.getValueToString(j, 0);
            w.write(valueToString);
        }
        w.write('\n');
    }

    public static void createSchema(String topic) throws IOException {


        String header="message h {"+"\n";
        File file_csv=new File("data/"+topic+".csv");
        File file_schema=new File("data/"+topic+".schema");
        file_schema.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file_schema));
        BufferedReader reader = new BufferedReader(new FileReader(file_csv));
        String line = reader.readLine();
        if(line!=null) {
            writer.write(header);
            String[] columns=line.split(",");
            for(String c: columns){
                if(c.contains(" ")){
                    String[] split=c.split(" ");
                    c="";
                    for(int i=0;i<split.length;i++){
                        c+=split[i];
                        if(i!=split.length-1){
                            c+="_";
                        }
                    }

                }
                header="required binary "+c+" (UTF8);"+"\n";
                writer.write(header);
            }
        }
        reader.close();
        writer.write("}");
        writer.close();
        // System.out.println(header);



    }



    @Deprecated
    public static void convertParquetToCSVEx(File parquetFile, File csvOutputFile) throws IOException {
        Preconditions.checkArgument(parquetFile.getName().endsWith(".parquet"),
                "parquet file should have .parquet extension");
        Preconditions.checkArgument(csvOutputFile.getName().endsWith(".csv"),
                "csv file should have .csv extension");
        Preconditions.checkArgument(!csvOutputFile.exists(),
                "Output file " + csvOutputFile.getAbsolutePath() + " already exists");

        LOG.info("Converting " + parquetFile.getName() + " to " + csvOutputFile.getName());

        Path parquetFilePath = new Path(parquetFile.toURI());

        Configuration configuration = new Configuration(true);

        // TODO Following can be changed by using ParquetReader instead of ParquetFileReader
        ParquetMetadata readFooter = ParquetFileReader.readFooter(configuration, parquetFilePath);
        MessageType schema = readFooter.getFileMetaData().getSchema();
        ParquetFileReader parquetFileReader = new ParquetFileReader(
                configuration, parquetFilePath, readFooter.getBlocks(), schema.getColumns());
        BufferedWriter w = new BufferedWriter(new FileWriter(csvOutputFile));
        PageReadStore pages = null;
        try {
            while (null != (pages = parquetFileReader.readNextRowGroup())) {
                final long rows = pages.getRowCount();
                LOG.info("Number of rows: " + rows);

                final MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
                final RecordReader<Group> recordReader = columnIO.getRecordReader(pages, new GroupRecordConverter(schema));
                for (int i = 0; i < rows; i++) {
                    final Group g = recordReader.read();
                    writeGroup(w, g, schema);
                }
            }
        } finally {
            Utils.closeQuietly(parquetFileReader);
            Utils.closeQuietly(w);
        }
    }
}
