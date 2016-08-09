package edu.whu.irlab.mobile.service;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Roger on 2016/7/28.
 */
public class PreprocessAllPureFeatureService extends AbstractPreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessAllPureFeatureService.class);

    private static PreprocessAllPureFeatureService instance = new PreprocessAllPureFeatureService();

    private PreprocessAllPureFeatureService(){}

    public static PreprocessAllPureFeatureService getInstance(){
        return instance;
    }

    /**
     * 73个特征: 无依赖数据
     * @param bigDataFiles
     * @return
     * @throws IOException
     */
    @Override
    public File genData(List<File> bigDataFiles) throws IOException {
        logger.info("Start preprocessing: exsort and generate intersection");

        File initFile = genDataInit(bigDataFiles);

        logger.info("End preprocessing: exsort and generate intersection");
        return initFile;
    }

    @Override
    public File mergeFeatureData(File initFile, File deriveFile) throws IOException {
        return null;
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
                    linesFeatureRecord.add(Joiner.on(",").skipNulls().join(singleLineFeature));
                }
            }else {
                linesFeatureRecord.add(Joiner.on(",").skipNulls().join(singleLineFeature));
            }

            count++;
        }
        return Joiner.on(",").skipNulls().join(linesFeatureRecord);
    }

    @Override
    String getFeatureRecordDerive(String mobile, List<String> lines) {
        return null;
    }
}
