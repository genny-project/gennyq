package life.genny.qwandaq.converter;

import io.quarkus.arc.Arc;
import life.genny.qwandaq.constants.MinIOConstant;
import life.genny.qwandaq.constants.QwandaQConstant;
import life.genny.qwandaq.utils.MinIOUtils;
import org.jboss.logging.Logger;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class MinIOConverter implements AttributeConverter<String, String> {

    static final Logger log = Logger.getLogger(MinIOConverter.class);

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData != null && dbData.startsWith(QwandaQConstant.MINIO_LAZY_PREFIX)) {
            log.info("Fetching from MinIO");

            byte[] data = Arc.container().instance(MinIOUtils.class).get().fetchFromStorePublicDirectory(dbData);
            if (data.length > 0) {
                return new String(data);
            } else {
                // Exception handled in Minio.fetchFromStorePublicDirectory(dbData);;
                // This will be the default text the attribute value will show since there was exception in Minio.fetchFromStorePublicDirectory(dbData);
                return MinIOConstant.ERROR_FALLBACK_MSG;
            }
        } else {
            return dbData;
        }
    }
}