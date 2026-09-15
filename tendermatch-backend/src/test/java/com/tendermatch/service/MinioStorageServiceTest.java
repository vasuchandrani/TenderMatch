package com.tendermatch.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageService minioStorageService;
    private final String bucketName = "test-tender-pdfs";

    @BeforeEach
    void setUp() {
        minioStorageService = new MinioStorageService(minioClient, bucketName);
    }

    @Test
    void shouldUploadFileToCorrectPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "sample pdf content".getBytes()
        );
        String objectKey = "requirements/123/test.pdf";

        when(minioClient.bucketExists(any())).thenReturn(true);

        String resultKey = minioStorageService.uploadFile(objectKey, file);

        assertEquals(objectKey, resultKey);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void shouldGeneratePresignedUrl() throws Exception {
        String objectKey = "requirements/123/test.pdf";
        String expectedUrl = "http://localhost:9000/test-tender-pdfs/requirements/123/test.pdf?signature=xyz";

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

        String actualUrl = minioStorageService.getPresignedUrl(objectKey, 3600);

        assertEquals(expectedUrl, actualUrl);
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void shouldDeleteFile() throws Exception {
        String objectKey = "requirements/123/test.pdf";

        minioStorageService.deleteFile(objectKey);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
