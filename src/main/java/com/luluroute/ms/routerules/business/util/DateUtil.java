package com.luluroute.ms.routerules.business.util;

import com.luluroute.ms.routerules.business.exceptions.MappingFormatException;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import static com.logistics.luluroute.util.DateUtil.FROM_DATE_TIME_FORMAT;

@Slf4j
public class DateUtil {
	public static final String FROM_DATE_FORMAT = "yyyy-MM-dd";
	public static long getCurrentTime() {
		return Instant.now().toEpochMilli();
	}

	public static Date getCurrentUtcTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat ldf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Date d1 = null;
		try {
			d1 = ldf.parse(sdf.format(new Date()));
		} catch (java.text.ParseException e) {
			log.error("Error occured from getCurrentUtcTime  {}", "ParseException",
                    ExceptionUtils.getStackTrace(e));
		}
		return d1;
	}
	
	public static Date getCurrentUtcDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat ldf = new SimpleDateFormat("yyyyMMdd");
		Date d1 = null;
		try {
			d1 = ldf.parse(sdf.format(new Date()));
		} catch (java.text.ParseException e) {
			log.error("Error occured from getCurrentUtcTime  {}", "ParseException",
                    ExceptionUtils.getStackTrace(e));
		}
		return d1;
	}

	public static Date convertToDate(String value, String field) throws MappingFormatException {
		Date date = null;
		SimpleDateFormat df = new SimpleDateFormat(FROM_DATE_TIME_FORMAT);
		try {
			date = df.parse(value);
		} catch (Exception e) {
			throw new MappingFormatException(String.format(Constants.PARSER_ERROR_FORMAT, field, ExceptionUtils.getStackTrace(e)));
		}
		return date;
	}

	public static long currentDateTimeInLong() {
		return Instant.now().getEpochSecond();
	}
	
	public static long currentDateTimeInLong(String timeZone) {
		return StringUtils.isNotEmpty(timeZone) ? Instant.now().atZone(ZoneId.of(timeZone)).toEpochSecond()
				: currentDateTimeInLong();
	}

	public static long convertToEpoch(String dateString) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			return dateFormat.parse(dateString).toInstant().toEpochMilli()/ 1000; // Divide by 1000 to convert milliseconds to seconds
		} catch (ParseException e) {
			e.printStackTrace();
			return -1; // Return a default value or handle the error appropriately
		}
	}

	public static Date longToDate(long dateInLong) {
		Date date = new Date(dateInLong * 1000);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String formattedDate = sdf.format(date);

		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");

		try {
			return sdf2.parse(formattedDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Date parseStringToDate(String dateString) throws ParseException {
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd");

		return inputFormat.parse(dateString);
	}

	public static String epochToIso8601String(long epochTimeStamp) {
		LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochTimeStamp), ZoneOffset.UTC);
		return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
}

