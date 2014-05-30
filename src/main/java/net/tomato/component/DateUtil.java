package net.tomato.component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author krisjin
 */
public class DateUtil {

	private DateUtil() {
		super();
	}

	public static String getCurrentTimeAsString() {
		return getTimeAsString(System.currentTimeMillis());
	}

	public static String getCurrentDateAsString() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.applyPattern("yyyyMMdd");
		return dateFormat.format(new Date(date.getTime()));
	}

	public static String getTimeAsString(long timeMillis, String newFormat) {
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.applyPattern(newFormat);
		return dateFormat.format(new Date(timeMillis));
	}

	public static String getTimeAsString(long timeMillis) {
		return getTimeAsString(timeMillis, "yyyyMMddHHmmss");
	}

	public static long getTimeMillis(String timeString) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat();
		dateFormat.applyPattern("yyyyMMddHHmmss");
		Date dt = dateFormat.parse(timeString);
		return dt.getTime();
	}

	public static String format(long timeMillis) {
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
		return dateFormat.format(new Date(timeMillis));
	}

	public static long getTimeMillis(int days, int hour, int minutes) {
		return days * 24 * 60 * 60 * 1000L + hour * 60 * 60 * 1000L + minutes * 60 * 1000L;
	}

	public static long getDays(long timeMillis) {
		return timeMillis / (24 * 60 * 60 * 1000L);
	}

	public static long getHours(long timeMillis) {
		return timeMillis / (60 * 60 * 1000L);
	}

	public static long getMinutes(long timeMillis) {
		return timeMillis / (60 * 1000L);
	}

	public static String getDHM(long timeMillis) {
		return getDHM(timeMillis, Locale.getDefault());
	}

	public static String getDHM(long timeMillis, Locale locale) {
		long hours = timeMillis % (24 * 60 * 60 * 1000L);
		long minutes = hours % ((60 * 60 * 1000L));
		String day = " day(s)";
		String hour = " hour(s)";
		String minute = " minute(s)";
		try {
			ResourceBundle DatetimeSymbols = ResourceBundle.getBundle("DatetimeSymbols", locale);
			day = DatetimeSymbols.getString("day");
			hour = DatetimeSymbols.getString("hour");
			minute = DatetimeSymbols.getString("minute");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return String.valueOf(getDays(timeMillis)) + day + "," + String.valueOf(getHours(hours)) + hour + "," + String.valueOf(getMinutes(minutes)) + minute;
	}

	public static int getHourOfDay(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(timeMillis));
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	public static int getMinute(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(timeMillis));
		return calendar.get(Calendar.MINUTE);
	}

	public static int getSecond(long timeMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(timeMillis));
		return calendar.get(Calendar.SECOND);
	}

	public static String formatDate(Date date, String newFormat) {
		if ((date == null) || (newFormat == null)) {
			return null;
		}
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(newFormat);
		return formatter.format(date);
	}
	
	
	public static void main(String[] args) {
		String d=DateUtil.format(System.currentTimeMillis());
		System.out.println(d);
	}
	
	
}
