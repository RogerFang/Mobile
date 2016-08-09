package edu.whu.irlab.mobile.service;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Roger on 2016/7/21.
 */
public class PreprocessIBMServiceTest {

    @Test
    public void genData() throws Exception {
        PreprocessIBMService preprocessIBMService = PreprocessIBMService.getInstance();
        List<File> fileList = new ArrayList<>();
        fileList.add(new File("E:\\data\\raw\\201501.txt"));
        fileList.add(new File("E:\\data\\raw\\201502.txt"));

        preprocessIBMService.genData(fileList);
    }
}