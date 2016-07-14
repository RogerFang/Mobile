package edu.whu.irlab.mobile.service;

import edu.whu.irlab.mobile.util.CalendarUtil;
import edu.whu.irlab.mobile.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Roger on 2016/6/29.
 */
public class PreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessService.class);

    private static PreprocessService instance = new PreprocessService();

    private static ExSortService exSortService = ExSortService.getInstance();

    private boolean isTrain = true;
    private boolean isClassification = true;

    private PreprocessService(){
    }

    public static PreprocessService getInstance(){
        return instance;
    }

    /**
     * 传入原始的大数据文件, 生成初始特征文件
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
        for (int i=0; i<4; i++){
            String month = CalendarUtil.getLastMonth(firstMonth, i+1);
            File monthFile = FileUtil.getDataFile(month + fileExtension);
            if (!monthFile.exists()){
                logger.info("Data file for generating derived feature doesn't exist!");
                return null;
            }
            deriveDataFile.add(monthFile);
        }

        if (isTrain){
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

    private File mergeFeatureData(File initFile, File deriveFile) throws IOException {
        logger.info("Start merge feature data: initFile={}, deriveFile={}", initFile.getAbsolutePath(), deriveFile.getAbsolutePath());
        BufferedReader brInit = new BufferedReader(new FileReader(initFile));
        BufferedReader brDerive = new BufferedReader(new FileReader(deriveFile));

        File mergeFile = FileUtil.getInterTmpFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(mergeFile));
        String lineInit = brInit.readLine();
        String lineDerive = brDerive.readLine();

        while (lineInit != null){
            if (lineDerive == null){
                bw.write(lineInit);
                bw.newLine();
                lineInit = brInit.readLine();
            }else {
                String[] chunksInit = lineInit.split(";");
                String[] chunksDerive = lineDerive.split(";");
                if (chunksDerive[0].compareTo(chunksInit[0]) == 0){
                    int endIndex;
                    if (isTrain){
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
                    bw.write(lineInit);
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

    /**
     * 传入原始的大数据文件, 生成初始特征文件
     *
     * @param bigDataFiles 大数据文件列表
     * @return 返回大数据文件集的交集记录
     */
    public File genDataInit(List<File> bigDataFiles) throws IOException {
        logger.info("Start genDataInit: exsort and generate intersection");
        List<File> sortedFileList = exSortService.sortFlexible(bigDataFiles);
        File interFile = genInterData(sortedFileList, false);
        logger.info("End genDataInit!");
        return interFile;
    }

    /**
     * 传入原始的大数据文件, 处理得到部分特征用于计算衍生数据
     *
     * @param bigDataFiles 大数据文件列表
     * @return 返回大数据文件集的交集记录
     */
    public File genDataDerive(List<File> bigDataFiles, File initFile) throws IOException {
        logger.info("Start genDataDerive: exsort and generate intersection");
        List<File> sortedFileList = exSortService.sortFlexible(bigDataFiles);
        sortedFileList.add(initFile);
        File interFile = genInterData(sortedFileList, true);
        logger.info("Start genDataDerive!");
        return interFile;
    }

    /**
     * 根据已排序的大数据文件列表, 生成交集数据
     *
     * @param sortedFileList
     * @param isDerived 是否用于生成衍生特征数据
     * @return
     */
    private File genInterData(List<File> sortedFileList, boolean isDerived) throws IOException {
        logger.info("Start generating intersection from the sorted file list");
        int interCount = 0;

        int fileListSize = sortedFileList.size();

        List<BufferedReader> brList = new ArrayList<>();
        for (File sortedFile: sortedFileList){
            brList.add(new BufferedReader(new FileReader(sortedFile)));
        }

        // 是否有文件已经读完
        boolean notComplete = true;

        List<String> lines = new ArrayList<>();
        List<String> mobiles = new ArrayList<>();
        // 初始化
        for (int i=0; i<fileListSize; i++){
            String line = brList.get(i).readLine();
            if (line != null){
                if (StringUtils.isNotEmpty(line)){
                    lines.add(line);
                    mobiles.add(line.substring(0, 11));
                }
            }else {
                // 有任何文件已经读完后就表示求交集完成
                notComplete = false;
            }
        }

        File interFile = FileUtil.getInterTmpFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(interFile));

        while (notComplete){
            // 对lines, mobiles求交集
            List<Integer> smallerList = getMaxMobile(mobiles);
            // mobiles不等, 较小的索引对应的文件继续往下读
            if (smallerList.size() > 0){
                for (Integer index: smallerList){
                    String line = brList.get(index).readLine();

                    if (line != null){
                        if (StringUtils.isNotEmpty(line)){
                            lines.set(index, line);
                            mobiles.set(index, line.substring(0, 11));
                        }
                    }else {
                        // 有任何文件已经读完后就表示求交集完成
                        notComplete = false;
                    }
                }
            }else {
                // smallerList.size() == 0时, 表示mobiles都相等是交集, 写入交集文件
                String featureRecord = getFeatureRecord(mobiles.get(0), lines, isDerived);
                if (featureRecord != null){
                    bw.write(featureRecord);
                    bw.newLine();
                    interCount++;
                }

                // 写入交集文件后重新读取数据更新lines, mobiles
                for (int i=0; i<brList.size(); i++){
                    String line = brList.get(i).readLine();

                    if (line != null){
                        if (StringUtils.isNotEmpty(line)){
                            lines.set(i, line);
                            mobiles.set(i, line.substring(0, 11));
                        }
                    }else {
                        // 有任何文件已经读完后就表示求交集完成
                        notComplete = false;
                    }
                }
            }
        }


        if (bw != null){
            bw.close();
        }

        for (BufferedReader br: brList){
            if (br != null){
                br.close();
            }
        }

        logger.info("End generating intersection from the sorted file list, intersection file: {}", interFile.getAbsolutePath());
        return interFile;
    }

    private String getFeatureRecord(String mobile, List<String> lines, boolean isDerived){
        if (isDerived){
            return getFeatureRecordDerive(mobile, lines);
        }else {
            return getFeatureRecordInit(mobile, lines);
        }
    }

    /**
     * 将传入的交集lines转换为一条特征记录
     *
     * @param lines
     * @return
     */
    private String getFeatureRecordInit(String mobile, List<String> lines){
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

                // 销售渠道
                if (tmp.equals("代销商")){
                    tmp = "0";
                }else if (tmp.equals("零售店")){
                    tmp = "1";
                }else if (tmp.equals("营业厅")){
                    tmp = "2";
                }

                // 付款方式
                if (tmp.equals("现金")){
                    tmp = "1";
                }else if (tmp.equals("借记卡")){
                    tmp = "2";
                }else if (tmp.equals("银行托收")){
                    tmp = "3";
                }else if (tmp.equals("信用卡")){
                    tmp = "4";
                }else if (tmp.equals("支票")){
                    tmp = "5";
                }else if (tmp.equals("未知")){
                    tmp = "6";
                }else if (tmp.equals("级别不详")){
                    tmp = "0";
                }

                singleLineFeature.add(tmp);
            }

            // 衍生的特征: 是否为代销商
            if (props[15].equals("代销商")){
                singleLineFeature.add("1");
            }else {
                singleLineFeature.add("0");
            }

            // 衍生特征: 是否前三个月月均用户通信对端手机号码数大于4
            singleLineFeature.add(props[74]);
            //衍生特征: 当月漫游通话与前4个月的平均值相比是否增加
            singleLineFeature.add(props[25]);

            if (count == lines.size()-1){
                if (isTrain){
                    // train&test
                    if (isClassification){
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
            }

            count++;

        }
        return StringUtils.join(linesFeatureRecord, ";");
    }

    /**
     * 生成衍生特征
     *
     * @param mobile
     * @param lines
     * @return
     */
    private String getFeatureRecordDerive(String mobile, List<String> lines){
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

    /**
     * 返回较小的index索引值
     * @param mobiles
     * @return
     */
    private List<Integer> getMaxMobile(List<String> mobiles){
        // 降序
        Collections.sort(mobiles, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        List<Integer> smallerIds = new ArrayList<>();
        String max = mobiles.get(0);
        for (int i=0; i<mobiles.size(); i++){
            if (mobiles.get(i).compareTo(max) < 0){
                smallerIds.add(i);
            }
        }
        return smallerIds;
    }



    public void setIsTrain(boolean isTrain) {
        this.isTrain = isTrain;
    }

    public void setIsClassification(boolean isClassification) {
        this.isClassification = isClassification;
    }
}
