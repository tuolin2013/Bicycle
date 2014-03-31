package com.tuo.bicyclists.util.tools;

import android.annotation.SuppressLint;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class DatetimeTools {
	/**
	 * ����ʱ��֮�������������
	 * 
	 * @param one
	 *            ʱ����� 1��
	 * @param two
	 *            ʱ����� 2��
	 * @return �������
	 */
	public static long getDistanceDays(String str1, String str2) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date one;
		Date two;
		long days = 0;

		try {
			one = df.parse(str1);
			two = df.parse(str2);
			long time1 = one.getTime();
			long time2 = two.getTime();
			long diff = 0;
			if (time1 < time2) {
				diff = time2 - time1;
			} else {
				diff = time1 - time2;
			}
			days = diff / (1000 * 60 * 60 * 24);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return days;
	}

	public static long getDistance(String str1, String str2) {
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date one;
		Date two;
		long days = 0;
		long diff = 0;
		try {
			one = df.parse(str1);
			two = df.parse(str2);
			long time1 = one.getTime();
			long time2 = two.getTime();

			if (time1 < time2) {
				diff = time2 - time1;
			} else {
				diff = time1 - time2;
			}
			days = diff / (1000 * 60 * 60 * 24);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return diff;
	}

	public static boolean isBefore(String str1, String str2) {
		boolean flag = false;
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date one;
		Date two;
		try {
			one = df.parse(str1);
			two = df.parse(str2);
			long time1 = one.getTime();
			long time2 = two.getTime();

			if (time1 < time2) {
				flag = true;
			} else {
				flag = false;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return flag;

	}

	/**
	 * ����ʱ����������������Сʱ���ٷֶ�����
	 * 
	 * @param str1
	 *            ʱ����� 1 ��ʽ��1990-01-01 12:00:00
	 * @param str2
	 *            ʱ����� 2 ��ʽ��2009-01-01 12:00:00
	 * @return long[] ����ֵΪ��{��, ʱ, ��, ��}
	 */
	public static long[] getDistanceTimes(String str1, String str2) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date one;
		Date two;
		long day = 0;
		long hour = 0;
		long min = 0;
		long sec = 0;
		try {
			one = df.parse(str1);
			two = df.parse(str2);
			long time1 = one.getTime();
			long time2 = two.getTime();
			long diff;
			if (time1 < time2) {
				diff = time2 - time1;
			} else {
				diff = time1 - time2;
			}
			day = diff / (24 * 60 * 60 * 1000);
			hour = (diff / (60 * 60 * 1000) - day * 24);
			min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
			sec = (diff / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
		} catch (Exception e) {
			e.printStackTrace();
		}
		long[] times = { day, hour, min, sec };
		return times;
	}

	/**
	 * ����ʱ����������������Сʱ���ٷֶ�����
	 * 
	 */
	public static JSONObject getDistanceTime(long time1, long time2) {

		long day = 0;
		long hour = 0;
		long min = 0;
		long sec = 0;

		long diff;
		if (time1 < time2) {
			diff = time2 - time1;
		} else {
			diff = time1 - time2;
		}
		day = diff / (24 * 60 * 60 * 1000);
		hour = (diff / (60 * 60 * 1000) - day * 24);
		min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
		sec = (diff / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);

		JSONObject jsObject = new JSONObject();
		try {
			jsObject.put("day", day);
			jsObject.put("hour", hour);
			jsObject.put("min", min);
			jsObject.put("sec", sec);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsObject;
	}

}
