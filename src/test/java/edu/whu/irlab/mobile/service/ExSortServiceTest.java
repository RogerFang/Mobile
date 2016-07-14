package edu.whu.irlab.mobile.service;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by Roger on 2016/7/14.
 */
public class ExSortServiceTest {

    @Test
    public void test() throws IOException {
        ExSortService exSortService = ExSortService.getInstance();
        File file1 = new File("E:\\data\\raw\\201412.txt");
        File sortedFile = exSortService.sort(file1, false);
        System.out.println(sortedFile.getAbsolutePath());
    }

}