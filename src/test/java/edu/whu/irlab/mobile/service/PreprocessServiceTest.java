package edu.whu.irlab.mobile.service;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Roger on 2016/7/14.
 */
public class PreprocessServiceTest {
    @Test
    public void test() throws IOException {
        PreprocessService preprocessService = PreprocessService.getInstance();
        preprocessService.setIsTrain(false);
        List<File> fileList = new ArrayList<>();
        // fileList.add(new File("E:\\data\\raw\\201412.txt"));
        // fileList.add(new File("E:\\data\\raw\\201501.txt"));
        fileList.add(new File("E:\\data\\raw\\201502.txt"));
        // fileList.add(new File("E:\\data\\feature\\tmp\\inter_tmp_file_00607e8a-0211-43ec-9eca-208efaabe377.txt"));
        // preprocessService.genData(fileList);
        // preprocessService.genDataInit(fileList);
        // preprocessService.genDataDerive(fileList);

        preprocessService.genData(fileList);
    }

}