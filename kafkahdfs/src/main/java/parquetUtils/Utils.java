package parquetUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.parquet.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
    private static final Log LOG = Log.getLog(Utils.class);

    public static void closeQuietly(Closeable res) {
        try {
            if(res != null) {
                res.close();
            }
        } catch (IOException ioe) {
            LOG.warn("Exception closing reader " + res + ": " + ioe.getMessage());
        }
    }

    public static void writePerfResult(String module, long millis) throws IOException{
        PrintWriter writer = null;
        try {
            File outputFile = new File("target/test/perftime." + module + ".txt");
            outputFile.delete();
            writer = new  PrintWriter(outputFile);
            writer.write(String.valueOf(millis));
        } finally {
            closeQuietly(writer);
        }
    }

    public static long readPerfResult(String version, String module) throws IOException {
        BufferedReader reader = null;
        try {
            File inFile = new File("../" + version + "/target/test/perftime." + module + ".txt");
            reader = new  BufferedReader(new FileReader(inFile));
            return Long.parseLong(reader.readLine());
        } finally {
            closeQuietly(reader);
        }
    }

    public static File createTestFile(long largerThanMB) throws IOException {
        File outputFile = new File("target/test/csv/perftest.csv");
        if(outputFile.exists()) {
            return outputFile;
        }
        File toCopy = new File("../parquet-testdata/tpch/customer.csv");
        FileUtils.copyFile(new File("../parquet-testdata/tpch/customer.schema"), new File("target/test/csv/perftest.schema"));

        OutputStream output = null;
        InputStream input = null;

        try {
            output = new BufferedOutputStream(new FileOutputStream(outputFile, true));
            input = new BufferedInputStream(new FileInputStream(toCopy));
            input.mark(Integer.MAX_VALUE);
            while(outputFile.length() <= largerThanMB * 1024 * 1024) {
                //appendFile(output, toCopy);
                IOUtils.copy(input, output);
                input.reset();
            }
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }

        return outputFile;
    }

    public static File[] getAllOriginalCSVFiles() {
        File baseDir = new File("../parquet-testdata/tpch");
        final File[] csvFiles = baseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv");
            }
        });
        return csvFiles;
    }

    public static File getParquetOutputFile(String name, String module, boolean deleteIfExists) {
        File outputFile = new File("target/parquet/", getParquetFileName(name, module));
        outputFile.getParentFile().mkdirs();
        if(deleteIfExists) {
            outputFile.delete();
        }
        return outputFile;
    }

    private static String getParquetFileName (String name, String module) {
        return name + (module != null ? "." + module : "") + ".parquet";
    }

    public static File getParquetFile(String name, String version, String module, boolean failIfNotExist)
            throws IOException {
        File parquetFile = new File("../"+version+"/target/parquet/", getParquetFileName(name, module));
        parquetFile.getParentFile().mkdirs();
        if(!parquetFile.exists()) {
            String msg = "File " + parquetFile.getAbsolutePath() + " does not exist";
            if(failIfNotExist) {
                throw new IOException(msg);
            }
            LOG.warn(msg);
        }
        return parquetFile;
    }

    private static String getCurrentVersion() throws IOException {
        return new File(".").getCanonicalFile().getName().replace("parquet-compat-", "");
    }
/*
    public static String[] getImpalaDirectories() throws IOException {
        File baseDir = new File("../parquet-testdata/impala");
        final String currentVersion = getCurrentVersion();
        final String[] impalaVersions = baseDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.startsWith(".")) {
                    return false;
                }
                if (name.contains("-")) {
                    name = name.split("-")[0];
                }
                return new Version(name).compareMajorMinor(new Version(currentVersion)) == 0;
            }
        });
        return impalaVersions;
    }*/

    public static File getParquetImpalaFile(String name, String impalaVersion) throws IOException {
        String fileName = name + ".impala.parquet";
        File parquetFile = new File("../parquet-testdata/impala/" + impalaVersion, fileName);
        if(!parquetFile.exists()) {
            throw new IOException("File " + fileName + " does not exist");
        }
        return parquetFile;
    }

    public static String getFileNamePrefix(File file) {
        return file.getName().substring(0, file.getName().indexOf("."));
    }

    public static File getCsvTestFile(String name, String module, boolean deleteIfExists) {
        File outputFile = new File("target/test/csv/",
                name + (module != null ? "." + module : "") + ".csv");
        outputFile.getParentFile().mkdirs();
        if(deleteIfExists) {
            outputFile.delete();
        }
        return outputFile;
    }

    public static File getParquetTestFile(String name, String module, boolean deleteIfExists) {
        File outputFile = new File("target/test/parquet/",
                name + (module != null ? "." + module : "") + ".csv");
        outputFile.getParentFile().mkdirs();
        if(deleteIfExists) {
            outputFile.delete();
        }
        return outputFile;
    }

    public static void verify(File expectedCsvFile, File outputCsvFile) throws IOException {
        BufferedReader expected = null;
        BufferedReader out = null;
        try {
            expected = new BufferedReader(new FileReader(expectedCsvFile));
            out = new BufferedReader(new FileReader(outputCsvFile));
            String lineIn;
            String lineOut = null;
            int lineNumber = 0;
            while ((lineIn = expected.readLine()) != null && (lineOut = out.readLine()) != null) {
                ++ lineNumber;
                lineOut = lineOut.substring(lineOut.indexOf("\t") + 1);
               // assertEquals("line " + lineNumber, lineIn, lineOut);
            }
            //assertNull("line " + lineNumber, lineIn);
            //assertNull("line " + lineNumber, out.readLine());
        } finally {
            Utils.closeQuietly(expected);
            Utils.closeQuietly(out);
        }
    }

    public static void verify(File expectedCsvFile, File outputCsvFile, boolean orderMatters) throws IOException {
        if(!orderMatters) {
            // sort the files before diff'ing them
            expectedCsvFile = sortFile(expectedCsvFile);
            outputCsvFile = sortFile(outputCsvFile);
        }
        verify(expectedCsvFile, outputCsvFile);
    }

    private static File sortFile(File inFile) throws IOException {
        File sortedFile = new File(inFile.getAbsolutePath().concat(".sorted"));
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        PrintWriter out = new PrintWriter(new FileWriter(sortedFile));

        try {
            String inputLine;
            List<String> lineList = new ArrayList<String>();
            while ((inputLine = reader.readLine()) != null) {
                lineList.add(inputLine);
            }
            Collections.sort(lineList);

            for (String outputLine : lineList) {
                out.println(outputLine);
            }
            out.flush();
        } finally {
            closeQuietly(reader);
            closeQuietly(out);
        }
        return sortedFile;
    }
}
