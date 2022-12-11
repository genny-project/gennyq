package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import life.genny.qwandaq.utils.callbacks.FILogCallback;

public class LogUtils {

	private static Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Log a set of {@link HttpHeaders}
	 * @param level - the log level to log at (default: info)
	 * @param headers - the headers to log
	 */
	public static void logHeaders(FILogCallback level, HttpHeaders headers) {
		Map<String, List<String>> headerMap = headers.map();
		headerMap.keySet().stream().forEach((key) -> {
			List<String> values = headerMap.get(key);
			String valueString = values.stream()
			.collect(Collectors.joining(", ", "[", "]"));

			level.log(key + ": " + valueString);
		});
	}

	/**
	 * Log a set of {@link HttpHeaders} to the info log level
	 * @param log - the logger to log from
	 * @param headers - the headers to log
	 */
	public static void logHeaders(Logger log, HttpHeaders headers) {
		logHeaders(log::info, headers);
	}

	/**
	 * Log a set of {@link HttpHeaders} to the {@link HttpUtils#log} level
	 * @param headers - the headers to log
	 */
	public static void logHeaders(HttpHeaders headers) {
		logHeaders(log, headers);
	}

}
