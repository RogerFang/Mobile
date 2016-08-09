package edu.whu.irlab.mobile.service;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roger on 2016/7/28.
 */
public class PreprocessAllPureFeatureServiceTest {
    @Test
    public void genData() throws Exception {
        PreprocessAllPureFeatureService service = PreprocessAllPureFeatureService.getInstance();
        List<File> fileList = new ArrayList<>();
        // fileList.add(new File("E:\\data\\raw\\201412.txt"));
        fileList.add(new File("E:\\data\\raw\\201501.txt"));
        fileList.add(new File("E:\\data\\raw\\201502.txt"));
        service.genData(fileList);
    }

}