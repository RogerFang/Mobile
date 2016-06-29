package service;

import org.apache.commons.lang3.StringUtils;
import util.FileUtil;

import java.io.*;
import java.util.*;

/**
 * Created by Roger on 2016/6/29.
 */
public class ExSortService {

    private static ExSortService instance = new ExSortService();

    // 单个子文件的记录数
    private static final int SINGLE_FILE_COUNT = 10000 * 3;

    // 从大数据文件中, 一次缓冲读取的字节数(10M)
    private static final int BUFFER_SIZE = 1024 * 1024 * 1;

    // 存储子文件的临时目录
    private static final String TMP_DIR = "tmp";

    // 存储单行的字节数
    private static final int LINE_SIZE = 2000;

    private static final String FILE_EXTENSION = ".txt";

    private ExSortService() {
        File tmpFile = new File(TMP_DIR);
        if (!tmpFile.exists()){
            tmpFile.mkdirs();
        }
    }

    public static ExSortService getInstance(){
        return instance;
    }

    /**
     * 大数据文件外部排序入口
     * @param bigDataFile 大数据文件
     * @return 返回排序后的合并文件
     */
    public File sort(File bigDataFile) throws IOException {
        List<File> splitFileList = splitFileData(bigDataFile);
        sortSplitFile(splitFileList);
        return mergeSplitFiles(splitFileList);
    }

    /**
     * 将大数据文件切分到几个小文件中
     */
    public List<File> splitFileData(File bigDataFile) throws IOException {
        System.out.println("INFO: single file data sort!");
        List<File> splitFileList = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(bigDataFile));
        String line = null;
        int lineCount = 0;

        File splitFile = FileUtil.getSplitFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(splitFile));
        splitFileList.add(splitFile);
        while ((line = br.readLine()) != null){
            // 超出规定的子文件行数就新建一个子文件
            if (lineCount >= SINGLE_FILE_COUNT){
                bw.close();
                splitFile = FileUtil.getSplitFile();
                bw = new BufferedWriter(new FileWriter(splitFile));
                splitFileList.add(splitFile);
                lineCount = 0;
            }

            bw.write(line);
            bw.newLine();
            lineCount++;
        }

        if (bw != null){
            bw.close();
        }
        if (br != null){
            br.close();
        }

        return splitFileList;
    }

    /**
     * 将大数据文件的单个子文件进行排序
     *
     * @param splitFileList
     */
    public void sortSplitFile(List<File> splitFileList) throws IOException {
        System.out.println("INFO: single file data sort!");

        for (File splitFile: splitFileList){
            BufferedReader br = new BufferedReader(new FileReader(splitFile));

            Set<String> splitDataSet = new TreeSet<>();

            String line = null;
            while ((line=br.readLine()) != null){
                if (StringUtils.isEmpty(line)){
                    continue;
                }
                if (!line.matches("^\\d{11}.*")){
                    continue;
                }
                splitDataSet.add(line);
            }
            br.close();

            BufferedWriter bw = new BufferedWriter(new FileWriter(splitFile));
            for (String str: splitDataSet){
                bw.write(str);
                bw.newLine();
            }
            bw.close();
        }
    }

    /**
     * 对已排序的子文件进行合并
     *
     * @param splitFileList
     */
    public File mergeSplitFiles(List<File> splitFileList) throws IOException {
        File mergeFile = FileUtil.getMergeFile();
        File tmpFile = FileUtil.getTmpFile();

        boolean flag = false;
        BufferedReader smallIn = null;
        BufferedReader largeIn = null;
        String smallLine = null;
        String largeLine = null;
        BufferedWriter mergeOut = null;

        for (File splitFile: splitFileList){
            smallIn = new BufferedReader(new FileReader(splitFile));

            if (flag){
                largeIn = new BufferedReader(new FileReader(tmpFile));
                mergeOut = new BufferedWriter(new FileWriter(mergeFile));
                flag = false;
            }else {
                largeIn = new BufferedReader(new FileReader(mergeFile));
                mergeOut = new BufferedWriter(new FileWriter(tmpFile));
                flag = true;
            }

            smallLine = smallIn.readLine();
            largeLine = largeIn.readLine();
            while (smallLine != null && largeLine != null){
                int rt = smallLine.compareTo(largeLine);
                if (rt < 0){
                    mergeOut.write(smallLine);
                    smallLine = smallIn.readLine();
                }else {
                    mergeOut.write(largeLine);
                    largeLine = largeIn.readLine();
                }
                mergeOut.newLine();
            }

            while (smallLine != null){
                mergeOut.write(smallLine);
                mergeOut.newLine();
                smallLine = smallIn.readLine();
            }
            while (largeLine != null){
                mergeOut.write(largeLine);
                mergeOut.newLine();
                largeLine = largeIn.readLine();
            }

            mergeOut.close();
            smallIn.close();
            largeIn.close();

            splitFile.delete();
        }

        if (flag){
            mergeFile.delete();
            return tmpFile;
        }else {
            tmpFile.delete();
            return mergeFile;
        }
    }


    public static void main(String[] args) throws IOException {
        ExSortService exSortService = new ExSortService();
        File bigDataFile = new File("201412.txt");
        exSortService.sort(bigDataFile);
    }
}
