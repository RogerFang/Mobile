package edu.whu.irlab.mobile.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.whu.irlab.mobile.props.ConfigProps;
import edu.whu.irlab.mobile.util.FileUtil;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Created by Roger on 2016/6/29.
 */
public class ExSortService {
    private static Logger logger = LoggerFactory.getLogger(ExSortService.class);

    private static ExSortService instance = new ExSortService();

    private static ConfigProps configProps = ConfigProps.getInstance();

    // 单个子文件的记录数
    private static final int EXSORT_SINGLE_FILE_COUNT = Integer.parseInt(configProps.getProp("EXSORT_SINGLE_FILE_COUNT"));

    // 从大数据文件中, 一次缓冲读取的字节数(10M)
    // private static final int BUFFER_SIZE = 1024 * 1024 * 1;
    // 存储单行的字节数
    // private static final int LINE_SIZE = 2000;

    private ExSortService() {
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
        File sortedFile = FileUtil.getSortedFile(bigDataFile.getName());
        if (sortedFile.exists()){
            // 存在已排序文件
            logger.info("Start sorting: existing sorted file, sorted file={}", sortedFile.getAbsolutePath());
            return sortedFile;
        }

        logger.info("Start sorting: {}", bigDataFile.getAbsolutePath());
        List<File> splitFileList = splitFileData(bigDataFile);
        sortSplitFile(splitFileList);
        return mergeSplitFiles(splitFileList, bigDataFile.getName());
    }

    /**
     * 针对传入的文件列表分别进行外部排序
     * 1、判断是否已经存在该文件对应的已排序文件
     * 2、存在则不再重复进行排序, 否则对其排序并保存下来供以后使用
     * @param bigDataFileList
     * @return
     */
    public List<File> sort(List<File> bigDataFileList) throws IOException {
        List<File> sortedFileList = new ArrayList<>();
        for (File originFile: bigDataFileList){
            sortedFileList.add(sort(originFile));
        }
        return sortedFileList;
    }

    /**
     * 将大数据文件切分到几个小文件中
     */
    private List<File> splitFileData(File bigDataFile) throws IOException {
        logger.info("Start splitting file data: {}", bigDataFile.getAbsolutePath());
        List<File> splitFileList = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(bigDataFile));
        String line;
        int lineCount = 0;

        File splitFile = FileUtil.getSplitFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(splitFile));
        splitFileList.add(splitFile);
        while ((line = br.readLine()) != null){
            // 超出规定的子文件行数就新建一个子文件
            if (lineCount >= EXSORT_SINGLE_FILE_COUNT){
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
        logger.info("End splitting file data: {}, {} split files!", bigDataFile.getAbsolutePath(), splitFileList.size());
        return splitFileList;
    }

    /**
     * 将大数据文件的每个子文件进行排序
     *
     * @param splitFileList
     */
    private void sortSplitFile(List<File> splitFileList) throws IOException {
        logger.info("Start sorting every single split file: size={}", splitFileList.size());

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

        logger.info("End sorting every single split file: size={}", splitFileList.size());
    }

    /**
     * 对已排序的子文件进行合并
     *
     * @param splitFileList
     */
    private File mergeSplitFiles(List<File> splitFileList, String bigDataFileName) throws IOException {
        logger.info("Start Merging every single sorted split file: size={}, filename={}", splitFileList.size(), bigDataFileName);
        File mergeFile = FileUtil.getMergeFile(bigDataFileName);
        File tmpFile = FileUtil.getTmpFile();

        boolean flag = false;
        BufferedReader smallIn;
        BufferedReader largeIn;
        String smallLine;
        String largeLine;
        BufferedWriter mergeOut;

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

        // 结果保存在临时文件中, 复制到mergeFile
        if (flag){
            mergeFile.delete();
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                inputChannel = new FileInputStream(tmpFile).getChannel();
                outputChannel = new FileOutputStream(mergeFile).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } finally {
                inputChannel.close();
                outputChannel.close();
            }
        }

        tmpFile.delete();

        logger.info("End Merging every single sorted split file: size={}, filename={}", splitFileList.size(), mergeFile.getName());
        return mergeFile;
    }


    /*public static void main(String[] args) throws IOException {
        // ExSortService exSortService = new ExSortService();
        // File bigDataFile = new File("201408.txt");
        // File sortedFile = exSortService.sort(bigDataFile);
        // System.out.println(sortedFile.getAbsolutePath());
    }*/
}
