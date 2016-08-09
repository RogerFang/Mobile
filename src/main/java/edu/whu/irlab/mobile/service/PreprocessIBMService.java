package edu.whu.irlab.mobile.service;

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
public class PreprocessIBMService extends AbstractPreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessIBMService.class);

    private static PreprocessIBMService instance = new PreprocessIBMService();

    private PreprocessIBMService() {}

    public static PreprocessIBMService getInstance(){
        return instance;
    }

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
                bw.write(StringUtils.join(chunksInit, ","));
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

                        chunksInit[i] = StringUtils.join(propsInit, ",");
                    }

                    bw.write(StringUtils.join(chunksInit, ","));
                    bw.newLine();

                    lineInit = brInit.readLine();
                    lineDerive = brDerive.readLine();

                }else if (chunksDerive[0].compareTo(chunksInit[0]) > 0){
                    // brInit 往下读
                    bw.write(StringUtils.join(chunksInit, ","));
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
            if (count == lines.size()-1){
                continue;
            }

            List<String> singleLineFeature = new ArrayList<>();
            String[] props = line.split(",");

            // F1: 是否集团用户
            if (props[8].equals("是")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            // F2: 总充值额度
            singleLineFeature.add(props[18]);

            // F3: 欠费月份数
            singleLineFeature.add(props[45]);

            // F4: 客户账户普通余额
            singleLineFeature.add(props[46]);

            // F5: 用户总积分
            singleLineFeature.add(props[47]);

            // F6: 语音通话次数
            singleLineFeature.add(props[50]);

            // F7: GPRS4G流量
            singleLineFeature.add(props[69]);

            // F8: 当月ARPU
            singleLineFeature.add(props[70]);

            // F9: 是否为代销商
            if (props[15].equals("代销商")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            // F10: 前3月通信对端均值是否大于4
            singleLineFeature.add("0");

            // F11: 漫游通话费与前四个月的均值相比是否有所增加
            singleLineFeature.add(props[25]);

            linesFeatureRecord.add(StringUtils.join(singleLineFeature, ","));

            if (props[2].equals("在网-开通")){
                linesFeatureRecord.add("1");
            }else{
                linesFeatureRecord.add("0");
            }

            /*if (count == lines.size()-1){
                if (isTrain()){
                    // train&test
                    if (isClassification()){
                        // classify
                        if (props[2].equals("在网-开通")){
                            linesFeatureRecord.add("1");
                        }else{
                            linesFeatureRecord.add("0");
                        }
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
            }*/

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
