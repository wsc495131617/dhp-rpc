package org.dhp.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 功能说明: 日期工具<br>
 * 注意事项: 类似其他相关功能，可参考 org.apache.commons.lang.time 包下面的DateUtils、DateFormatUtils、FastDateFormat<br>
 * 系统版本: v1.0<br>
 * 开发人员: <br>
 * 开发时间: <br>
 */
public class DateUtils {

    /** 模式 :yyyyMMddHHmmss */
    private static final String YYYYMMDD_HHMMSS = "yyyyMMddHHmmss";
    /** 模式 :yyyyMMdd */
    private static final String YYYYMMDD = "yyyyMMdd";
    /** */
    private static final String YYYYMM = "yyyyMM";
    
    /** 模式 :HHmmss */
    private static final String HHMMSS = "HHmmss";

    /** 秒与毫秒转化的单位 */
    public static final int SECOND_UNIT = 1000;

    static String CYCLE_TYPE_YEAR = "1";// 年
    static String CYCLE_TYPE_HFYEAR = "2";// 半年
    static String CYCLE_TYPE_QUARTET = "3";// 季度
    static String CYCLE_TYPE_MONTH = "4";// 月
    static String CYCLE_TYPE_WEEK = "5";// 周

    /**
     * 方法说明：Integer类型转换为日期类型.
     * 
     * @param date 14为整型
     * @return
     */
    public static Date Integer2Date(Integer date) {
        return null == date ? null : string2Date(date.toString());
    }

    /**
     * 方法说明：字符串转日期类型.
     * 
     * @param date
     * @return
     */
    public static Date string2Date(String date) {
        try {
            if (date.length() != 14) {
                return getDateFormat().parse(date);
            } else {
                return getDateFormat(YYYYMMDD).parse(date);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parse(String date, String formatText) {
        try {
            return getDateFormat(formatText).parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 20190101 + 010101 转 Date对象
     * 
     * @param date
     * @param time
     * @return
     */
    public static Date int2Date(int date, int time) {
        String dateStr = date + "" + StringUtils.leftPad(time + "", 6, "0");
        try {
            return getDateFormat(YYYYMMDD_HHMMSS).parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：日期类型转成yyyyMMdd格式字符串.
     * 
     * @param date
     * @return
     */
    public static String date2String(Date date) {
        return date2String(date, YYYYMMDD);
    }

    /**
     * 方法说明：日期类型按照指定格式转成字符串.
     * 
     * @param date
     * @return
     */
    public static String date2String(Date date, String pattern) {
        if (null == date) {
            date = new Date();
        }
        try {
            return getDateFormat(pattern).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：将日期类型转为YYYYMMDD的Integer类型
     * 
     * @param date 如果为null,则取当前服务器日期.
     * @return
     */
    public static Integer date2Integer(Date date) {
        if (null == date) {
            date = new Date();
        }
        try {
            return Integer.valueOf(getDateFormat(YYYYMMDD).format(date));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：将日期类型转为Integer类型
     * 
     * @param date 如果为null,则取当前服务器日期.
     * @return
     */
    public static Integer date2IntegerYyyymmdd(Date date) {
        if (null == date) {
            date = new Date();
        }
        try {
            return Integer.valueOf(getDateFormat(YYYYMMDD).format(date));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：将时间格式化.
     * 
     * @param time 如果为null,则取当前服务器日期.
     * @return @
     */
    public static Integer timeFormat(Long time) {
        if (null == time) {
            time = new Date().getTime();
        }
        try {
            return Integer.valueOf(getDateFormat(HHMMSS).format(time));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：输入符合要求的日期型长整数，将其转化成Calendar.
     * 
     * @param dateInteger 日期格式 yyyyMMdd
     * @return @
     */
    public static Calendar IntegerToCalendar(Integer dateInteger) {
        if (null == dateInteger) {
            return null;
        }
        try {
            Date date = getDateFormat().parse(dateInteger.toString());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：输入Calendar,转化为符合要求的日期型长整数:yyyyMMdd.
     * 
     * @param calendarDate 如果为null,则取当前服务器日期.
     * @return @
     */
    public static Integer calendarToInteger(Calendar calendarDate) {
        if (calendarDate == null) {
            calendarDate = Calendar.getInstance();
        }
        try {
            return Integer.valueOf(getDateFormat().format(calendarDate.getTime()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 方法说明：获取指定模式pattern的SimpleDateFormat对象.
     * 
     * @param pattern
     * @return
     */
    public static SimpleDateFormat getDateFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    /**
     * 方法说明：获取默认模式"yyyyMMdd"的SimpleDateFormat对象.
     * 
     * @return
     */
    private static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(YYYYMMDD);
    }

    /**
     * 以date为基准获取n天后的日期，
     * 
     * @param date Integer型日期
     * @param n -1表示date的前一天，1表示date的后一天。
     * @return Integer型日期 @ 业务异常
     */
    public static Integer getNextDate(Integer date, int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(Integer2Date(date));
        int newDay = c.get(Calendar.DAY_OF_MONTH) + n;
        c.set(Calendar.DAY_OF_MONTH, newDay);
        return date2Integer(c.getTime());
    }

    /**
     * 以date为基准获取n天后的日期，
     * 
     * @param date Integer型日期
     * @param n -1表示date的前一天，1表示date的后一天。
     * @return Integer型日期 @ 业务异常
     */
    public static Integer getNextDateYyyymmdd(Integer date, int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(Integer2Date(date));
        int newDay = c.get(Calendar.DAY_OF_MONTH) + n;
        c.set(Calendar.DAY_OF_MONTH, newDay);
        return date2IntegerYyyymmdd(c.getTime());
    }

    /**
     * 得到两个日期的间隔天数
     * 
     * @param begindate 起始日期
     * @param enddate 结束日期
     * @return 间隔天数 @ 统一业务异常
     */
    public static Long getDays(Integer begindate, Integer enddate) {
        Calendar begin = IntegerToCalendar(begindate);
        Calendar end = IntegerToCalendar(enddate);
        return diffDays(end.getTime(), begin.getTime());
    }

    /**
     * 计算两个日期相差天数。 用第一个日期减去第二个。如果前一个日期小于后一个日期，则返回负数
     * 
     * @param one 第一个日期数，作为基准
     * @param two 第二个日期数，作为比较
     * @return 两个日期相差天数
     */
    public static Long diffDays(Date one, Date two) {
        return (one.getTime() - two.getTime()) / (24 * 60 * 60 * 1000);
    }

    /**
     * 获取两个日期之间的相差天数
     * 
     * @param beginDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public static Long getDateDiff(Integer beginDate, Integer endDate) {
        Date dateEnd = null; // 定义时间类型
        Date dateBegin = null; // 定义时间类型
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd");
        try {
            dateEnd = inputFormat.parse(endDate.toString()); // 将字符型转换成日期型
            dateBegin = inputFormat.parse(beginDate.toString()); // 将字符型转换成日期型
            return (Long) (dateEnd.getTime() - dateBegin.getTime()) / 1000 / 3600 / 24; // 返回天数
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据周期类型确定当前周期的起始、截止日期
     * 
     * @param cycleType
     * @param now
     * @return @
     */
    public static Integer[] genBeginEndDate(String cycleType, Integer now) {
        Integer year = now / 10000;
        Integer month = now % 10000 / 100;
        Integer[] result = new Integer[2];
        // 如果是年,当年的1.1 - 12.31
        if (CYCLE_TYPE_YEAR.equals(cycleType)) {
            result[0] = year * 10000 + 101;
            result[1] = year * 10000 + 1231;
        }
        // 半年，判断是上半年或下半年
        else if (CYCLE_TYPE_HFYEAR.equals(cycleType)) {
            if (month < 6) {
                result[0] = year * 10000 + 101;
                result[1] = year * 10000 + 531;
            } else {
                result[0] = year * 10000 + 601;
                result[1] = year * 10000 + 1231;
            }
        }
        // 季度
        else if (CYCLE_TYPE_QUARTET.equals(cycleType)) {
            result[0] = year * 10000 + (month / 4 == 0 ? 1 : month / 4 * 3 + 1) * 100 + 1;
            if (month == 12l) {
                result[1] = year * 10000 + 1231;
            } else {
                Integer d = 1;
                if (month >= 1 && month <= 3) {
                    d = 4;
                } else if (month >= 4 && month <= 6) {
                    d = 7;
                } else if (month >= 7 && month <= 9) {
                    d = 10;
                } else {
                    d = 1;
                    year++;
                }
                Integer nextDate = year * 10000 + d * 100 + 1; // 下一个周期的第一天
                result[1] = getNextDateYyyymmdd(nextDate, -1); // 下一个周期的第一天的前一天，即为当前周期的最后一天
            }
        }
        // 月
        else if (CYCLE_TYPE_MONTH.equals(cycleType)) {
            result[0] = year * 10000 + month * 100 + 1;
            Integer nextDate = year * 10000 + (month + 1) * 100 + 1; // 下一个周期的第一天
            result[1] = getNextDateYyyymmdd(nextDate, -1); // 下一个周期的第一天的前一天，即为当前周期的最后一天
        }
        // 周
        else if (CYCLE_TYPE_WEEK.equals(cycleType)) {
            Calendar calendar = IntegerToCalendar(now);
            int day_of_week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            calendar.add(Calendar.DATE, -day_of_week);
            result[0] = date2IntegerYyyymmdd(calendar.getTime());
            calendar.add(Calendar.DATE, 6);
            result[1] = date2IntegerYyyymmdd(calendar.getTime());
        }
        return result;
    }

    /**
     * 获取Integer型日期 YYYYMMDD
     * 
     * @param date
     * @return
     */
    public static Integer getIntegerDateTime(Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmmss");
        return Integer.valueOf(sd.format(date));
    }

    /**
     * 根据Date返回格式如20120901的Integer型日期数值
     * 
     * @param date 日期
     * @return Integer型日期
     */
    public static Integer getIntegerDate(Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
        return Integer.valueOf(sd.format(date));
    }

    /**
     * 根据Date返回格式如120801的Integer型时间数值，表示12点零8分1秒 40411表示4点04分11秒
     * 
     * @param date 日期
     * @return Integer型时间
     */
    public static Integer getIntegerTime(Date date) {
        SimpleDateFormat sd = new SimpleDateFormat("HHmmss");
        return Integer.valueOf(sd.format(date));
    }

    /**
     * 指定时间、偏移量(以秒为单位)，获取偏移后的date
     * 
     * @param date 指定时间
     * @param offset 偏移秒数，负数表示向前，正数表示向后。
     * @return 偏移后的时间。
     */
    public static Date getOffsetDate(Date date, Integer offset) {
        Long baseTime = date.getTime();
        Long targetTime = baseTime + offset * SECOND_UNIT;
        Date resultDate = new Date();
        resultDate.setTime(targetTime);
        return resultDate;
    }

    /**
     * 获取当前日期时间
     * 
     * @param format
     * @return
     */
    public static String getDateTime(String format) {
        Date dt = new Date();
        return formatDate(dt, format);
    }

    /**
     * 格式化日期时间为指定格式的字符串
     * 
     * @param format
     * @return
     */
    public static String formatDate(Date dt, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(dt);
    }

    /**
     * 判断日期是否为周末
     * 
     * @param date 日期
     * @return true是周末
     */
    public static boolean isWeekend(Integer date) {
        Calendar c = Calendar.getInstance();
        c.setTime(DateUtils.Integer2Date(date));
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
            return true;
        }
        return false;
    }

    /**
     * 获取日期是一周的第几天
     * 
     * @param date 日期
     * @return 周日是第一天，周六是第七天
     */
    public static Integer dayOfWeek(Integer date) {
        Calendar c = Calendar.getInstance();
        c.setTime(DateUtils.Integer2Date(date));
        return c.get(Calendar.DAY_OF_WEEK);
    }

    /**
     * 获取日期是一月的第几天
     * 
     * @param date 日期
     * @return
     */
    public static Integer dayOfMonth(Integer date) {
        Calendar c = Calendar.getInstance();
        c.setTime(DateUtils.Integer2Date(date));
        return c.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取日期是一年的第几天
     * 
     * @param date 日期
     * @return
     */
    public static Integer dayOfYear(Integer date) {
        Calendar c = Calendar.getInstance();
        c.setTime(DateUtils.Integer2Date(date));
        return c.get(Calendar.DAY_OF_YEAR);
    }

    public static Integer getNowDate() {
        return getIntegerDate(new Date());
    }

    public static Integer getNowTime() {
        return getIntegerTime(new Date());
    }
    
    public static Date getCurrentMonthBeginDay() {
        Calendar calendar = Calendar.getInstance();
        // 设置为第一天
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getCurrentYearBeginDay() {
        return getCurrentYearBeginDay(TimeZone.getDefault());
    }

    public static Date getCurrentYearBeginDay(TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        // 设置为第一天
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getCurrentMonthEndDay() {
        // 获取Calendar
        Calendar calendar = Calendar.getInstance();
        // 设置日期为本月最大日期
        calendar.set(Calendar.DATE, calendar.getActualMaximum(calendar.DATE));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    public static Date getLastMonthBeginDay() {
        Calendar calendar = Calendar.getInstance();
        // 设置为第一天
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getLastMonthEndDay() {
        // 获取Calendar
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        // 设置日期为本月最大日期
        calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    /**
     * 返回 YYYYMM
     * @return
     */
    public static Integer getLastMonthInteger() {
        Date lastMonthEndDay = getLastMonthEndDay();
        return Integer.valueOf(formatDate(lastMonthEndDay, YYYYMM));
    }
    
    /**
     * 返回 YYYYMM
     * @return
     */
    public static Integer getCurrentMonthInteger() {
        Date lastMonthEndDay = getCurrentMonthEndDay();
        return Integer.valueOf(formatDate(lastMonthEndDay, YYYYMM));
    }
    
    /**
     * 返回上一日
     * @return
     */
    public static Date getLastDate() {
        Calendar calendar = Calendar.getInstance();
        // 设置为第一天
        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获取当天日期
     * @return
     */
    public static Date getTodayDate() {
        Calendar calendar = Calendar.getInstance();
        // 设置为第一天
//        calendar.add(Calendar.DATE, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    /**
     * 返回 YYYYMMDD
     * @return
     */
    public static Integer getLastDateInteger() {
        return Integer.valueOf(formatDate(getLastDate(), YYYYMMDD));
    }



    public static void main(String[] args) {
        System.out.println(formatDate(getTodayDate(),YYYYMMDD));
    }

}
