package com.tendermatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.minio.endpoint=http://localhost:9000",
        "app.minio.access-key=test",
        "app.minio.secret-key=test",
        "app.minio.bucket-name=test-bucket"
})
class TenderMatchApplicationTests {

    @Test
    void contextLoads() {
    }
}
