package service;

import org.apache.commons.lang3.StringUtils;
import props.ConfigProps;
import util.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roger on 2016/6/29.
 */
public class GenTrainService {
    private static GenTrainService instance = new GenTrainService();

    private static ConfigProps configProps = ConfigProps.getInstance();

    // 大数据文件交集进行拆分后的子文件记录数
    public static final int FEATURE_SINGLE_FILE_COUNT = Integer.parseInt(configProps.getProp("FEATURE_SINGLE_FILE_COUNT"));

    // 本次训练train存放最终索引特征数据的目录
    private String trainDirForThisTime;
    // 本次训练test存放最终索引特征数据的目录
    private String testDirForThisTime;

    private PreprocessService preprocessService = PreprocessService.getInstance();

    private GenTrainService(){
    }

    public static GenTrainService getInstance(){
        return instance;
    }

    public void genMultiMonth(List<String> months, boolean isClassification){
        List<File> files = new ArrayList<File>();
        for (String month: months){
            files.add(new File(month));
        }

        int N = months.size();
        String dirName;
        if (isClassification){
            dirName = "classify_"+files.get(N-1).getName().split("\\.")[0]+"_"+(N-1);
        }else {
            dirName = "regress_"+files.get(N-1).getName().split("\\.")[0]+"_"+(N-1);
        }
        makeTrainAndTestDirForEveryTime(dirName);
        try {
            genTrainAndTestData(files, isClassification);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void genTrainAndTestData(List<File> bigDataFiles, boolean isClassification) throws IOException {
        preprocessService.setIsTrain(true);
        preprocessService.setIsClassification(isClassification);
        File interFile = preprocessService.genData(bigDataFiles);
        BufferedReader br = new BufferedReader(new FileReader(interFile));

        List<String> splitFileRecordsTrain = new ArrayList<>();
        List<String> splitFileRecordsTest = new ArrayList<>();
        // train索引文件索引
        int indexTrain = 0;
        // test索引文件索引
        int indexTest = 0;
        File splitFileTrain = FileUtil.getIndexSplitFile(trainDirForThisTime, indexTrain);
        BufferedWriter bwTrain = new BufferedWriter(new FileWriter(splitFileTrain));
        splitFileRecordsTrain.add(splitFileTrain.getName());
        File splitFileTest = FileUtil.getIndexSplitFile(testDirForThisTime, indexTest);
        BufferedWriter bwTest = new BufferedWriter(new FileWriter(splitFileTest));
        splitFileRecordsTest.add(splitFileTest.getName());

        int allCount = 0;
        // 用于切分文件的计数
        int countTrain = 0;
        // 用于切分文件的计数
        int countTest = 0;
        int totalCountTrain = 0;
        int totalCountTest = 0;

        String line;
        while ((line=br.readLine()) != null){
            if (StringUtils.isNotEmpty(line)){

                if (allCount%10 == 0){
                    // test数据
                    bwTest.write(line);
                    bwTest.newLine();
                    countTest++;
                    if (countTest >= FEATURE_SINGLE_FILE_COUNT){
                        bwTest.close();
                        indexTest++;
                        splitFileTest = FileUtil.getIndexSplitFile(testDirForThisTime, indexTest);
                        bwTest = new BufferedWriter(new FileWriter(splitFileTest));
                        splitFileRecordsTest.add(splitFileTest.getName());
                        countTest = 0;
                    }
                    totalCountTest++;
                }else {
                    // train数据
                    bwTrain.write(line);
                    bwTrain.newLine();
                    countTrain++;
                    if (countTrain >= FEATURE_SINGLE_FILE_COUNT){
                        bwTrain.close();
                        indexTrain++;
                        splitFileTrain = FileUtil.getIndexSplitFile(trainDirForThisTime, indexTrain);
                        bwTrain = new BufferedWriter(new FileWriter(splitFileTrain));
                        splitFileRecordsTrain.add(splitFileTrain.getName());
                        countTrain = 0;
                    }
                    totalCountTrain++;
                }

                allCount++;
            }
        }

        if (bwTest != null){
            bwTest.close();
        }
        if (bwTrain != null){
            bwTrain.close();
        }
        if (br != null){
            br.close();
        }

        BufferedWriter bwIndexTrain = new BufferedWriter(new FileWriter(trainDirForThisTime + File.separator + "index.txt"));
        bwIndexTrain.write("allCount:"+totalCountTrain);bwIndexTrain.newLine();
        bwIndexTrain.write("perCount:"+FEATURE_SINGLE_FILE_COUNT);bwIndexTrain.newLine();
        for (int i=0; i<splitFileRecordsTrain.size(); i++){
            bwIndexTrain.write(i+":"+splitFileRecordsTrain.get(i));
            bwIndexTrain.newLine();
        }

        if (bwIndexTrain != null){
            bwIndexTrain.close();
        }

        BufferedWriter bwIndexTest = new BufferedWriter(new FileWriter(testDirForThisTime + File.separator + "index.txt"));
        bwIndexTest.write("allCount:"+totalCountTest);bwIndexTest.newLine();
        bwIndexTest.write("perCount:"+FEATURE_SINGLE_FILE_COUNT);bwIndexTest.newLine();
        for (int i=0; i<splitFileRecordsTest.size(); i++){
            bwIndexTest.write(i+":"+splitFileRecordsTest.get(i));
            bwIndexTest.newLine();
        }

        if (bwIndexTest != null){
            bwIndexTest.close();
        }

        if (interFile != null){
            interFile.delete();
        }
    }

    private void makeTrainAndTestDirForEveryTime(String dirName){
        File fileTrainForThisTime = FileUtil.makeTrainDirForEveryTime(dirName);
        File fileTestForThisTime = FileUtil.makeTestDirForEveryTime(dirName);
        this.trainDirForThisTime = fileTrainForThisTime.getAbsolutePath();
        this.testDirForThisTime = fileTestForThisTime.getAbsolutePath();
    }

    public String getTrainDirForThisTime() {
        return trainDirForThisTime;
    }

    public String getTestDirForThisTime() {
        return testDirForThisTime;
    }

    /*public static void main(String[] args) {
        List<String> files = new ArrayList<>();
        files.add("201408.txt");
        files.add("201409.txt");
        GenTrainService genTrainService = new GenTrainService();
        genTrainService.genMultiMonth(files, true);
        System.out.println(genTrainService.getTrainDirForThisTime());
        System.out.println(genTrainService.getTestDirForThisTime());
    }*/
}
