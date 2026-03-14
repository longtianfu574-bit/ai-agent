package com.aiagent.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for McpService
 */
@SpringBootTest
class McpServiceTests {

    @Autowired
    private McpService mcpService;

    @MockBean
    private McpClient mcpClient;

    @Test
    void listAllTools_returnsToolsFromAllServers() throws Exception {
        List<McpTool> mockTools = List.of(
            new McpTool("read_file", "Read file", Map.of("path", Map.of("type", "string"))),
            new McpTool("write_file", "Write file", Map.of())
        );

        when(mcpClient.listTools(anyString()))
            .thenReturn(CompletableFuture.completedFuture(mockTools));

        List<McpTool> allTools = mcpService.listAllTools().join();

        // 5 servers * 2 tools each = 10 tools
        assertEquals(10, allTools.size());

        // Check that server prefix is added (all tools should have a server prefix)
        assertTrue(allTools.stream().allMatch(t -> t.getName().contains("/")));
    }

    @Test
    void callTool_callsCorrectServer() throws Exception {
        var mockResult = new McpClient.ToolCallResult(true, "output", null);

        when(mcpClient.callTool(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(mockResult));

        var result = mcpService.callTool("filesystem", "read_file", Map.of("path", "test.txt")).join();

        assertTrue(result.success());
        assertEquals("output", result.output());
    }

    @Test
    void callTool_withUnknownServer_throwsException() {
        var future = mcpService.callTool("unknown-server", "some_tool", Map.of());

        assertThrows(Exception.class, () -> future.join());
    }

    @Test
    void getServerUrl_returnsCorrectUrl() {
        String url = mcpService.getServerUrl("filesystem");
        assertEquals("http://localhost:5001", url);
    }

    @Test
    void getServerNames_returnsAllServers() {
        var names = mcpService.getServerNames();

        assertEquals(5, names.size());
        assertTrue(names.contains("filesystem"));
        assertTrue(names.contains("shell"));
        assertTrue(names.contains("database"));
        assertTrue(names.contains("http"));
        assertTrue(names.contains("git"));
    }
}
