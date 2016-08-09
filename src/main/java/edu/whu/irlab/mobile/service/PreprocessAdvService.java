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
public class PreprocessAdvService extends AbstractPreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessAdvService.class);

    private static PreprocessAdvService instance = new PreprocessAdvService();

    private PreprocessAdvService() {}

    public static PreprocessAdvService getInstance(){
        return instance;
    }

    /**
     * 在最初开始的71个特征上，稍作整改，依赖前3个月数据
     *
     * @param bigDataFiles 大数据文件列表
     * @return 返回大数据文件集的交集记录
     */
    public File genData(List<File> bigDataFiles) throws IOException {
        logger.info("Start preprocessing: exsort and generate intersection");
        List<File> deriveDataFile = new ArrayList<>();
        // 获取生成衍生特征数据所需的大数据文件
        File firstFile = bigDataFiles.get(0);
        String firstFilename = firstFile.getName();
        String firstMonth = firstFilename.substring(0, 6);
        String fileExtension = firstFilename.substring(6);
        for (int i=0; i<3; i++){
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
                        // feature:上个月余额是否小于0
                        String feature1 = propsDerive1[i+1];
                        // feature:上上个月余额是否小于0
                        String feature2 = propsDerive1[i];

                        // feature:前三个月总消费的平均值
                        double f2 = (Double.valueOf(propsDerive2[i-1]) + Double.valueOf(propsDerive2[i]) + Double.valueOf(propsDerive2[i]))/3;
                        String feature3 = String.valueOf(f2);
                        double f = Double.valueOf(propsInit[42]);
                        // feature:当前月份余额减去平均值是否大于0
                        String feature4;
                        if (f > f2){
                            feature4 = "1";
                        }else {
                            feature4 = "0";
                        }
                        propsInit[propsInit.length -4] = feature1;
                        propsInit[propsInit.length -3] = feature2;
                        propsInit[propsInit.length -2] = feature3;
                        propsInit[propsInit.length -1] = feature4;

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
            List<String> singleLineFeature = new ArrayList<>();
            String[] props = line.split(",");

            // 第一个特征,第一次欠费距离本月的月数
            String[] now = props[1].split("-");
            String[] last = props[5].split("-");
            if (now.length<2 || last.length<2){
                return null;
            }
            //F1
            singleLineFeature.add(String.valueOf((Integer.parseInt(now[0]) - Integer.parseInt(last[0])) * 12 + (Integer.parseInt(now[1]) - Integer.parseInt(last[1]))));

            //F2,3,4,5,6
            // 使用品牌编号: 0,1 (动感地带品牌 全球通品牌 数据卡品牌 神州行品牌 TD先锋卡 )
            if (props[3].equals("动感地带品牌")){
                singleLineFeature.add("1,0,0,0,0");
            }else if (props[3].equals("全球通品牌")){
                singleLineFeature.add("0,1,0,0,0");
            }else if (props[3].equals("数据卡品牌")){
                singleLineFeature.add("0,0,1,0,0");
            }else if (props[3].equals("神州行品牌")){
                singleLineFeature.add("0,0,0,1,0");
            }else if (props[3].equals("TD先锋卡")){
                singleLineFeature.add("0,0,0,0,1");
            }

            //F7
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
                }else if (i==61 || i==62 || i==63 || i==64 || i==65 || i==66 || i==67 || i==68 || i==69){
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

                // 付款方式
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
            // singleLineFeature.add(props[74]);
            //衍生特征: 当月漫游通话与前4个月的平均值相比是否增加
            // singleLineFeature.add(props[25]);

            //add2:用户当月用户余额是否小于0
            if (Double.parseDouble(props[46]) < 0){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //add3:default 0
            singleLineFeature.add("0");
            //add4:default 0
            singleLineFeature.add("0");
            //add5:default 0.0
            singleLineFeature.add("0.0");
            //add6:default 0
            singleLineFeature.add("0");

            if (count == lines.size()-1){
                if (isTrain()){
                    // train&test
                    if (isClassification()){
                        // classify
                        /*if (props[2].equals("在网-开通")){
                            linesFeatureRecord.add("1");
                        }else{
                            linesFeatureRecord.add("0");
                        }*/

                        if(Double.parseDouble(props[46]) >= 0){
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
            // 特征:余额是否小于0
            if (Double.parseDouble(props[46]) < 0){
                props1.add("1");
            }else {
                props1.add("0");
            }

            // 特征:总消费 用于计算(最近三个月总消费的平均值和当前月份余额减去平均值是否大于0)
            props2.add(props[23]);
        }
        linesFeatureRecord.add(StringUtils.join(props1, ","));
        linesFeatureRecord.add(StringUtils.join(props2, ","));
        return StringUtils.join(linesFeatureRecord, ";");
    }
}
