import command.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Roger on 2016/6/30.
 */
public class MobileEntrance {

    public static void main(String[] args) {
        List<String> inputList = Arrays.asList(args);

        if (inputList.contains("-help")){
            System.out.println("please input command and params Sequentially, For example: -mode train -model linear -model_path /path/to -out true -files /path/201408.txt /path/201409.txt]");
            System.out.println("--1[mode]should be MODE train or predict");
            System.out.println("--2[model] should be MODEL linear,rmlp,cmlp,cnn or rnn");
            System.out.println("--3[model_path] should be the model path");
            System.out.println("--4[out] represent whether to use std out, 'true':stdOut;'false':logfile, default:true");
            System.out.println("--5[files] should be the files");
            System.exit(0);
        }

        if (!inputList.contains("-mode")){
            System.out.println("please input [1] command '-mode' with value: train or predict!");
            System.exit(0);
        }
        if (!inputList.contains("-model")){
            System.out.println("please input [2] command '-model' with value: linear,rmlp,cmlp,cnn or rnn");
            System.exit(0);
        }
        if (!inputList.contains("-model_path")){
            System.out.println("please input [3] command '-model_path' with the path to save model in mode 'train' or use model in mode 'predict'!");
            System.exit(0);
        }
        boolean isStdOut = true;
        if (inputList.contains("-out")){
            isStdOut = Boolean.valueOf(inputList.get(inputList.indexOf("-out") + 1));
        }
        String mode = inputList.get(inputList.indexOf("-mode") + 1);
        String model = inputList.get(inputList.indexOf("-model") + 1);
        String modelPath = inputList.get(inputList.indexOf("-model_path") + 1);
        List<String> months = new ArrayList<>();
        for (int i=inputList.indexOf("-files")+1; i<inputList.size(); i++){
            months.add(inputList.get(i));
        }

        if (mode.equals(Command.MODE_TRAIN)){
            if (model.equals(Command.MODEL_RNN)){
                Train train = new Train(model, months, modelPath);
                train.doTrain(isStdOut);
            }else {
                for (int i=0; i<months.size()-1; i++){
                    Train train = new Train(model, months.subList(i, i+2), modelPath);
                    train.doTrain(isStdOut);
                }
            }
        }else {
            Predict predict = new Predict(model, months, modelPath);
            predict.doPredict(isStdOut);
        }
    }
}
