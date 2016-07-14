package edu.whu.irlab.mobile.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Roger on 2016/5/19.
 */
public class CalendarUtil {

    private static DateFormat format = new SimpleDateFormat("yyyyMM");

    /**
     * 获取本月
     * @return eg:201605
     */
    public static String getCurMonth(){
        Calendar calender = Calendar.getInstance();
        calender.setTime(calender.getTime());
        return format.format(calender.getTime());
    }

    /**
     * 获取上月
     * @return eg: cur:201605, return:201604
     */
    public static String getLastMonth(){
        Calendar calender = Calendar.getInstance();
        calender.setTime(calender.getTime());
        calender.add(Calendar.MONTH, -1);
        return format.format(calender.getTime());
    }

    /**
     * 获取下月
     * @return eg: cur:201605, return:201606
     */
    public static String getNextMonth(){
        Calendar calender = Calendar.getInstance();
        calender.setTime(calender.getTime());
        calender.add(Calendar.MONTH, 1);
        return format.format(calender.getTime());
    }

    public static String getLastMonth(String month){
        Calendar calender = Calendar.getInstance();
        try {
            calender.setTime(format.parse(month));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calender.add(Calendar.MONTH, -1);
        return format.format(calender.getTime());
    }

    public static String getLastMonth(String month, int n){
        Calendar calender = Calendar.getInstance();
        try {
            calender.setTime(format.parse(month));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calender.add(Calendar.MONTH, -n);
        return format.format(calender.getTime());
    }

    /**
     * 根据输入月份获取下月
     * @param month eg:201605
     * @return eg:201606
     */
    public static String getNextMonth(String month){
        Calendar calender = Calendar.getInstance();
        try {
            calender.setTime(format.parse(month));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calender.add(Calendar.MONTH, 1);
        return format.format(calender.getTime());
    }

}
