package com.aiagent.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Collections.Distance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class QdrantCollectionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QdrantCollectionInitializer.class);

    private final QdrantClientFactory clientFactory;
    private final QdrantConfig config;

    public QdrantCollectionInitializer(
            QdrantClientFactory clientFactory,
            QdrantConfig config) {
        this.clientFactory = clientFactory;
        this.config = config;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeCollection();
    }

    private void initializeCollection() throws ExecutionException, InterruptedException {
        try (QdrantClient client = clientFactory.createClient()) {
            String collectionName = config.getCollection();

            // Try to create collection, ignore if already exists
            try {
                log.info("Creating Qdrant collection: {}", collectionName);
                client.createCollectionAsync(
                    collectionName,
                    Collections.VectorParams.newBuilder()
                        .setSize(config.getVectorSize())
                        .setDistance(Distance.Cosine)
                        .build()
                ).get();
                log.info("Collection created successfully");
            } catch (ExecutionException e) {
                if (e.getCause() != null && e.getCause().getMessage() != null
                    && e.getCause().getMessage().contains("already exists")) {
                    log.info("Collection {} already exists", collectionName);
                } else {
                    throw e;
                }
            }
        }
    }
}
