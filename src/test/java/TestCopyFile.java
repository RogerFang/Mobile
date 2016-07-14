import org.junit.Test;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * Created by Roger on 2016/7/14.
 */
public class TestCopyFile {

    @Test
    public void test() throws IOException {
        File tmpFile = new File("E:\\1.txt");
        File mergeFile = new File("E:\\data\\2.txt");

        // mergeFile.delete();
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(tmpFile).getChannel();
            outputChannel = new FileOutputStream(mergeFile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }
}
