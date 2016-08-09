package edu.whu.irlab.mobile;

import edu.whu.irlab.mobile.command.Command;
import edu.whu.irlab.mobile.props.ConfigProps;
import edu.whu.irlab.mobile.service.PreprocessOriginService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Roger on 2016/6/30.
 */
public class MobileEntrance {
    private static ConfigProps configProps = ConfigProps.getInstance();

    public static void main(String[] args) {
        List<String> inputList = Arrays.asList(args);

        if (inputList.contains("-help")){
            System.out.println("please input edu.whu.irlab.mobile.command and params Sequentially, For example: -mode train -model linear -model_path /path/to -out true -files /path/201408.txt /path/201409.txt]");
            System.out.println("--1[mode]should be MODE train or predict");
            System.out.println("--2[model] should be MODEL linear,rmlp,cmlp,cnn or rnn");
            System.out.println("--3[model_path] should be the model path");
            System.out.println("--4[out] represent whether to use std out, 'true':stdOut;'false':logfile, default:true");
            System.out.println("--5[files] should be the files");
            System.exit(0);
        }

        if (!inputList.contains("-mode")){
            System.out.println("please input [1] edu.whu.irlab.mobile.command '-mode' with value: train or predict!");
            System.exit(0);
        }
        if (!inputList.contains("-model")){
            System.out.println("please input [2] edu.whu.irlab.mobile.command '-model' with value: linear,rmlp,cmlp,cnn or rnn");
            System.exit(0);
        }
        if (!inputList.contains("-model_path")){
            System.out.println("please input [3] edu.whu.irlab.mobile.command '-model_path' with the path to save model in mode 'train' or use model in mode 'predict'!");
            System.exit(0);
        }
        if (!inputList.contains("-files")){
            System.out.println("please input [4] edu.whu.irlab.mobile.command '-files' with the files path !");
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
            Map<String, Object> rtnMap = null;
            if (model.equals(Command.MODEL_RNN)){
                for (int i=0; i< (months.size()-Integer.valueOf(configProps.getProp("numSteps"))); i++){
                    Train train = new Train(model, months.subList(i, i+Integer.valueOf(configProps.getProp("numSteps"))+1), modelPath);
                    rtnMap = train.doTrain(isStdOut);
                }
            }else {
                for (int i=0; i<months.size()-1; i++){
                    Train train = new Train(model, months.subList(i, i+2), modelPath);
                    rtnMap = train.doTrain(isStdOut);
                }
            }
            for (Map.Entry<String, Object> entry: rtnMap.entrySet()){
                System.out.println("Train final: "+entry.getKey()+"\t"+entry.getValue());
            }
        }else {
            Predict predict = new Predict(model, months, modelPath);
            predict.doPredict(isStdOut);
        }
    }
}
