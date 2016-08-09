package edu.whu.irlab.mobile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by Roger on 2016/6/29.
 */
public class PreprocessService {
    private static Logger logger = LoggerFactory.getLogger(PreprocessService.class);

    private static PreprocessService instance = new PreprocessService();

    private PreprocessService(){
    }

    public static PreprocessService getInstance(){
        return instance;
    }

    public File genData(List<File> bigDataFiles, boolean isTrain) throws IOException {
        return genData(bigDataFiles, isTrain, false);
    }

    /**
     * 传入原始的大数据文件, 生成初始特征文件
     *
     * @param bigDataFiles 大数据文件列表
     * @return 返回大数据文件集的交集记录
     */
    public File genData(List<File> bigDataFiles, boolean isTrain, boolean isClassification) throws IOException {
        /*PreprocessIBMService preprocessIBMService = PreprocessIBMService.getInstance();
        preprocessIBMService.setIsTrain(isTrain);
        preprocessIBMService.setIsClassification(isClassification);
        return preprocessIBMService.genData(bigDataFiles);*/

        /*PreprocessOriginService preprocessOriginService = PreprocessOriginService.getInstance();
        preprocessOriginService.setIsTrain(true);
        preprocessOriginService.setIsClassification(true);
        return preprocessOriginService.genData(bigDataFiles);*/

        /*PreprocessLeaveOneService preprocessLeaveOneService = PreprocessLeaveOneService.getInstance();
        preprocessLeaveOneService.setIsTrain(true);
        preprocessLeaveOneService.setIsClassification(true);
        return preprocessLeaveOneService.genData(bigDataFiles);*/

        /*PreprocessAllService preprocessAllService = PreprocessAllService.getInstance();
        preprocessAllService.setIsTrain(isTrain);
        preprocessAllService.setIsClassification(isClassification);
        return preprocessAllService.genData(bigDataFiles);*/

        /*PreprocessAdvService preprocessAdvService = PreprocessAdvService.getInstance();
        preprocessAdvService.setIsClassification(true);
        preprocessAdvService.setIsTrain(true);
        return preprocessAdvService.genData(bigDataFiles);*/

        // PreprocessAllPureFeatureService service = PreprocessAllPureFeatureService.getInstance();
        // PreprocessAllFeatureService service = PreprocessAllFeatureService.getInstance();
        PreprocessPartFeatureService service = PreprocessPartFeatureService.getInstance();
        service.setIsTrain(isTrain);
        service.setIsClassification(isClassification);
        return service.genData(bigDataFiles);
    }
}
