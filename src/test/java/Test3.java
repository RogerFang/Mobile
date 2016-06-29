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
}
