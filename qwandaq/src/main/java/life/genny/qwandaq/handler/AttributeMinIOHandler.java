package life.genny.qwandaq.handler;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.constants.QwandaQConstant;
import life.genny.qwandaq.dto.FileUpload;
import life.genny.qwandaq.exception.runtime.AttributeMinIOException;
import life.genny.qwandaq.utils.ConfigUtils;
import life.genny.qwandaq.utils.MinIOUtils;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AttributeMinIOHandler {
    private static final Logger log = Logger.getLogger(AttributeMinIOHandler.class);

    public static String convertToMinIOObject(String valueString, String baseEntityCode, String attributeCode) {
        try {
            int limit = ConfigUtils.getConfig("attribute.minio.threshold", Integer.class) * 1024; // 4Kb
            // logic to push to minIO if it is greater than certain size
            byte[] data = valueString.getBytes(StandardCharsets.UTF_8);
            if (data.length > limit) {
                log.info("Converting attribute as MinIO Object");
                String fileName = QwandaQConstant.MINIO_LAZY_PREFIX + baseEntityCode + "-" + attributeCode;
                String path = ConfigUtils.getConfig("file.temp", String.class);
                File theDir = new File(path);
                if (!theDir.exists()) {
                    theDir.mkdirs();
                }
                String fileInfoName = path.concat(fileName);
                File fileInfo = new File(fileInfoName);
                try (FileWriter myWriter = new FileWriter(fileInfo.getPath())) {
                    myWriter.write(valueString);
                } catch (IOException e) {
                    new AttributeMinIOException("Converting to MinIO Object failed", e.fillInStackTrace()).printStackTrace();
                    return valueString;
                }
                log.info("Writing to MinIO");

                String fileId =  Arc.container().instance(MinIOUtils.class).get().saveOnStore(new FileUpload(fileName, fileInfoName));

                fileInfo.delete();

                return fileId;
            } else {
                return valueString;
            }
        } catch (Exception ex) {
            new AttributeMinIOException("Converting to MinIO Object failed", ex.fillInStackTrace()).printStackTrace();
            return valueString;
        }
    }
}
