package edu.whu.irlab.mobile;

import edu.whu.irlab.mobile.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.whu.irlab.mobile.service.GenTrainService;
import edu.whu.irlab.mobile.util.FileUtil;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Roger on 2016/6/30.
 */
public class Train {
    private static Logger logger = LoggerFactory.getLogger(Train.class);

    private Command command = null;
    private List<String> months = null;
    private GenTrainService genTrainService = GenTrainService.getInstance();
    private boolean isClassification;

    public Train(String model, List<String> months, String modelPath) {
        this.months = months;
        command = new Command();
        command.setMode(Command.MODE_TRAIN);
        if (model != null){
            command.setModel(model);
        }
        if (modelPath != null){
            command.setModelPath(modelPath);
        }

        if (model.equals(Command.MODEL_RNN)){
            // 回归RNN至少需要三个月的数据
            if (months.size() < 3){
                throw new RuntimeException("the input months List size must be at least 3 in model " + model);
            }
            isClassification = false;
        }else if (model.equals(Command.MODEL_RMLP)){
            // 回归
            isClassification = false;
        }else if (model.equals(Command.MODEL_LINEAR) || model.equals(Command.MODEL_CMLP) || model.equals(Command.MODEL_CNN)){
            isClassification = true;
        }else {
            throw new RuntimeException("the input 'model' must be in: " + Command.MODEL_LINEAR + "," + Command.MODEL_CMLP + "," + Command.MODEL_CNN + "," + Command.MODEL_RMLP + "," +Command.MODEL_RNN);
        }

        if (months.size() != 2){
            throw new RuntimeException("the input months List size must be 2 in model " + model);
        }

        logger.info("edu.whu.irlab.mobile.Train: model={}, file list size={}, modelPath={}", model, months.size(), modelPath);
    }

    public Map<String, Object> doTrain(boolean isStdOut){
        Map<String, Object> rtnMap = new HashMap<>();

        if (isClassification){
            genTrainService.genClassification(months);
        }else {
            genTrainService.genRegression(months);
        }
        String trainDataPath = genTrainService.getTrainDirForThisTime();
        String testDataPath = genTrainService.getTestDirForThisTime();
        command.setTrainDataPath(trainDataPath);
        command.setTestDataPath(testDataPath);

        String cmd = command.getCommand();
        logger.info("predict edu.whu.irlab.mobile.command: {}", cmd);

        boolean isCompleted = false;
        String precision = null;

        try {
            BufferedWriter bw = null;
            if (!isStdOut){
                String[] splitsDir = trainDataPath.split(File.separator);
                File pyLogFile = FileUtil.getLogFile("py_train_error."+ splitsDir[splitsDir.length-1]);
                bw = new BufferedWriter(new FileWriter(pyLogFile));
            }

            Process pr = Runtime.getRuntime().exec(cmd);

            BufferedReader brInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String lineInput;
            while ((lineInput=brInput.readLine())!=null){
                if(lineInput.startsWith("test accuracy:")){
                    precision = lineInput.split(":")[1];
                    logger.info("Train Precision: {}", precision);
                }

                if (lineInput.trim().equals("training complete!")){
                    isCompleted = true;
                }

                outputInfo(lineInput.trim(), bw);
            }
            brInput.close();

            BufferedReader brError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String lineError;
            while ((lineError = brError.readLine()) != null) {
                outputInfo(lineError.trim(), bw);
            }
            brError.close();

            pr.waitFor();
            pr.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rtnMap.put("isCompleted", isCompleted);
        rtnMap.put("precision", precision);
        return rtnMap;
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
        files.add("201409.txt");

        edu.whu.irlab.mobile.Train train = new edu.whu.irlab.mobile.Train("rmlp", files, "/path/modelpath");
        train.doTrain(true);
    }*/
}
