package life.genny.qwandaq.utils;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigUtils {

    public static <T> T getConfig(String key, Class<T> clazz) {
        Config config = ConfigProvider.getConfig();
        return config.getValue(key, clazz);
    }
}
