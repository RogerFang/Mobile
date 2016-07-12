package edu.whu.irlab.mobile;

import edu.whu.irlab.mobile.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.whu.irlab.mobile.service.GenPredictService;
import edu.whu.irlab.mobile.util.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roger on 2016/6/30.
 */
public class Predict {
    private static Logger logger = LoggerFactory.getLogger(Predict.class);

    private Command command = null;
    private List<String> months = null;
    private GenPredictService genPredictService = GenPredictService.getInstance();

    public Predict(String model, List<String> months, String modelPath) {
        logger.info("edu.whu.irlab.mobile.Predict: model={}, file list size={}, modelPath={}", model, months.size(), modelPath);
        this.months = months;
        command = new Command();
        command.setMode(Command.MODE_PREDICT);
        if (model != null){
            command.setModel(model);
        }
        if (modelPath != null){
            command.setModelPath(modelPath);
        }
    }

    public String doPredict(boolean isStdOut){
        String resultPath = "";

        genPredictService.genMultiMonth(months);
        String dataPath = genPredictService.getPredictDirForThisTime();
        command.setPredictDataPath(dataPath);

        String cmd = command.getCommand();
        logger.info("predict edu.whu.irlab.mobile.command: {}", cmd);
        System.out.println(dataPath + File.separator + "index.txt");
        try {
            // 读取预测索引文件
            BufferedReader brIndex = new BufferedReader(new FileReader(dataPath + File.separator +"index.txt"));
            List<String> splitDataPath = new ArrayList<>();
            System.out.println(brIndex.readLine());
            System.out.println(brIndex.readLine());
            String splitTmpLine;
            while ((splitTmpLine=brIndex.readLine())!=null){
                splitDataPath.add(splitTmpLine.trim().split(":")[1]);
            }
            brIndex.close();


            // 执行python 脚本,并获取输入流

            File predictTmpResultFile = FileUtil.getPredictResultTmpFile();
            Process pr = Runtime.getRuntime().exec(cmd);
            BufferedWriter bw = new BufferedWriter(new FileWriter(predictTmpResultFile));

            // 标准输出流 存到tmp.result
            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String lineResult;
            while ((lineResult=br.readLine())!=null){
                bw.write(lineResult);
                bw.newLine();
            }
            bw.close();
            br.close();

            // 错误输出流
            BufferedWriter bwError = null;
            if (!isStdOut){
                String[] splitsDir = dataPath.split(File.separator);
                File pyLogFile = FileUtil.getLogFile("py_predict_error."+ splitsDir[splitsDir.length-1]);
                bwError = new BufferedWriter(new FileWriter(pyLogFile));
            }
            BufferedReader brError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String lineError;
            while ((lineError=brError.readLine())!=null){
                outputInfo(lineError.trim(), bwError);
            }
            if (bwError!=null){
                bwError.close();
            }
            brError.close();

            pr.waitFor();
            pr.destroy();

            // 将预测结果对应的电话号码添加到result
            BufferedReader brTmp = new BufferedReader(new FileReader(predictTmpResultFile));
            String tmpResult;
            String predictResultFileName = new File(dataPath).getName()+"_"+command.getModel()+".result";
            File predictResultFile = FileUtil.getPredictResultFile(predictResultFileName);
            BufferedWriter bwFinal = new BufferedWriter(new FileWriter(predictResultFile));
            for (String path: splitDataPath){
                BufferedReader brSplit = new BufferedReader(new FileReader(dataPath+File.separator+path));
                String line;
                while ((line=brSplit.readLine())!=null){
                    String tel = line.trim().split(",")[0];
                    if ((tmpResult=brTmp.readLine())!=null){
                        bwFinal.write(tel+","+tmpResult.trim());
                        bwFinal.newLine();
                    }
                }
            }
            bwFinal.close();
            brTmp.close();
            resultPath = predictResultFile.getAbsolutePath();
            logger.info("predict result path: {}", predictResultFile.getAbsolutePath());
            // 删除predictTmpResultPath
            predictTmpResultFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resultPath;
    }

    private void outputInfo(String line, BufferedWriter bw) throws IOException {
        if (bw == null){
            System.out.println(line);
        }else {
            bw.write(line);
            bw.newLine();
        }
    }

    /*public static void main(String[] args) {
        List<String> files = new ArrayList<>();
        files.add("201408.txt");
//        files.add("201409.txt");
        edu.whu.irlab.mobile.Predict predict = new edu.whu.irlab.mobile.Predict("linear", files, "/path/model");
        predict.doPredict(true);
    }*/
}
