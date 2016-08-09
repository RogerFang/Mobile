package edu.whu.irlab.mobile.service;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Roger on 2016/7/21.
 */
public class PreprocessOriginServiceTest {

    @Test
    public void test() throws IOException {
        PreprocessOriginService preprocessOriginService = PreprocessOriginService.getInstance();
        List<File> fileList = new ArrayList<>();
        fileList.add(new File("E:\\data\\raw\\201501.txt"));
        fileList.add(new File("E:\\data\\raw\\201502.txt"));
        preprocessOriginService.genData(fileList);
    }
}