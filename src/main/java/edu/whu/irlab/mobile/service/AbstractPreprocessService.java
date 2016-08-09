package edu.whu.irlab.mobile.service;

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
 * Created by Roger on 2016/7/21.
 */
public abstract class AbstractPreprocessService {
    private static Logger logger = LoggerFactory.getLogger(AbstractPreprocessService.class);

    private static ExSortService exSortService = ExSortService.getInstance();

    private boolean isTrain = true;
    private boolean isClassification = true;

    public abstract File genData(List<File> bigDataFiles) throws IOException;

    public abstract File mergeFeatureData(File initFile, File deriveFile) throws IOException;

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
    abstract String getFeatureRecordInit(String mobile, List<String> lines);

    /**
     * 生成衍生特征
     *
     * @param mobile
     * @param lines
     * @return
     */
    abstract String getFeatureRecordDerive(String mobile, List<String> lines);

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

    public boolean isTrain() {
        return isTrain;
    }

    public boolean isClassification() {
        return isClassification;
    }
}
