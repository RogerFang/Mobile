import command.Command;
import service.GenTrainService;
import util.FileUtil;

import java.io.*;
import java.util.List;

/**
 * Created by Roger on 2016/6/30.
 */
public class Train {
    private Command command = null;
    private List<String> months = null;
    private GenTrainService genTrainService = GenTrainService.getInstance();

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
    }

    public void doTrain(boolean isStdOut){
        genTrainService.genMultiMonth(months, isStdOut);
        String trainDataPath = genTrainService.getTrainDirForThisTime();
        String testDataPath = genTrainService.getTestDirForThisTime();
        command.setTrainDataPath(trainDataPath);
        command.setTestDataPath(testDataPath);

        String cmd = command.getCommand();
        System.out.println("INFO: cmd=" + cmd);

        try {
            BufferedWriter bw = null;
            if (!isStdOut){
                String[] splitsDir = trainDataPath.split(File.separator);
                File pyLogFile = FileUtil.getLogFile("py_train."+ splitsDir[splitsDir.length-1]);
                bw = new BufferedWriter(new FileWriter(pyLogFile));
            }

            Process pr = Runtime.getRuntime().exec(cmd);

            BufferedReader brInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String lineInput;
            while ((lineInput=brInput.readLine())!=null){
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
    }

    private void outputInfo(String line, BufferedWriter bw) throws IOException {
        if (bw == null){
            System.out.println(line);
        }else {
            bw.write(line);
            bw.newLine();
        }
    }
}
