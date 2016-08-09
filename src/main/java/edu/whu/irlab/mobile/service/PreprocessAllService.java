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
public class PreprocessAllService extends AbstractPreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessAllService.class);

    private static PreprocessAllService instance = new PreprocessAllService();

    private PreprocessAllService() {
    }

    public static PreprocessAllService getInstance(){
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
                logger.info("Data file {} for generating derived feature doesn't exist!", monthFile.getCanonicalPath());
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

    private static int removeIndexes[] = new int[]{3, 5, 17, 18, 19, 22, 23, 24, 26, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 44, 49, 50, 51, 53, 58, 59, 62, 63, 65, 68, 69, 70, 78, 79};
    // private static int removeIndexes[] = new int[]{1, 3, 5, 6, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 48, 49, 50, 51, 52, 53, 54, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 74, 75, 76, 77, 78, 79};
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
                        String[] propsDerive3 = chunksDerive[3].split(",");
                        String[] propsDerive4 = chunksDerive[4].split(",");

                        // feature:上个月余额是否小于0
                        String feature1 = propsDerive1[i+1];
                        // feature:上上个月余额是否小于0
                        String feature2 = propsDerive1[i];

                        // feature:前三个月总消费的平均值
                        double f2 = (Double.valueOf(propsDerive2[i-1]) + Double.valueOf(propsDerive2[i]) + Double.valueOf(propsDerive2[i]))/3;
                        String feature3 = String.valueOf(f2);
                        double f = Double.valueOf(propsInit[propsInit.length -3]);
                        // feature:当前月份余额减去平均值是否大于0
                        String feature4;
                        if (f > f2){
                            feature4 = "1";
                        }else {
                            feature4 = "0";
                        }
                        propsInit[propsInit.length -6] = feature1;
                        propsInit[propsInit.length -5] = feature2;
                        propsInit[propsInit.length -4] = feature3;
                        propsInit[propsInit.length -3] = feature4;

                        // 前三个月数据
                        int f5 = (Integer.valueOf(propsDerive3[i]) + Integer.valueOf(propsDerive3[i+1]) + Integer.valueOf(propsDerive3[i+2]))/3;
                        String feature5;
                        if (f5>4){
                            feature5 = "1";
                        }else {
                            feature5 = "0";
                        }

                        // 前四个月数据
                        double f6 = (Double.valueOf(propsDerive4[i-1]) + Double.valueOf(propsDerive4[i]) + Double.valueOf(propsDerive4[i+1]) + Double.valueOf(propsDerive4[i+2]))/4;
                        double fe6 = Double.valueOf(propsInit[propsInit.length - 1]);
                        String feature6;
                        if (f6>fe6){
                            feature6 = "1";
                        }else {
                            feature6 = "0";
                        }
                        propsInit[propsInit.length -2] = feature5;
                        propsInit[propsInit.length -1] = feature6;

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

            //F1: 第一次欠费距离本月的月数
            String[] now = props[1].split("-");
            String[] last = props[5].split("-");
            if (now.length<2 || last.length<2){
                return null;
            }
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

            //F8: 用户在网时长（月）
            singleLineFeature.add(props[7]);

            //F9: 是否集团成员
            if (props[8].equals("是")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F10: 是否实名制客户
            if (props[9].equals("是")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F11: 是否硬捆绑
            if (props[10].equals("是")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F12: 是否双卡用户
            if (props[11].equals("是")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F13: 套餐流量资源使用率
            if (props[12].equals("")){
                singleLineFeature.add("0");
            }else {
                singleLineFeature.add(props[12]);
            }

            //F14: 套餐语音资源使用率
            if (props[13].equals("")){
                singleLineFeature.add("0");
            }else {
                singleLineFeature.add(props[13]);
            }

            //F15: 套餐短信资源使用率
            if (props[14].equals("")){
                singleLineFeature.add("0");
            }else {
                singleLineFeature.add(props[14]);
            }

            //F16: 码号资源-渠道大类(零售店)
            if (props[15].equals("零售店")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F17: 码号资源-渠道大类(代销商)
            if (props[15].equals("代销商")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F18: 码号资源-渠道大类(营业厅)
            if (props[15].equals("营业厅")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F19: 最后付费类型（近6个月） (现金)
            if (props[16].equals("现金")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //F20: 最后付费类型（近6个月） (借记卡)
            if (props[16].equals("借记卡")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //F21: 最后付费类型（近6个月） (银行托收)
            if (props[16].equals("银行托收")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //F22: 最后付费类型（近6个月） (信用卡)
            if (props[16].equals("信用卡")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //F23: 最后付费类型（近6个月） (未知)
            if (props[16].equals("未知")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            //F24: 总充值次数
            singleLineFeature.add(props[17]);
            //F25: 总充值额度
            singleLineFeature.add(props[18]);
            //F26: 银行代收费
            singleLineFeature.add(props[19]);
            //F27: 银行托收
            singleLineFeature.add(props[20]);
            //F28: 小额充值
            singleLineFeature.add(props[21]);
            //F29: 当月欠费
            singleLineFeature.add(props[22]);
            //F30: 总消费（元）
            singleLineFeature.add(props[23]);
            //F31: 本地通话费（元）
            singleLineFeature.add(props[24]);
            //F32: 漫游通话费（元）
            singleLineFeature.add(props[25]);
            //F33: 漫游通话费-省际漫游费（元）
            singleLineFeature.add(props[26]);
            //F34: 漫游通话费-港澳台漫游出访费用（元）
            singleLineFeature.add(props[27]);
            //F35: 漫游通话费-国际漫游费（元）
            singleLineFeature.add(props[28]);
            //F36: 普通国内长途费（元）
            singleLineFeature.add(props[29]);
            //F37: 普通港澳台长途费（元）
            singleLineFeature.add(props[30]);
            //F38: 普通国际长途费（元）
            singleLineFeature.add(props[31]);
            //F39: IP国内长途费（元）
            singleLineFeature.add(props[32]);
            //F40: IP港澳台长途费（元）
            singleLineFeature.add(props[33]);
            //F41: IP国际长途费（元）
            singleLineFeature.add(props[34]);
            //F42: 话音增值业务收入（元）
            singleLineFeature.add(props[35]);
            //F43: 短信数据业务收入（元）
            singleLineFeature.add(props[36]);
            //F44: 非短信数据业务收入（元）
            singleLineFeature.add(props[37]);
            //F45: 其他数据业务收入（元）
            singleLineFeature.add(props[38]);
            //F46: 月租费（元）
            singleLineFeature.add(props[39]);
            //F47: 月租费-停机保号费（元）
            singleLineFeature.add(props[40]);
            //F48: 套餐月租费（元）
            singleLineFeature.add(props[41]);

            //客户价值等级 (都是"等级不详")
            // singleLineFeature.add(props[42]);

            //F49: 流量ARPU
            singleLineFeature.add(props[43]);
            //F50: 当月国际漫游费用（元）
            singleLineFeature.add(props[44]);
            //F51: 欠费月份数
            singleLineFeature.add(props[45]);
            //F52: 客户账户普通余额
            singleLineFeature.add(props[46]);
            //F53: 用户总积分
            singleLineFeature.add(props[47]);
            //F54: 预付费家庭计划
            if (props[48].equals("是")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //F55: 语音通话-时长（分钟）
            singleLineFeature.add(props[49]);
            //F56: 语音通话-次数
            singleLineFeature.add(props[50]);
            //F57: 当月被叫通话时长
            singleLineFeature.add(props[51]);
            //F58: 国内长途-次数
            singleLineFeature.add(props[52]);
            //F59: 港澳台长途-次数
            singleLineFeature.add(props[53]);
            //F60: 国际长途-次数
            singleLineFeature.add(props[54]);
            //F61: 港澳台漫游-次数
            singleLineFeature.add(props[55]);
            //F62: 省际漫游-次数
            singleLineFeature.add(props[56]);
            //F63: 国际漫游-次数
            singleLineFeature.add(props[57]);
            //F64: 省际漫游-去话-时长（分钟）
            singleLineFeature.add(props[58]);
            //F65: 当月套餐外主叫通话时长
            singleLineFeature.add(props[59]);
            //F66: 当月套餐外被叫通话时长
            singleLineFeature.add(props[60]);

            // props[61-69] remove 流量

            //F67: 当月ARPU
            singleLineFeature.add(props[70]);
            //F68: 当月MOU
            singleLineFeature.add(props[71]);
            //F69: 短信-点对点短信-条数
            singleLineFeature.add(props[72]);
            //F70: 彩信-国际彩信-条数
            singleLineFeature.add(props[73]);
            //F71: 用户通信对端手机号码数
            singleLineFeature.add(props[74]);
            //F72: 短信话费查询次数（6个月）
            singleLineFeature.add(props[75]);
            //F73: 10086IVR话费查询次数（6个月）
            singleLineFeature.add(props[76]);
            //网站话费查询次数（6个月）
            // singleLineFeature.add(props[77]);


            //=============================
            //F74: 用户当月用户余额是否小于0
            if (Double.parseDouble(props[46]) < 0){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }
            //F75:default 0
            singleLineFeature.add("0");
            //F76:default 0
            singleLineFeature.add("0");
            //F77:default 0.0
            singleLineFeature.add("0.0");
            //F78:default 0
            singleLineFeature.add(props[46]);

            //F79: 是否前三个月月均用户通信对端手机号码数大于4
            singleLineFeature.add(props[74]);
            //F80: 当月漫游通话与前4个月的平均值相比是否增加
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
        List<String> props3 = new ArrayList<>();
        List<String> props4 = new ArrayList<>();
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

            // 特征:用户通信对端手机号码数
            props3.add(props[74]);

            // 特征:漫游通话费
            props4.add(props[25]);
        }
        linesFeatureRecord.add(StringUtils.join(props1, ","));
        linesFeatureRecord.add(StringUtils.join(props2, ","));
        linesFeatureRecord.add(StringUtils.join(props3, ","));
        linesFeatureRecord.add(StringUtils.join(props4, ","));
        return StringUtils.join(linesFeatureRecord, ";");
    }
}
