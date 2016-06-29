package util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by Roger on 2016/6/29.
 */
public class FileUtil {

    // 存储临时文件的目录
    private static final String TMP_DIR = "tmp";
    // 预测特征数据根目录
    private static final String PREDICT_DIR = "predict";
    // 文件扩展名
    private static final String FILE_EXTENSION = ".txt";

    /**
     * 获取子文件
     * @return
     */
    public static File getSplitFile(){
        return getFileInTmpDir("split_file_");
    }

    /**
     * 获取合并文件
     * @return
     */
    public static File getMergeFile(){
        return getFileInTmpDir("merge_file_");
    }

    /**
     * 获取临时文件
     * @return
     */
    public static File getTmpFile(){
        return getFileInTmpDir("tmp_file_");
    }

    /**
     * 获取交集文件
     * @return
     */
    public static File getInterTmpFile(){
        return getFileInTmpDir("inter_tmp_file_");
    }

    /**
     * 获取预测特征索引文件
     * @param parentDir
     * @param index
     * @return
     */
    public static File getPredictSplitFile(String parentDir, int index){
        return new File(parentDir + File.separator + index + FILE_EXTENSION);
    }

    /**
     * 创建本次预测数据索引文件存放目录
     * @param dirName
     * @return
     */
    public static File makePredictDirForEveryTime(String dirName){
        File filePredict = new File(PREDICT_DIR + File.separator + dirName + File.separator);
        if (!filePredict.exists()){
            filePredict.mkdirs();
        }else {
            clearPath(filePredict);
        }

        return filePredict;
    }

    /**
     * 获取文件File
     * @param prefix 文件名前缀
     * @return
     */
    private static File getFileInTmpDir(String prefix){
        String filePath = TMP_DIR + File.separator +prefix+ UUID.randomUUID()+FILE_EXTENSION;
        System.out.println(filePath);
        File file = new File(filePath);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 清空目录下的文件
     * @param file
     */
    private static void clearPath(File file){
        for (File f: file.listFiles()){
            if (f.isFile()){
                f.delete();
            }
        }
    }
}
