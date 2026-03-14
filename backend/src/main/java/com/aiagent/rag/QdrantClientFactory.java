package com.aiagent.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.stereotype.Component;

@Component
public class QdrantClientFactory {

    private final QdrantConfig config;

    public QdrantClientFactory(QdrantConfig config) {
        this.config = config;
    }

    public QdrantClient createClient() {
        QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(
                config.getHost(),
                config.getPort(),
                false // usePlaintext
            ).build();
        return new QdrantClient(grpcClient);
    }
}
