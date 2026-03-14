package com.aiagent.rag.query;

import com.aiagent.llm.LlmClient;
import com.aiagent.llm.LlmClient.FunctionCallResult;
import com.aiagent.mcp.McpFunctionDefinition;
import com.aiagent.mcp.McpToolExecutor;
import com.aiagent.rag.search.SearchRequest;
import com.aiagent.rag.search.SearchResult;
import com.aiagent.rag.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * RAG Query Service with MCP tool calling support
 */
@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);

    private final SearchService searchService;
    private final QueryRewriter rewriter;
    private final LlmClient llmClient;
    private final McpToolExecutor mcpToolExecutor;

    public RagQueryService(SearchService searchService, QueryRewriter rewriter,
                           LlmClient llmClient, McpToolExecutor mcpToolExecutor) {
        this.searchService = searchService;
        this.rewriter = rewriter;
        this.llmClient = llmClient;
        this.mcpToolExecutor = mcpToolExecutor;
    }

    /**
     * Chat with RAG + MCP tool calling support
     * Automatically calls MCP tools based on user intent
     */
    public CompletableFuture<String> chatWithTools(String userMessage, int topK) {
        log.info("Processing chat with MCP tools: {}", userMessage);

        // Get function definitions for LLM
        List<McpFunctionDefinition> functions = mcpToolExecutor.getFunctionDefinitions();
        List<Map<String, Object>> functionDefs = functions.stream()
            .map(this::toMap)
            .toList();

        // First, check if user needs RAG context or tool calling
        return llmClient.completeWithFunctions(userMessage, null, functionDefs)
            .thenCompose(result -> {
                if (result.functionName() != null) {
                    // LLM wants to call a tool
                    log.info("LLM requested to call tool: {}", result.functionName());
                    return mcpToolExecutor.executeTool(result.functionName(), result.arguments())
                        .thenApply(toolResult -> {
                            String toolOutput = mcpToolExecutor.formatResult(toolResult, result.functionName());

                            // Send tool result back to LLM for final response
                            String followUpPrompt = String.format(
                                "用户问题：%s\n\n工具执行结果：%s\n\n请根据工具执行结果回答用户的问题。",
                                userMessage, toolOutput
                            );
                            return followUpPrompt;
                        })
                        .thenCompose(prompt -> llmClient.complete(prompt, null));
                } else {
                    // No tool call needed, use RAG
                    log.info("No tool call, using RAG");
                    return processRag(userMessage, topK);
                }
            });
    }

    private CompletableFuture<String> processRag(String userMessage, int topK) {
        return buildContext(userMessage, topK)
            .thenCompose(context -> {
                if (context.isBlank()) {
                    return CompletableFuture.completedFuture(
                        "抱歉，没有找到相关的上下文信息。");
                }
                return llmClient.complete(userMessage, context);
            });
    }

    public CompletableFuture<List<SearchResult>> query(String userQuery, int topK) {
        String rewrittenQuery = rewriter.rewrite(userQuery);
        return searchService.search(SearchRequest.builder()
            .query(rewrittenQuery)
            .topK(topK)
            .scoreThreshold(0.5)
            .build());
    }

    public CompletableFuture<String> buildContext(String userQuery, int topK) {
        return query(userQuery, topK)
            .thenApply(results -> {
                StringBuilder context = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    var result = results.get(i);
                    context.append(String.format("[%d] %s (score: %.2f)\n",
                        i + 1, result.getContent(), result.getScore()));
                }
                return context.toString();
            });
    }

    /**
     * Convert function definition to Map for LLM API
     */
    private Map<String, Object> toMap(McpFunctionDefinition def) {
        return Map.of(
            "name", def.getName(),
            "description", def.getDescription(),
            "parameters", toParametersMap(def.getParameters())
        );
    }

    private Map<String, Object> toParametersMap(McpFunctionDefinition.Parameters params) {
        return Map.of(
            "type", params.getType(),
            "properties", params.getProperties(),
            "required", params.getRequired()
        );
    }
}
