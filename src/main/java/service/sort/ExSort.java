package service.sort;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Created by Roger on 2016/6/28.
 */
public class ExSort {

    // 单个子文件的记录数
    private static int SINGLE_FILE_COUNT = 10000 * 3;

    // 从大数据文件中, 一次缓冲读取的字节数(10M)
    private static int BUFFER_SIZE = 1024 * 1024 * 1;

    // 存储子文件的临时目录
    private static String TMP_DIR = "tmp";

    // 存储单行的字节数
    private static int LINE_SIZE = 2000;

    public ExSort(){
        File tmpFile = new File(TMP_DIR);
        if (!tmpFile.exists()){
            tmpFile.mkdirs();
        }
    }

    /**
     * 将大数据文件切分到几个小文件中
     */
    public List<File> splitFileData(File bigDataFile){
        // List<File> singleFiles = new ArrayList<>();
        try {
            FileChannel fcin = new RandomAccessFile(bigDataFile, "r").getChannel();
            return doSplitFileData(fcin);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将大数据文件的单个子文件进行排序
     */
    public void singleFileDataSort(File singleFile) throws IOException {
        System.out.println("INFO: single file data sort!");

        BufferedReader br = new BufferedReader(new FileReader(singleFile));

        Map<String, String> splitData = new TreeMap<>();

        String line = null;
        while ((line=br.readLine()) != null){
            if (StringUtils.isEmpty(line)){
                System.out.println(line);
                continue;
            }
            String tel = line.split(",")[0];
            if (tel.equals("手机号码")){
                System.out.println(line);
                continue;
            }
            splitData.put(tel, line);
        }
        br.close();

        BufferedWriter bw = new BufferedWriter(new FileWriter(singleFile));
        for (String str: splitData.values()){
            bw.write(str);
            bw.newLine();
        }
        bw.close();
    }

    /**
     * 多路归并
     * 将大数据文件的子文件进行合并，得到最终已排序的文件
     */
    public void multipleMerge(List<File> fileList) throws IOException {
        System.out.println("INFO: merge single files");
        int fileSize = fileList.size();

        if (fileSize == 1){
            return;
        }

        List<BufferedReader> brList = new ArrayList<>();
        for (int i=0; i<fileSize; i++){
            brList.add(new BufferedReader(new FileReader(fileList.get(i))));
        }

        // 合并文件writer流
        File mergeFile = getMergeFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(mergeFile));

        // 循环读取有序的子文件

    }

    private List<File> doSplitFileData(FileChannel fcin){
        List<File> splitFileList = new ArrayList<>();

        ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        // 读取的行数
        int lineCount = 0;
        File splitFile = getSplitFile();
        FileChannel fcout = getSplitFileChannel(splitFile);
        splitFileList.add(splitFile);

        String enterStr = "\n";
        try {
            byte[] bs = new byte[BUFFER_SIZE];
            //temp：由于是按固定字节读取，在一次读取中，第一行和最后一行经常是不完整的行，因此定义此变量来存储上次的最后一行和这次的第一行的内容，
            //并将之连接成完成的一行，否则会出现汉字被拆分成2个字节，并被提前转换成字符串而乱码的问题，数组大小应大于文件中最长一行的字节数
            byte[] temp = new byte[LINE_SIZE];
            while (fcin.read(rBuffer) != -1) {
                int rSize = rBuffer.position();
                rBuffer.rewind();
                rBuffer.get(bs);
                rBuffer.clear();

                //windows下ascii值13、10是换行和回车，unix下ascii值10是换行
                //从开头顺序遍历，找到第一个换行符
                int startNum=0;
                int length=0;
                for(int i=0;i<rSize;i++){
                    if(bs[i]==13){//找到换行字符
                        startNum=i;
                        for(int k=0;k<LINE_SIZE;k++){
                            if(temp[k]==0){//temp已经存储了上一次读取的最后一行，因此遍历找到空字符位置，继续存储此次的第一行内容，连接成完成一行
                                length=i+k;
                                for(int j=0;j<=i;j++){
                                    temp[k+j]=bs[j];
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
                //将拼凑出来的完整的一行转换成字符串
                String tempString1 = new String(temp, 0, length+1);
                //清空temp数组
                for(int i=0;i<temp.length;i++){
                    temp[i]=0;
                }
                //从末尾倒序遍历，找到第一个换行符
                int endNum=0;
                int k = 0;
                for(int i=rSize-1;i>=0;i--){
                    if(bs[i]==10){
                        endNum=i;//记录最后一个换行符的位置
                        for(int j=i+1;j<rSize;j++){
                            temp[k++]=bs[j];//将此次读取的最后一行的不完整字节存储在temp数组，用来跟下一次读取的第一行拼接成完成一行
                            bs[j]=0;
                        }
                        break;
                    }
                }
                //去掉第一行和最后一行不完整的，将中间所有完整的行转换成字符串
                String tempString2 = new String(bs, startNum+1, endNum-startNum);

                //拼接两个字符串
                String tempString = tempString1 + tempString2;

                int fromIndex = 0;
                int endIndex = 0;
                while ((endIndex = tempString.indexOf(enterStr, fromIndex)) != -1) {
                    String line = tempString.substring(fromIndex, endIndex) + enterStr;//按行截取字符串

                    lineCount++;
                    //写入文件
                    writeFileByLine(fcout, line);
                    fromIndex = endIndex + 1;

                    if (lineCount >= SINGLE_FILE_COUNT){
                        lineCount = 0;
                        splitFile = getSplitFile();
                        fcout = getSplitFileChannel(splitFile);
                        splitFileList.add(splitFile);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return splitFileList;
    }

    /**
     * 向写通道写出单行数据
     * @param fcout
     * @param line
     */
    private static void writeFileByLine(FileChannel fcout, String line) {
        try {
            ByteBuffer wBuffer = ByteBuffer.allocateDirect(LINE_SIZE);
            fcout.write(wBuffer.wrap(line.getBytes("UTF-8")), fcout.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取子文件
     * @return
     */
    private File getSplitFile(){
        String splitPath = TMP_DIR + File.separator + "split_file_"+ UUID.randomUUID() +".tmp";
        System.out.println(splitPath);
        File file = new File(splitPath);
        return file;
    }

    /**
     * 获取子文件的写通道
     * @return
     */
    private FileChannel getSplitFileChannel(File file){
        FileChannel fcout = null;
        try {
            fcout = new RandomAccessFile(file, "rws").getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fcout;
    }

    /**
     * 获取合并文件
     * @return
     */
    private File getMergeFile(){
        String mergePath = TMP_DIR + File.separator +"mergeFile_"+UUID.randomUUID()+".txt";
        System.out.println(mergePath);
        return new File(mergePath);
    }


    public static void main(String[] args) throws IOException {
        ExSort exSort = new ExSort();
        File bigDataFile = new File("201408.txt");
        List<File> singleFiles = exSort.splitFileData(bigDataFile);
        /*for (File file: singleFiles){
            exSort.singleFileDataSort(file);
        }*/
    }
}
