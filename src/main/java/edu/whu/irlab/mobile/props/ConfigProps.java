package edu.whu.irlab.mobile.props;

import java.io.*;
import java.util.Properties;

/**
 * Created by Roger on 2016/6/30.
 */
public class ConfigProps {

    private static ConfigProps instance = new ConfigProps();

    private Properties props = null;

    private ConfigProps(){
        props = new Properties();
        // jar包从外部读取配置文件
        InputStream in = null;
        try {
            in = new FileInputStream(new File("config.properties"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // 从classpath下读取配置文件
//        InputStream in = this.getClass().getResourceAsStream("/config_local.properties");
        try {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConfigProps getInstance(){
        return instance;
    }

    public String getProp(String key){
        return (String) this.props.get(key);
    }

    public Properties getProps() {
        return props;
    }
}
