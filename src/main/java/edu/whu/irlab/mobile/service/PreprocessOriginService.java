package edu.whu.irlab.mobile.service;

import com.google.common.base.Joiner;
import edu.whu.irlab.mobile.util.CalendarUtil;
import edu.whu.irlab.mobile.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roger on 2016/7/21.
 */
public class PreprocessOriginService extends AbstractPreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessOriginService.class);

    private static PreprocessOriginService instance = new PreprocessOriginService();

    private PreprocessOriginService() {
    }

    public static PreprocessOriginService getInstance(){
        return instance;
    }

    /**
     * 79个特征: 在最初开始的71个特征上，稍作整改，依赖前4个月数据
     * @param bigDataFiles
     * @return
     * @throws IOException
     */
    @Override
    public File genData(List<File> bigDataFiles) throws IOException {
        logger.info("Start preprocessing: exsort and generate intersection");
        List<File> deriveDataFile = new ArrayList<>();
        // 获取生成衍生特征数据所需的大数据文件
        File firstFile = bigDataFiles.get(0);
        String firstFilename = firstFile.getName();
        String firstMonth = firstFilename.substring(0, 6);
        String fileExtension = firstFilename.substring(6);
        for (int i=0; i<4; i++){
            String month = CalendarUtil.getLastMonth(firstMonth, i+1);
            File monthFile = FileUtil.getDataFile(month + fileExtension);
            if (!monthFile.exists()){
                logger.info("Data file for generating derived feature doesn't exist!");
                return null;
            }
            deriveDataFile.add(monthFile);
        }

        if (isTrain()){
            deriveDataFile.addAll(bigDataFiles.subList(0, bigDataFiles.size()-2));
        }else {
            deriveDataFile.addAll(bigDataFiles.subList(0, bigDataFiles.size()-1));
        }

        File initFile = genDataInit(bigDataFiles);

        File deriveFile = genDataDerive(deriveDataFile, initFile);

        File mergeFile = mergeFeatureData(initFile, deriveFile);

        initFile.delete();
        deriveFile.delete();
        logger.info("End preprocessing: exsort and generate intersection");
        return mergeFile;
    }

    private static int featureIndexes[] = new int[]{2, 4, 5, 8, 9, 11, 14, 16, 19, 23, 24, 27, 30, 31, 33, 35, 36, 39, 40, 43, 44, 46, 47, 48, 51, 53, 54, 55, 58, 59, 60, 61, 63, 64, 65, 68, 69, 70, 71, 72, 73, 75, 77};
    private static int removeIndexes[] = new int[]{1, 34, 37, 12, 18, 57, 26};

    @Override
    public File mergeFeatureData(File initFile, File deriveFile) throws IOException {
        logger.info("Start merge feature data: initFile={}, deriveFile={}", initFile.getAbsolutePath(), deriveFile.getAbsolutePath());
        BufferedReader brInit = new BufferedReader(new FileReader(initFile));
        BufferedReader brDerive = new BufferedReader(new FileReader(deriveFile));

        File mergeFile = FileUtil.getInterTmpFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(mergeFile));
        String lineInit = brInit.readLine();
        String lineDerive = brDerive.readLine();

        while (lineInit != null){
            String[] chunksInit = lineInit.split(";");
            if (lineDerive == null){
                String[] propsInit = chunksInit[1].split(",");
                for (int removeIndex: removeIndexes){
                    propsInit[removeIndex] = null;
                }
                chunksInit[1] = Joiner.on(",").skipNulls().join(propsInit);

                bw.write(Joiner.on(",").skipNulls().join(chunksInit));
                bw.newLine();
                lineInit = brInit.readLine();
            }else {
                String[] chunksDerive = lineDerive.split(";");
                if (chunksDerive[0].compareTo(chunksInit[0]) == 0){
                    int endIndex;
                    if (isTrain()){
                        endIndex = chunksInit.length - 1;
                    }else {
                        endIndex = chunksInit.length;
                    }

                    for (int i=1; i<endIndex; i++){
                        String[] propsInit = chunksInit[i].split(",");
                        String[] propsDerive1 = chunksDerive[1].split(",");
                        String[] propsDerive2 = chunksDerive[2].split(",");
                        // 前三个月数据
                        int f1 = (Integer.valueOf(propsDerive1[i]) + Integer.valueOf(propsDerive1[i+1]) + Integer.valueOf(propsDerive1[i+2]))/3;

                        String feature1;
                        if (f1>4){
                            feature1 = "1";
                        }else {
                            feature1 = "0";
                        }

                        // 前四个月数据
                        double f2 = (Double.valueOf(propsDerive2[i-1]) + Double.valueOf(propsDerive2[i]) + Double.valueOf(propsDerive2[i+1]) + Double.valueOf(propsDerive2[i+2]))/4;
                        double f = Double.valueOf(propsInit[propsInit.length - 1]);
                        String feature2;
                        if (f>f2){
                            feature2 = "1";
                        }else {
                            feature2 = "0";
                        }
                        propsInit[propsInit.length -2] = feature1;
                        propsInit[propsInit.length -1] = feature2;

                        // propsInit[removeIndex] = null;
                        for (int removeIndex: removeIndexes){
                            propsInit[removeIndex] = null;
                        }
                        chunksInit[i] = Joiner.on(",").skipNulls().join(propsInit);
                    }



                    bw.write(StringUtils.join(chunksInit, ","));
                    bw.newLine();

                    lineInit = brInit.readLine();
                    lineDerive = brDerive.readLine();

                }else if (chunksDerive[0].compareTo(chunksInit[0]) > 0){
                    // brInit 往下读
                    String[] propsInit = chunksInit[1].split(",");
                    for (int removeIndex: removeIndexes){
                        propsInit[removeIndex] = null;
                    }
                    chunksInit[1] = Joiner.on(",").skipNulls().join(propsInit);

                    bw.write(Joiner.on(",").skipNulls().join(chunksInit));
                    bw.newLine();
                    lineInit = brInit.readLine();
                }else if (chunksDerive[0].compareTo(chunksInit[0]) < 0){
                    // brDerive 往下读
                    lineDerive = brDerive.readLine();
                }
            }
        }

        if (brInit != null){
            brInit.close();
        }
        if (brDerive != null){
            brDerive.close();
        }
        if (bw != null){
            bw.close();
        }
        logger.info("Final merge feature file: {}", mergeFile.getAbsolutePath());
        return mergeFile;
    }

    @Override
    String getFeatureRecordInit(String mobile, List<String> lines) {
        List<String> linesFeatureRecord = new ArrayList<>();
        linesFeatureRecord.add(mobile);
        int count = 0;
        for (String line: lines){
            List<String> singleLineFeature = new ArrayList<>();
            String[] props = line.split(",");

            // 第一个特征,第一次欠费距离本月的月数
            String[] now = props[1].split("-");
            String[] last = props[5].split("-");
            if (now.length<2 || last.length<2){
                return null;
            }
            singleLineFeature.add(String.valueOf((Integer.parseInt(now[0]) - Integer.parseInt(last[0])) * 12 + (Integer.parseInt(now[1]) - Integer.parseInt(last[1]))));
            // 使用子品牌编号
            singleLineFeature.add(props[4].split("-")[0]);
            // 次欠费的时间转化为历史上是否欠费
            if (props[6].equals("0")){
                singleLineFeature.add(props[6]);
            }else {
                singleLineFeature.add("1");
            }

            // feature
            for (int i=0; i<props.length; i++){
                if (i<7){
                    continue;
                }else if (i==62 || i==61){
                    continue;
                }else  if (i==props.length-1){
                    continue;
                }

                String tmp = props[i];
                if (tmp.equals("是")){
                    tmp = "1";
                }else if (tmp.equals("否")){
                    tmp = "0";
                }else if (tmp.equals("")){
                    tmp = "0";
                }else if (tmp.equals("不详")){
                    tmp = "0";
                }

                if (tmp.equals("代销商")){
                    tmp = "1,0,0";
                }
                if (tmp.equals("零售店")){
                    tmp = "0,1,0";
                }
                if (tmp.equals("营业厅")){
                    tmp = "0,0,1";
                }

                if (tmp.equals("现金")){
                    tmp = "1,0,0,0,0";
                }
                if (tmp.equals("借记卡")){
                    tmp = "0,1,0,0,0";
                }
                if (tmp.equals("银行托收")){
                    tmp = "0,0,1,0,0";
                }
                if (tmp.equals("信用卡")){
                    tmp = "0,0,0,1,0";
                }if (tmp.equals("支票")){
                    tmp = "0,0,0,0,1";
                }
                if (tmp.equals("未知")){
                    tmp = "0,0,0,0,0";
                }

                if (tmp.equals("级别不详")){
                    tmp = "0";
                }

                singleLineFeature.add(tmp);
            }

            // 衍生特征: 是否前三个月月均用户通信对端手机号码数大于4
            singleLineFeature.add(props[74]);
            // 衍生特征: 当月漫游通话与前4个月的平均值相比是否增加
            singleLineFeature.add(props[25]);

            if (count == lines.size()-1){
                if (isTrain()){
                    // train&test
                    if (isClassification()){
                        // classify
                        if (props[2].equals("在网-开通")){
                            linesFeatureRecord.add("1");
                        }else{
                            linesFeatureRecord.add("0");
                        }
                        /*if(Double.parseDouble(props[46]) >= 0){
                            linesFeatureRecord.add("1");
                        }else{
                            linesFeatureRecord.add("0");
                        }*/
                    }else {
                        // regress
                        linesFeatureRecord.add(props[22]);
                    }
                }else {
                    // predict
                    linesFeatureRecord.add(StringUtils.join(singleLineFeature, ","));
                }
            }else {
                linesFeatureRecord.add(StringUtils.join(singleLineFeature, ","));
            }

            count++;
        }
        return StringUtils.join(linesFeatureRecord, ";");
    }

    @Override
    String getFeatureRecordDerive(String mobile, List<String> lines) {
        List<String> linesFeatureRecord = new ArrayList<>();
        List<String> props1 = new ArrayList<>();
        List<String> props2 = new ArrayList<>();
        linesFeatureRecord.add(mobile);
        for (int i=0; i<lines.size()-1; i++){
            String[] props = lines.get(i).split(",");
            // 第一个特征:用户通信对端手机号码数
            props1.add(props[74]);

            // 第二个特征:漫游通话费
            props2.add(props[25]);

        }
        linesFeatureRecord.add(StringUtils.join(props1, ","));
        linesFeatureRecord.add(StringUtils.join(props2, ","));
        return StringUtils.join(linesFeatureRecord, ";");
    }
}
