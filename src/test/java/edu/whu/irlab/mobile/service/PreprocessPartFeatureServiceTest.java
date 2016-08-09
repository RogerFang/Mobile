package edu.whu.irlab.mobile.service;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Roger on 2016/7/28.
 */
public class PreprocessPartFeatureServiceTest {
    @Test
    public void genData() throws Exception {
        PreprocessPartFeatureService service = PreprocessPartFeatureService.getInstance();
        List<File> fileList = new ArrayList<>();
        // fileList.add(new File("E:\\data\\raw\\201412.txt"));
        fileList.add(new File("E:\\data\\raw\\201501.txt"));
        fileList.add(new File("E:\\data\\raw\\201502.txt"));
        service.genData(fileList);
    }

}