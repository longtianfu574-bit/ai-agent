package com.aiagent.rag;

import lombok.Data;

@Data
public class QdrantCollectionConfig {
    private String collectionName;
    private int vectorSize;
    private String distance = "Cosine"; // Cosine, Euclid, Dot

    public static QdrantCollectionConfig defaultConfig(String collectionName, int vectorSize) {
        QdrantCollectionConfig config = new QdrantCollectionConfig();
        config.setCollectionName(collectionName);
        config.setVectorSize(vectorSize);
        config.setDistance("Cosine");
        return config;
    }
}
