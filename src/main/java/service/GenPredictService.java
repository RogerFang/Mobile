package service;

import org.apache.commons.lang3.StringUtils;
import util.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roger on 2016/6/29.
 */
public class GenPredictService {

    // 大数据文件交集进行拆分后的子文件记录数
    public static final int PER_FILE_COUNT = 30000;
    // 预测特征数据根目录
    private static final String PREDICT_DIR = "predict";

    // 本次预测存放最终索引特征数据的目录
    private String predictDirForThisTime;

    private PreprocessService preprocessService = PreprocessService.getInstance();

    private GenPredictService(){
        File dir = new File(PREDICT_DIR);
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    public void genMultiMonth(List<String> months){
        String dataPath = null;
        List<File> files = new ArrayList<File>();
        for (String month: months){
            files.add(new File(month));
        }

        int N = months.size();
        String dirName = files.get(N-1).getName().split("\\.")[0]+"_"+N;
        makePredictDirForEveryTime(dirName);
        try {
            genPredictData(files, predictDirForThisTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void genPredictData(List<File> bigDataFiles, String predictDirForThisTime) throws IOException {
        preprocessService.setIsTrain(false);
        File interFile = preprocessService.genData(bigDataFiles);
        BufferedReader br = new BufferedReader(new FileReader(interFile));

        List<String> splitFileRecords = new ArrayList<>();
        // 索引文件索引
        int index = 0;
        File splitFile = FileUtil.getPredictSplitFile(predictDirForThisTime, index);
        BufferedWriter bw = new BufferedWriter(new FileWriter(splitFile));
        splitFileRecords.add(splitFile.getName());

        int allCount = 0;
        // 用于切分文件的计数
        int count = 0;
        String line = null;
        while ((line=br.readLine()) != null){
            if (StringUtils.isNotEmpty(line)){
                bw.write(line);
                bw.newLine();
                count++;
                if (count >= PER_FILE_COUNT){
                    bw.close();
                    index++;
                    splitFile = FileUtil.getPredictSplitFile(predictDirForThisTime, index);
                    bw = new BufferedWriter(new FileWriter(splitFile));
                    splitFileRecords.add(splitFile.getName());
                    count = 0;
                }
                allCount++;
            }
        }

        if (bw != null){
            bw.close();
        }
        if (br != null){
            br.close();
        }

        BufferedWriter bwIndex = new BufferedWriter(new FileWriter(predictDirForThisTime + File.separator + "index.txt"));
        bwIndex.write("allCount:"+allCount);bwIndex.newLine();
        bwIndex.write("perCount:"+PER_FILE_COUNT);bwIndex.newLine();
        for (int i=0; i<splitFileRecords.size(); i++){
            bwIndex.write(i+":"+splitFileRecords.get(i));
            bwIndex.newLine();
        }

        if (bwIndex != null){
            bwIndex.close();
        }

        if (interFile != null){
            interFile.delete();
        }
    }

    /**
     * 生成每次预测的特征索引文件存放目录
     * @param dirName 本次预测特征数据存放的目录名
     * @return
     */
    private void makePredictDirForEveryTime(String dirName){
        File filePredictForThisTime = FileUtil.makePredictDirForEveryTime(dirName);
        this.predictDirForThisTime = filePredictForThisTime.getAbsolutePath();
    }

    public String getPredictDirForThisTime() {
        return predictDirForThisTime;
    }

    public static void main(String[] args) {
        List<String> files = new ArrayList<>();
        files.add("201408.txt");
        files.add("201409.txt");
        GenPredictService genPredictService = new GenPredictService();
        genPredictService.genMultiMonth(files);
        System.out.println(genPredictService.getPredictDirForThisTime());
    }
}
