package br.com.viafluvial.acervodigital.application.ports;

public interface ObjectStoragePort {

    void putObject(String bucketName, String objectPath, byte[] content, String mimeType);

    byte[] getObject(String bucketName, String objectPath);

    void copyObject(String sourceBucketName, String sourceObjectPath, String targetBucketName, String targetObjectPath);

    void deleteObject(String bucketName, String objectPath);
}
