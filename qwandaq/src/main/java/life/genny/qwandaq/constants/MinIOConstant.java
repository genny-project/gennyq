package life.genny.qwandaq.constants;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MinIOConstant {

    private static final String MINIO_SERVER_URL = "MINIO_SERVER_URL";
    private static final String MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY";
    private static final String MINIO_SECRET_KEY = "MINIO_SECRET_KEY";
    private static final String BUCKET_NAME = "BUCKET_NAME";

    // Fallback message if MinIO throws an exception fetching the object from MinIO
    public final static String ERROR_FALLBACK_MSG = "Error Occurred";

    public static String getServerUrl() {
        return System.getenv(MINIO_SERVER_URL) != null ? System.getenv(MINIO_SERVER_URL) : MINIO_SERVER_URL;
    }

    public static String getAccessKey() {
        return System.getenv(MINIO_ACCESS_KEY) != null ? System.getenv(MINIO_ACCESS_KEY) : MINIO_ACCESS_KEY;
    }

    public static String getPrivateKey() {
        return System.getenv(MINIO_SECRET_KEY) != null ? System.getenv(MINIO_SECRET_KEY) : MINIO_SECRET_KEY;
    }

    public static String getBucketName() {
        return System.getenv(BUCKET_NAME) != null ? System.getenv(BUCKET_NAME) : BUCKET_NAME;
    }
}
