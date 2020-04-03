package com.box.l10n.mojito.service.blobstorage.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.Tag;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation that uses S3 to store blobs.
 * <p>
 * Rely on S3 lifecyle rules to cleanup expired blobs. This must be setup manually else no clean up will happen.
 * <p>
 * Objects will have a "retention" tag, see values in {@link Retention}
 * <p>
 * See https://docs.aws.amazon.com/AmazonS3/latest/dev/object-lifecycle-mgmt.html for detail.
 *
 * <pre>
 * Overall steps are:
 *    - Add a lifecyle rule
 *    - Enter rule name: "Expire ephemeral"
 *    - Choose a rule scope: Limit the scope to specific prefixes or tags
 *    - Add prefix or tag filter: "ephemeral", no value
 *    - next > next > Expiration
 *    - Configure expiration
 *        Current version: tick
 *        Previous versions: tick
 *        Expire current version of object: after 1 day
 *        Permanently delete previous versions: after 1 day
 *    - next > Save
 * </pre>
 */
public class S3BlobStorage implements BlobStorage {

    static final Logger logger = LoggerFactory.getLogger(S3BlobStorage.class);
    static final String NO_SUCH_KEY = "NoSuchKey";

    AmazonS3 amazonS3;

    S3BlobStorageConfigurationProperties s3BlobStorageConfigurationProperties;

    public S3BlobStorage(AmazonS3 amazonS3,
                         S3BlobStorageConfigurationProperties s3BlobStorageConfigurationProperties) {
        Preconditions.checkNotNull(amazonS3);
        Preconditions.checkNotNull(s3BlobStorageConfigurationProperties);

        this.amazonS3 = amazonS3;
        this.s3BlobStorageConfigurationProperties = s3BlobStorageConfigurationProperties;
    }

    @Override
    public Optional<byte[]> getBytes(String name) {

        byte[] bytes = null;

        try (S3Object object = amazonS3.getObject(s3BlobStorageConfigurationProperties.getBucket(), getFullName(name))) {
            S3ObjectInputStream objectContent = object.getObjectContent();
            bytes = ByteStreams.toByteArray(objectContent);
        } catch (AmazonServiceException e) {
            if (!NO_SUCH_KEY.equals(e.getErrorCode())) {
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Optional.ofNullable(bytes);
    }

    @Override
    public Optional<String> getString(String name) {
        String objectAsString = null;

        try {
            objectAsString = amazonS3.getObjectAsString(s3BlobStorageConfigurationProperties.getBucket(), getFullName(name));
        } catch (AmazonServiceException e) {
            if (!NO_SUCH_KEY.equals(e.getErrorCode())) {
                throw e;
            }
        }

        return Optional.ofNullable(objectAsString);
    }


    @Override
    public void put(String name, byte[] content, Retention retention) {
        put(name, content, retention, new ObjectMetadata());
    }

    @Override
    public void put(String name, String content, Retention retention) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("text/plain");
        objectMetadata.setContentEncoding(StandardCharsets.UTF_8.toString());

        put(name, bytes, retention, objectMetadata);
    }

    void put(String name, byte[] content, Retention retention, ObjectMetadata objectMetadata) {

        Preconditions.checkNotNull(objectMetadata);
        objectMetadata.setContentLength(content.length);

        PutObjectRequest putRequest = new PutObjectRequest(
                s3BlobStorageConfigurationProperties.getBucket(),
                getFullName(name),
                new ByteArrayInputStream(content),
                objectMetadata);

        List<Tag> tags = new ArrayList<Tag>();
        tags.add(new Tag("retention", retention.toString()));

        putRequest.setTagging(new ObjectTagging(tags));

        amazonS3.putObject(putRequest);
    }

    String getFullName(String name) {
        return s3BlobStorageConfigurationProperties.getPrefix() + "/" + name;
    }
}
