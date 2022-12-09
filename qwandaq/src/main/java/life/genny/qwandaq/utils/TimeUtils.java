package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;

import org.jboss.logging.Logger;

/**
 * A utility class for date and time related operations.
 * 
 * @author Jasper Robison
 */
public class TimeUtils {

	@Inject
	private static Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	/** 
	 * Format a LocalTime object to a string
	 *
	 * @param time the time to be formatted
	 * @param format the format to use
	 * @return String
	 */
	public static String formatTime(LocalTime time, String format) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return time.format(formatter);
	}

	/** 
	 * Format a LocalDate object to a string
	 *
	 * @param date the date to be formatted
	 * @param format the format to use
	 * @return String
	 */
	public static String formatDate(LocalDate date, String format) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return date.format(formatter);
	}

	/** 
	 * Format a LocalDateTime object to a string
	 *
	 * @param dateTime the dateTime to format
	 * @param format the format to use
	 * @return String
	 */
	public static String formatDateTime(LocalDateTime dateTime, String format) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return dateTime.format(formatter);
	}

	/** 
	 * Format a ZonedDateTime object to a string
	 *
	 * @param dateTime the zoned dateTime to be formatted
	 * @param format the format to use
	 * @return String
	 */
	public static String formatZonedDateTime(ZonedDateTime dateTime, String format) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		return dateTime.format(formatter);
	}

}
