package life.genny.qwandaq.utils;

import java.security.SecureRandom;
import java.util.stream.Collectors;

/**
 * @author Manjit Shakya
 */
public class RandomStringUtils {

    public static String generateRandomString(int length){
        char[] allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789^$?!@#%&".toCharArray();
        return new SecureRandom()
                .ints(length, 0, allowed.length)
                .mapToObj(i -> String.valueOf(allowed[i]))
                .collect(Collectors.joining());
    }
}
