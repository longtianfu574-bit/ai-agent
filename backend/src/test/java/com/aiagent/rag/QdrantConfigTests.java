package com.aiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "qdrant.host=test-host",
    "qdrant.port=9999",
    "qdrant.collection=test-collection"
})
class QdrantConfigTests {

    @Autowired
    private QdrantConfig config;

    @Test
    void configLoadsPropertiesCorrectly() {
        assertEquals("test-host", config.getHost());
        assertEquals(9999, config.getPort());
        assertEquals("test-collection", config.getCollection());
    }
}
