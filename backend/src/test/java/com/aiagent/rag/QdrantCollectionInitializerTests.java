package com.aiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "qdrant.host=localhost",
    "qdrant.port=6334",
    "qdrant.collection=test-init-collection"
})
class QdrantCollectionInitializerTests {

    @Autowired
    private QdrantCollectionInitializer initializer;

    @Test
    void initializer_runsWithoutError() throws Exception {
        // This test verifies the initializer can run without throwing
        // In a real test, you would use Testcontainers for Qdrant
        initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    }
}
