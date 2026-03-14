package com.aiagent.embedding;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WireMockTest(httpPort = 9999)
@TestPropertySource(properties = {
    "embedding.service-url=http://localhost:9999"
})
class EmbeddingClientTests {

    @Autowired
    private EmbeddingClient client;

    @Test
    void embedSingle_returnsEmbedding() throws Exception {
        stubFor(post("/embeddings")
            .willReturn(okJson("""
                {
                    "model": "Qwen3-Embedding",
                    "object": "list",
                    "usage": {"prompt_tokens": 2, "total_tokens": 2},
                    "data": [{
                        "embedding": [0.1, 0.2, 0.3, 0.4, 0.5],
                        "index": 0,
                        "object": "embedding"
                    }]
                }
                """)));

        var result = client.embedSingle("test text").join();

        assertEquals(5, result.size());
        assertEquals(0.1f, result.get(0));
    }

    @Test
    void embed_returnsMultipleEmbeddings() throws Exception {
        stubFor(post("/embeddings")
            .willReturn(okJson("""
                {
                    "model": "Qwen3-Embedding",
                    "object": "list",
                    "usage": {"prompt_tokens": 4, "total_tokens": 4},
                    "data": [{
                        "embedding": [0.1, 0.2, 0.3, 0.4, 0.5],
                        "index": 0,
                        "object": "embedding"
                    }]
                }
                """)));

        var result = client.embed(List.of("text1", "text2")).join();

        assertEquals(2, result.size());
        assertEquals(5, result.get(0).size());
    }
}
