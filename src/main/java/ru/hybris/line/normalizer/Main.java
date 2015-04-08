package ru.hybris.line.normalizer;

import org.apache.commons.io.FilenameUtils;
import org.springframework.integration.file.RecursiveLeafOnlyDirectoryScanner;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ilya.Igolnikov on 08.04.2015.
 */
public class Main {

    private static final String REGEX_PATTERN = "/\\*\\s+\\d+\\s+\\*/";

    public static void main(String[] args) {

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        normalizeLines();
    }

    private static void normalizeLines() {
        int n = 0;
        for (File file : getAllJavaFiles()) {
            processFile(file.toString());
            System.out.println(file);
            n++;
        }
        System.out.println("" + n + " files processed");
    }

    private static List<File> getAllJavaFiles() {
        final File folder = new File(System.getProperty("user.dir"));
        final RecursiveLeafOnlyDirectoryScanner scanner = new RecursiveLeafOnlyDirectoryScanner();

        final CompositeFileListFilter<File> filter = new CompositeFileListFilter<>();
        filter.addFilter(new AcceptOnceFileListFilter<File>());
        filter.addFilter(new JavaFileListFilter());

        scanner.setFilter(filter);
        return scanner.listFiles(folder);
    }

    private static class JavaFileListFilter extends AbstractFileListFilter<File> {
        @Override
        protected boolean accept(File file) {
            return FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("java");
        }
    }

    private static void processFile(String oldFileName) {
        String tmpFileName = oldFileName.replace(".java", "_try.java");

        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new FileReader(oldFileName));
            bw = new BufferedWriter(new FileWriter(tmpFileName));
            Pattern pattern = Pattern.compile(REGEX_PATTERN);

            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                if (line.matches(REGEX_PATTERN + ".+")) {
                    Matcher matcher = pattern.matcher(line);
                    matcher.find();
                    int d = Integer.valueOf(matcher.group().replace("/*", "").replace("*/", "").trim());
                    while (lineNo < d) {
                        bw.write("\r\n");
                        lineNo++;
                    }
                }
                bw.write(line+"\r\n");
                lineNo++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                if(br != null)
                    br.close();
            } catch (IOException e) {
                //
            }
            try {
                if(bw != null)
                    bw.close();
            } catch (IOException e) {
                //
            }
        }
        // Once everything is complete, delete old file..
        File oldFile = new File(oldFileName);
        oldFile.delete();

        // And rename tmp file's name to old file name
        File newFile = new File(tmpFileName);
        newFile.renameTo(oldFile);

    }
}
