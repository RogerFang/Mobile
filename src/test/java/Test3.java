import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * Created by Roger on 2016/6/29.
 */
public class Test3 {

    @Test
    public void test(){
        String s = "13401017813,在网-开通,神州行品牌";
        System.out.println(s.matches("^\\d{11}.*"));
    }

    @Test
    public void test1(){
        String s = "13401017813,在网-开通,神州行品牌";
        System.out.println(s.substring(0, 11));
    }

    @Test
    public void test2(){
        String s1 = "";
        String s2 = null;
        System.out.println(StringUtils.isNotEmpty(s1));
        System.out.println(StringUtils.isNotBlank(s1));
        System.out.println(StringUtils.isNotEmpty(s2));
        System.out.println(StringUtils.isNotBlank(s2));
    }

    @Test
    public void test3(){
        String s = "13401000928,2014-09,在网-开通,动感地带品牌,04-动感地带,2006-03-19,0,103,否,是,否,否,1.000,0.000,0.391,零售店,现金,1,100.00,0.00,0.00,0.00,0.00,53.93,15.24,0.00,0.00,0.00,0.00,2.37,0.00,0.00,0.00,0.00,0.00,0.00,1.00,10.32,0.00,0.00,0.00,1.00,级别不详,20.03,0.00,0,227.07,1447,否,306,135,220,2,0,0,0,0,0,0,86,0,2012-12-17,132913,680,0,91733,,0,132913,0,53.93,306,47,0,38,0,0,0";
        String[] props = s.split(",");
        for (int i=0; i<props.length; i++){
            System.out.println(i+"\t"+props[i]);
        }
    }
}
