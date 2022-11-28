package life.genny.qwandaq.utils;

import io.minio.*;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.dto.FileUpload;
import life.genny.qwandaq.exception.runtime.MinIOException;
import life.genny.qwandaq.models.GennySettings;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RegisterForReflection
public class MinIOUtils {

    private static final Logger log = Logger.getLogger(MinIOUtils.class);

    private MinioClient minioClient;
    private String REALM = Optional.ofNullable(System.getenv("REALM")).orElse("internmatch");

    private static final String PUBLIC_MEDIA_PATH = "/public/media/";
    private static final String PUBLIC_PATH = "/public";
    private static final String MEDIA_PATH = "/media/";
    private static final String INFO_FILE_SUFFIX = "-info";

    @PostConstruct
    public void beforeConstruct() {
        try {
            minioClient = MinioClient
                    .builder()
                    .endpoint(GennySettings.minIOServerUrl())
                    .credentials(GennySettings.minIOAccessKey(), GennySettings.minIOSecretKey())
                    .build();
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            throw new MinIOException("Constructing MinIOClient failed");
        }
    }

    public String saveOnStore(FileUpload file) {
        boolean isUploaded = uploadFile(REALM + PUBLIC_PATH, file.uploadedFileName(), file.fileName());
        if (isUploaded) {
            return file.fileName();
        } else {
            return null;
        }
    }

    public String saveOnStore(String fileName, File file) {
        Boolean isFileUploaded = uploadFile(REALM + PUBLIC_PATH, file.getPath(), fileName);
        if (isFileUploaded) {
            return fileName;
        } else {
            return null;
        }
    }

    public UUID saveOnStore(FileUpload file, UUID userUUID) {
        UUID randomUUID = UUID.randomUUID();
        Boolean isFileUploaded = uploadFile(userUUID.toString(), file.uploadedFileName(), randomUUID.toString());
        if (isFileUploaded) {
            return randomUUID;
        } else {
            return null;
        }
    }

    public byte[] fetchFromStoreUserDirectory(UUID fileUUID, UUID userUUID) {
        try {
            String fullPath = REALM + "/" + userUUID.toString() + MEDIA_PATH + fileUUID.toString() + INFO_FILE_SUFFIX;
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).build();
            GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
            byte[] byteArray = IOUtils.toByteArray(getObjectResponse);
            return byteArray;
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            return new byte[]{};
        }
    }

    public StatObjectResponse fetchStatFromStorePublicDirectory(UUID fileUUID) {
        return fetchStatFromStorePublicDirectory(fileUUID.toString());
    }

    public StatObjectResponse fetchStatFromStorePublicDirectory(String fileUUID) {
        try {
            String fullPath = REALM + PUBLIC_MEDIA_PATH + fileUUID;
            StatObjectArgs statObjectArgs = StatObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).build();
            StatObjectResponse statObjectResponse = minioClient.statObject(statObjectArgs);
            return statObjectResponse;
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            return null;
        }
    }

    public String fetchInfoFromStorePublicDirectory(UUID fileUUID) {
        return fetchInfoFromStorePublicDirectory(fileUUID.toString());
    }

    public String fetchInfoFromStorePublicDirectory(String fileUUID) {
        try {
            String fullPath = REALM + PUBLIC_MEDIA_PATH + fileUUID + INFO_FILE_SUFFIX;
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).build();
            GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
            byte[] byteArray = IOUtils.toByteArray(getObjectResponse);
            return new String(byteArray);
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            return null;
        }
    }

    public byte[] streamFromStorePublicDirectory(UUID fileUUID, Long start, Long end) {
        return streamFromStorePublicDirectory(fileUUID.toString(), start, end);
    }

    public byte[] streamFromStorePublicDirectory(String fileUUID, Long start, Long end) {
        try {
            String fullPath = REALM + PUBLIC_MEDIA_PATH + fileUUID;
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).offset(start).length(end).build();
            GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
            byte[] byteArray = IOUtils.toByteArray(getObjectResponse);
            return byteArray;
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            return new byte[]{};
        }
    }

    public byte[] fetchFromStorePublicDirectory(UUID fileUUID) {
        try {
            String fullPath = REALM + PUBLIC_MEDIA_PATH + fileUUID.toString();
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).build();
            GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
            byte[] byteArray = IOUtils.toByteArray(getObjectResponse);
            return byteArray;
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            return new byte[]{};
        }
    }

    public byte[] fetchFromStorePublicDirectory(String fileName) {
        try {
            String fullPath = REALM + PUBLIC_MEDIA_PATH + fileName;
            GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).build();
            GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
            byte[] byteArray = IOUtils.toByteArray(getObjectResponse);
            return byteArray;
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            return new byte[]{};
        }
    }

    public void deleteFromStorePublicDirectory(UUID fileUUID) {
        try {
            String fullPath = REALM + PUBLIC_MEDIA_PATH + fileUUID.toString();
            RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(fullPath).build();
            minioClient.removeObject(removeObjectArgs);
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            throw new MinIOException("Delete from minIO failed");
        }
    }

    public boolean uploadFile(String sub, String inpt, String uuid) {
        boolean isSuccess = false;

        String path = sub + MEDIA_PATH + uuid;
        try {
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(GennySettings.minIOBucketName()).build();

            boolean isExist = minioClient.bucketExists(bucketExistsArgs);
            if (isExist) {
                log.debug("Bucket " + GennySettings.minIOBucketName() + "already exists.");
            } else {
                log.debug("Start creat Bucket:" + GennySettings.minIOBucketName());
                MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(GennySettings.minIOBucketName()).build();
                minioClient.makeBucket(makeBucketArgs);
                log.debug("Finish create Bucket:" + GennySettings.minIOBucketName());
            }

            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder().bucket(GennySettings.minIOBucketName()).object(path).filename(inpt).build();

            minioClient.uploadObject(uploadObjectArgs);

            isSuccess = true;
            log.debug("Success, File" + inpt + " uploaded to bucket with path:" + path);
        } catch (Exception ex) {
            log.error("Exception: " + ex.getMessage());
            throw new MinIOException("Uploading file failed");
        }
        return isSuccess;
    }

}
