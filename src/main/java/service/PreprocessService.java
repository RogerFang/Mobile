package service;

import org.apache.commons.lang3.StringUtils;
import util.FileUtil;

import java.io.*;
import java.util.*;

/**
 * Created by Roger on 2016/6/29.
 */
public class PreprocessService {

    private static PreprocessService instance = new PreprocessService();

    private boolean isTrain = true;
    private boolean isClassification = true;

    private PreprocessService(){
    }

    public static PreprocessService getInstance(){
        return instance;
    }

    /**
     * 传入原始的大数据文件
     *
     * @param bigDataFiles 大数据文件列表
     * @return 返回大数据文件集的交集记录
     */
    public File genData(List<File> bigDataFiles) throws IOException {
        List<File> sortedFileList = exSort(bigDataFiles);
        File interFile = genInterData(sortedFileList);
        System.out.println("inter file:" + interFile.getName());
        return interFile;
    }

    /**
     * 根据已排序的大数据文件列表, 生成交集数据
     * @param sortedFileList
     */
    private File genInterData(List<File> sortedFileList) throws IOException {
        System.out.println("INFO: gen inter data from sorted file list!");
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
                String featureRecord = getFeatureRecord(mobiles.get(0), lines);
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

        for (File file: sortedFileList){
            file.delete();
        }

        return interFile;
    }

    /**
     * 将传入的交集lines转换为一条特征记录
     *
     * @param lines
     * @return
     */
    private String getFeatureRecord(String mobile, List<String> lines){
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
        // System.out.println(linesFeatureRecord.size());
        return StringUtils.join(linesFeatureRecord, ",");
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

    /**
     * 返回排序后的大数据文件
     *
     * @param bigDataFile
     * @return
     */
    private File exSort(File bigDataFile){
        ExSortService exSortService = ExSortService.getInstance();
        try {
            return exSortService.sort(bigDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 针对传入的文件列表分别进行外部排序
     * @param bigDataFileList
     * @return
     */
    private List<File> exSort(List<File> bigDataFileList){
        List<File> sortedFileList = new ArrayList<>();
        for (File originFile: bigDataFileList){
            sortedFileList.add(exSort(originFile));
        }
        return sortedFileList;
    }

    public boolean isTrain() {
        return isTrain;
    }

    public void setIsTrain(boolean isTrain) {
        this.isTrain = isTrain;
    }

    public boolean isClassification() {
        return isClassification;
    }

    public void setIsClassification(boolean isClassification) {
        this.isClassification = isClassification;
    }

    /*public static void main(String[] args) throws IOException {
        PreprocessService preprocessService = new PreprocessService();
        List<File> fileList = new ArrayList<>();
        fileList.add(new File("201408.txt"));
        fileList.add(new File("201409.txt"));
        preprocessService.genData(fileList);
    }*/
}
