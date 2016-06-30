package command;

import props.ConfigProps;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roger on 2016/5/12.
 */
public class Command {
    private static ConfigProps configProps = ConfigProps.getInstance();

    private static final String PY_INTERFACE =  configProps.getProp("PY_INTERFACE");

    public static final String MODE_TRAIN = configProps.getProp("MODE_TRAIN");
    public static final String MODE_PREDICT = configProps.getProp("MODE_PREDICT");

    public static final String MODEL_LINEAR = configProps.getProp("MODEL_LINEAR");
    public static final String MODEL_RMLP = configProps.getProp("MODEL_RMLP");
    public static final String MODEL_CMLP = configProps.getProp("MODEL_CMLP");
    public static final String MODEL_CNN = configProps.getProp("MODEL_CNN");
    public static final String MODEL_RNN = configProps.getProp("MODEL_RNN");

    // 模式:train or predict(必须)
    private String mode = null;
    // 模型:linear,rmlp,cmlp,cnn,rnn; default:linear
    private String model = configProps.getProp("model");

    //=>> 数据地址
    // 训练数据位置(train模式下必须)
    private String trainDataPath = null;
    // 测试数据位置(train模式下必须)
    private String testDataPath = null;
    // 预测数据位置(predict模式下必须)
    private String predictDataPath = null;

    //=>> 系统运行参数
    // 训练轮数, defalut:200
    private String trainingEpochs = configProps.getProp("trainingEpochs");
    // batch size, default:200
    private String batchSize = configProps.getProp("batchSize");
    // 展示间隔display step, default:10
    private String displayStep = configProps.getProp("displayStep");
    // 保存模型间隔save step, default:50
    private String saveStep = configProps.getProp("saveStep");
    // 学习率learningRate, default:0.01
    private String learningRate = configProps.getProp("learningRate");
    // rnn中的月数, default:3
    private String numSteps = configProps.getProp("numSteps");
    // 特征数量, default:71
    private String featureSize = configProps.getProp("featureSize");
    // 类别数量, default:2
    private String outputSize = configProps.getProp("outputSize");
    // CNN model必须
    private String perm = configProps.getProp("perm");

    // 模型存放地址(train模式必须)
    private String modelPath = null;

    public Command(){
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getTrainDataPath() {
        return trainDataPath;
    }

    public void setTrainDataPath(String trainDataPath) {
        this.trainDataPath = trainDataPath;
    }

    public String getTestDataPath() {
        return testDataPath;
    }

    public void setTestDataPath(String testDataPath) {
        this.testDataPath = testDataPath;
    }

    public String getPredictDataPath() {
        return predictDataPath;
    }

    public void setPredictDataPath(String predictDataPath) {
        this.predictDataPath = predictDataPath;
    }

    public String getTrainingEpochs() {
        return trainingEpochs;
    }

    public void setTrainingEpochs(String trainingEpochs) {
        this.trainingEpochs = trainingEpochs;
    }

    public String getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public String getDisplayStep() {
        return displayStep;
    }

    public void setDisplayStep(String displayStep) {
        this.displayStep = displayStep;
    }

    public String getSaveStep() {
        return saveStep;
    }

    public void setSaveStep(String saveStep) {
        this.saveStep = saveStep;
    }

    public String getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(String learningRate) {
        this.learningRate = learningRate;
    }

    public String getNumSteps() {
        return numSteps;
    }

    public void setNumSteps(String numSteps) {
        this.numSteps = numSteps;
    }

    public String getFeatureSize() {
        return featureSize;
    }

    public void setFeatureSize(String featureSize) {
        this.featureSize = featureSize;
    }

    public String getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(String outputSize) {
        this.outputSize = outputSize;
    }

    public String getPerm() {
        return perm;
    }

    public void setPerm(String perm) {
        this.perm = perm;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    private String constructOptions(){
        Map<String, String> options = new HashMap<String, String>();
        options.put("--mode", getMode());
        options.put("--model", getModel());
        options.put("--train_data", getTrainDataPath());
        options.put("--test_data", getTestDataPath());
        options.put("--predict_data", getPredictDataPath());
        options.put("--training_epochs", getTrainingEpochs());
        options.put("--batch_size", getBatchSize());
        options.put("--display_step", getDisplayStep());
        options.put("--save_step", getSaveStep());
        options.put("--learning_rate", getLearningRate());
        options.put("--num_steps", getNumSteps());
        options.put("--feature_size", getFeatureSize());
        options.put("--output_size", getOutputSize());
        options.put("--perm", getPerm());
        options.put("--model_path", getModelPath());

        String rtnOptions = "";
        for (Map.Entry<String, String> entry: options.entrySet()){
            rtnOptions += " " + entry.getKey() + " " + entry.getValue();
        }

        return rtnOptions;
    }

    public String getCommand(){
        return this.toString();
    }

    @Override
    public String toString() {
        return "python -u "+ PY_INTERFACE +constructOptions();
    }
}
