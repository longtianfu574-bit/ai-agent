# AI Agent Skills 系统实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Skills 系统，支持 Markdown 格式技能定义、命令触发和自然语言路由、用户确认机制。

**Architecture:** Skills 系统包含技能解析器、执行器、路由器和注册表。技能定义存储在 Markdown 文件中，支持热重载。

**Tech Stack:** Java (Spring Boot), Markdown 解析 (Flexmark), Vue 3 (管理界面)

---

## 前置条件

**依赖计划:**
- `2026-03-13-ai-agent-foundation-plan.md` - 基础架构计划（必须已完成）

---

## Chunk 1: 技能定义和解析

### Task 1: 创建技能定义模型

**Files:**
- Create: `backend/src/main/java/com/aiagent/skill/SkillDefinition.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillParameter.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillStep.java`
- Test: `backend/src/test/java/com/aiagent/skill/SkillDefinitionTests.java`

- [ ] **Step 1: 创建 SkillParameter 类**

```java
package com.aiagent.skill;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillParameter {
    private String name;
    private String type; // string, number, boolean, file, enum
    private String description;
    private boolean required;
    private String defaultValue;
    private String[] enumValues;
}
```

- [ ] **Step 2: 创建 SkillStep 类**

```java
package com.aiagent.skill;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillStep {
    private int order;
    private String description;
    private String action; // tool call, decision, loop, etc.
    private String toolName;
    private String condition;
}
```

- [ ] **Step 3: 创建 SkillDefinition 类**

```java
package com.aiagent.skill;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SkillDefinition {
    private String id;
    private String name;
    private String description;
    private List<String> triggers; // Command triggers like /debug
    private List<String> naturalLanguageTriggers; // NL patterns like "help me debug"
    private List<SkillParameter> parameters;
    private List<SkillStep> steps;
    private boolean requiresConfirmation;
    private String confirmationMessage;
    private String category; // debugging, analysis, coding, etc.
    private String version;
    private String author;
}
```

- [ ] **Step 4: 创建测试**

```java
package com.aiagent.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillDefinitionTests {

    @Test
    void skillDefinition_buildsCorrectly() {
        SkillParameter param = SkillParameter.builder()
            .name("file")
            .type("string")
            .description("File path")
            .required(true)
            .build();

        SkillStep step = SkillStep.builder()
            .order(1)
            .description("Read the file")
            .action("tool_call")
            .toolName("read_file")
            .build();

        SkillDefinition skill = SkillDefinition.builder()
            .id("debug")
            .name("Debug")
            .description("Debug code issues")
            .triggers(List.of("/debug", "debug this"))
            .parameters(List.of(param))
            .steps(List.of(step))
            .requiresConfirmation(false)
            .build();

        assertEquals("debug", skill.getId());
        assertEquals(1, skill.getTriggers().size());
        assertEquals(1, skill.getParameters().size());
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend
mvn test -Dtest=SkillDefinitionTests
```
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/aiagent/skill/Skill*.java
git add backend/src/test/java/com/aiagent/skill/SkillDefinitionTests.java
git commit -m "feat: define Skill domain models"
```

---

### Task 2: 创建 Markdown 技能解析器

**Files:**
- Create: `backend/src/main/java/com/aiagent/skill/SkillMarkdownParser.java`
- Create: `backend/src/main/resources/skills/debug.md`
- Create: `backend/src/main/resources/skills/analyze.md`
- Test: `backend/src/test/java/com/aiagent/skill/SkillMarkdownParserTests.java`

- [ ] **Step 1: 添加 Flexmark 依赖 (Markdown 解析)**

在 `pom.xml` 中添加：

```xml
<!-- Markdown parsing -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

- [ ] **Step 2: 创建 Markdown 解析器**

```java
package com.aiagent.skill;

import com.vladsch.flexmark.ext.front.matter.FrontMatterExtension;
import com.vladsch.flexmark.ext.front.matter.FrontMatterNode;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SkillMarkdownParser {

    private final Parser parser;

    public SkillMarkdownParser() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
            FrontMatterExtension.create()
        ));
        this.parser = Parser.builder(options).build();
    }

    public SkillDefinition parse(Path markdownFile) throws IOException {
        String content = Files.readString(markdownFile);
        Node document = parser.parse(content);

        SkillDefinition.SkillDefinitionBuilder builder = SkillDefinition.builder();

        // Parse YAML front matter if exists
        FrontMatterNode frontMatter = document.getFirstChildOfType(FrontMatterNode.class);
        if (frontMatter != null) {
            parseFrontMatter(builder, frontMatter.getChars().toString());
        }

        // Parse sections
        parseName(builder, content);
        parseDescription(builder, content);
        parseTriggers(builder, content);
        parseParameters(builder, content);
        parseSteps(builder, content);
        parseConfirmation(builder, content);

        // Set ID from filename
        String filename = markdownFile.getFileName().toString();
        String id = filename.replace(".md", "");
        builder.id(id);

        return builder.build();
    }

    private void parseFrontMatter(SkillDefinition.SkillDefinitionBuilder builder, String yaml) {
        // Simple YAML parsing (in production, use snakeyaml)
        if (yaml.contains("version:")) {
            Matcher m = Pattern.compile("version:\\s*['\"]?([^'\"\\n]+)")
                .matcher(yaml);
            if (m.find()) builder.version(m.group(1).trim());
        }
        if (yaml.contains("author:")) {
            Matcher m = Pattern.compile("author:\\s*['\"]?([^'\"\\n]+)")
                .matcher(yaml);
            if (m.find()) builder.author(m.group(1).trim());
        }
    }

    private void parseName(SkillDefinition.SkillDefinitionBuilder builder, String content) {
        Matcher m = Pattern.compile("^#\\s*Skill:\\s*(.+)$", Pattern.MULTILINE)
            .matcher(content);
        if (m.find()) builder.name(m.group(1).trim());
    }

    private void parseDescription(SkillDefinition.SkillDefinitionBuilder builder, String content) {
        Matcher m = Pattern.compile("^##\\s*描述\\s*\\n\\n*(.+?)(?=\\n##)", Pattern.MULTILINE)
            .matcher(content);
        if (m.find()) builder.description(m.group(1).trim());
    }

    private void parseTriggers(SkillDefinition.SkillDefinitionBuilder builder, String content) {
        List<String> triggers = new ArrayList<>();
        List<String> nlTriggers = new ArrayList<>();

        // Parse command triggers (/command)
        Matcher m = Pattern.compile("^-\\s*(/\\w+)", Pattern.MULTILINE)
            .matcher(content);
        while (m.find()) triggers.add(m.group(1));

        // Parse natural language triggers (Chinese patterns)
        Matcher nlMatcher = Pattern.compile("帮我 (\\w+)|帮我做 (\\w+)|(\\w+) 一下")
            .matcher(content);
        while (nlMatcher.find()) {
            String match = Arrays.stream(nlMatcher.group(0).split("\\|"))
                .filter(s -> s != null && !s.isEmpty())
                .findFirst().orElse(null);
            if (match != null) nlTriggers.add(match.trim());
        }

        builder.triggers(triggers);
        builder.naturalLanguageTriggers(nlTriggers);
    }

    private void parseParameters(SkillDefinition.SkillDefinitionBuilder builder, String content) {
        List<SkillParameter> parameters = new ArrayList<>();

        Matcher sectionMatcher = Pattern.compile(
            "##\\s*参数\\s*\\n(.*?)(?=\\n##|$)",
            Pattern.DOTALL
        ).matcher(content);

        if (sectionMatcher.find()) {
            String section = sectionMatcher.group(1);
            Matcher paramMatcher = Pattern.compile(
                "-\\s*(\\w+):\\s*(.+?)(?=\\n-|$)",
                Pattern.DOTALL
            ).matcher(section);

            while (paramMatcher.find()) {
                String name = paramMatcher.group(1).trim();
                String desc = paramMatcher.group(2).trim();

                parameters.add(SkillParameter.builder()
                    .name(name)
                    .type("string")
                    .description(desc)
                    .required(true)
                    .build());
            }
        }

        builder.parameters(parameters);
    }

    private void parseSteps(SkillDefinition.SkillDefinitionBuilder builder, String content) {
        List<SkillStep> steps = new ArrayList<>();

        Matcher sectionMatcher = Pattern.compile(
            "##\\s*执行步骤\\s*\\n(.*?)(?=\\n##|$)",
            Pattern.DOTALL
        ).matcher(content);

        if (sectionMatcher.find()) {
            String section = sectionMatcher.group(1);
            Matcher stepMatcher = Pattern.compile(
                "(\\d+)\\.\\s*(.+?)(?=\\n\\d+|$)",
                Pattern.DOTALL
            ).matcher(section);

            int order = 0;
            while (stepMatcher.find()) {
                order++;
                String desc = stepMatcher.group(2).trim();

                steps.add(SkillStep.builder()
                    .order(order)
                    .description(desc)
                    .action("manual")
                    .build());
            }
        }

        builder.steps(steps);
    }

    private void parseConfirmation(SkillDefinition.SkillDefinitionBuilder builder, String content) {
        Matcher m = Pattern.compile("需要确认：\\s*(是 | 否|true|false)", Pattern.CASE_INSENSITIVE)
            .matcher(content);
        if (m.find()) {
            String value = m.group(1).toLowerCase();
            builder.requiresConfirmation("是".equals(value) || "true".equals(value));
        }
    }
}
```

- [ ] **Step 3: 创建示例技能 - debug.md**

```markdown
---
version: 1.0.0
author: AI Agent Team
---

# Skill: debug

## 描述

调试代码问题，分析错误日志并提供修复建议

## 触发词

- /debug
- /troubleshoot

## 自然语言触发

- 帮我调试
- 帮我排查问题
- 分析一下这个错误

## 参数

- file: 目标文件路径
- error: 错误信息或日志

## 执行步骤

1. 读取错误日志或用户提供的错误信息
2. 定位问题代码位置
3. 分析可能的原因
4. 提供修复建议和代码示例

## 权限

- 需要确认：否

## 类别

debugging
```

- [ ] **Step 4: 创建示例技能 - analyze.md**

```markdown
---
version: 1.0.0
author: AI Agent Team
---

# Skill: analyze

## 描述

分析代码质量、复杂度和改进建议

## 触发词

- /analyze
- /review

## 自然语言触发

- 帮我分析
- 查看代码质量
- 审查这段代码

## 参数

- file: 目标文件路径
- focus: 分析重点 (复杂度/安全性/性能)

## 执行步骤

1. 读取目标文件内容
2. 分析代码结构和复杂度
3. 识别潜在问题 (空指针、资源泄漏等)
4. 提供改进建议

## 权限

- 需要确认：否

## 类别

analysis
```

- [ ] **Step 5: 创建解析器测试**

```java
package com.aiagent.skill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkillMarkdownParserTests {

    @Autowired
    private SkillMarkdownParser parser;

    @Test
    void parseParsesDebugSkill() throws Exception {
        Path skillFile = Paths.get("src/main/resources/skills/debug.md");
        SkillDefinition skill = parser.parse(skillFile);

        assertEquals("debug", skill.getId());
        assertEquals("debug", skill.getName());
        assertFalse(skill.getTriggers().isEmpty());
        assertFalse(skill.getParameters().isEmpty());
        assertFalse(skill.getSteps().isEmpty());
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=SkillMarkdownParserTests
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/skill/SkillMarkdownParser.java
git add backend/src/main/resources/skills/
git add backend/src/test/java/com/aiagent/skill/SkillMarkdownParserTests.java
git commit -m "feat: implement Markdown skill parser with example skills"
```

---

## Chunk 2: 技能执行和路由

### Task 3: 创建技能执行器

**Files:**
- Create: `backend/src/main/java/com/aiagent/skill/SkillExecutor.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillContext.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillResult.java`
- Test: `backend/src/test/java/com/aiagent/skill/SkillExecutorTests.java`

- [ ] **Step 1: 创建 SkillContext 类**

```java
package com.aiagent.skill;

import lombok.Builder;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class SkillContext {
    private String sessionId;
    private String userId;
    private String triggeredSkill;
    private Map<String, Object> parameters;
    private Map<String, Object> state;

    public SkillContext() {
        this.parameters = new HashMap<>();
        this.state = new HashMap<>();
    }

    public void setParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
}
```

- [ ] **Step 2: 创建 SkillResult 类**

```java
package com.aiagent.skill;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SkillResult {
    private boolean success;
    private String output;
    private String nextStep;
    private boolean requiresUserInput;
    private List<String> requiredParameters;
    private boolean requiresConfirmation;
    private String confirmationMessage;
}
```

- [ ] **Step 3: 创建 SkillExecutor 接口和实现**

```java
package com.aiagent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(SkillExecutor.class);

    public CompletableFuture<SkillResult> execute(
            SkillDefinition skill,
            SkillContext context) {

        log.info("Executing skill: {} with context: {}", skill.getName(), context);

        // Check if all required parameters are provided
        List<String> missingParams = skill.getParameters().stream()
            .filter(SkillParameter::isRequired)
            .map(SkillParameter::getName)
            .filter(param -> context.getParameter(param) == null)
            .toList();

        if (!missingParams.isEmpty()) {
            SkillResult result = SkillResult.builder()
                .success(false)
                .requiresUserInput(true)
                .requiredParameters(missingParams)
                .output("Missing required parameters: " + String.join(", ", missingParams))
                .build();
            return CompletableFuture.completedFuture(result);
        }

        // Check if confirmation is required
        if (skill.isRequiresConfirmation()) {
            SkillResult result = SkillResult.builder()
                .success(true)
                .requiresConfirmation(true)
                .confirmationMessage(skill.getConfirmationMessage() != null
                    ? skill.getConfirmationMessage()
                    : "确认执行 " + skill.getName() + " ?")
                .nextStep("awaiting_confirmation")
                .build();
            return CompletableFuture.completedFuture(result);
        }

        // Execute the skill steps
        return executeSteps(skill, context);
    }

    private CompletableFuture<SkillResult> executeSteps(
            SkillDefinition skill,
            SkillContext context) {

        // In a real implementation, this would execute each step
        // potentially calling MCP tools or other services
        StringBuilder output = new StringBuilder();
        output.append("Executing skill: ").append(skill.getName()).append("\n");

        for (SkillStep step : skill.getSteps()) {
            output.append("Step ").append(step.getOrder())
                  .append(": ").append(step.getDescription()).append("\n");
            // In production: dispatch based on step.action
        }

        SkillResult result = SkillResult.builder()
            .success(true)
            .output(output.toString())
            .build();

        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<SkillResult> confirmAndContinue(
            SkillDefinition skill,
            SkillContext context,
            boolean confirmed) {

        if (!confirmed) {
            SkillResult result = SkillResult.builder()
                .success(false)
                .output("Skill execution cancelled by user")
                .build();
            return CompletableFuture.completedFuture(result);
        }

        return executeSteps(skill, context);
    }
}
```

- [ ] **Step 4: 创建执行器测试**

```java
package com.aiagent.skill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkillExecutorTests {

    @Autowired
    private SkillExecutor executor;

    @Test
    void execute_returnsMissingParameters() throws Exception {
        SkillDefinition skill = SkillDefinition.builder()
            .id("test")
            .name("Test")
            .parameters(List.of(
                SkillParameter.builder()
                    .name("file")
                    .type("string")
                    .required(true)
                    .build()
            ))
            .build();

        SkillContext context = new SkillContext();

        SkillResult result = executor.execute(skill, context).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isRequiresUserInput());
        assertEquals(1, result.getRequiredParameters().size());
    }

    @Test
    void execute_requiresConfirmation() throws Exception {
        SkillDefinition skill = SkillDefinition.builder()
            .id("test")
            .name("Test")
            .requiresConfirmation(true)
            .build();

        SkillContext context = new SkillContext();

        SkillResult result = executor.execute(skill, context).join();

        assertTrue(result.isRequiresConfirmation());
    }

    @Test
    void execute_runsSuccessfully() throws Exception {
        SkillDefinition skill = SkillDefinition.builder()
            .id("test")
            .name("Test")
            .steps(List.of(
                SkillStep.builder()
                    .order(1)
                    .description("Do something")
                    .build()
            ))
            .build();

        SkillContext context = new SkillContext();
        context.setParameter("file", "test.txt");

        SkillResult result = executor.execute(skill, context).join();

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Executing skill"));
    }
}
```

- [ ] **Step 5: 运行测试**

```bash
cd backend
mvn test -Dtest=SkillExecutorTests
```
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/aiagent/skill/SkillExecutor.java
git add backend/src/main/java/com/aiagent/skill/SkillContext.java
git add backend/src/main/java/com/aiagent/skill/SkillResult.java
git add backend/src/test/java/com/aiagent/skill/SkillExecutorTests.java
git commit -m "feat: implement Skill executor with confirmation support"
```

---

### Task 4: 创建技能路由器和控制器

**Files:**
- Create: `backend/src/main/java/com/aiagent/skill/SkillRouter.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillRegistry.java`
- Create: `backend/src/main/java/com/aiagent/skill/SkillController.java`
- Create: `backend/src/main/java/com/aiagent/skill/dto/ExecuteSkillRequest.java`
- Test: `backend/src/test/java/com/aiagent/skill/SkillRouterTests.java`

- [ ] **Step 1: 创建技能注册表**

```java
package com.aiagent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();
    private final SkillMarkdownParser parser;
    private final String skillsDirectory;

    public SkillRegistry(SkillMarkdownParser parser) {
        this.parser = parser;
        this.skillsDirectory = "src/main/resources/skills";
        loadSkills();
    }

    public void loadSkills() {
        skills.clear();
        Path dir = Paths.get(skillsDirectory);

        if (!Files.exists(dir)) {
            log.warn("Skills directory not found: {}", skillsDirectory);
            return;
        }

        try {
            Files.list(dir)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(this::loadSkill);

            log.info("Loaded {} skills", skills.size());
        } catch (IOException e) {
            log.error("Failed to load skills", e);
        }
    }

    private void loadSkill(Path path) {
        try {
            SkillDefinition skill = parser.parse(path);
            skills.put(skill.getId(), skill);
            log.debug("Loaded skill: {}", skill.getId());
        } catch (IOException e) {
            log.error("Failed to parse skill: {}", path, e);
        }
    }

    public Optional<SkillDefinition> getSkillById(String id) {
        return Optional.ofNullable(skills.get(id));
    }

    public Optional<SkillDefinition> getByTrigger(String trigger) {
        return skills.values().stream()
            .filter(s -> s.getTriggers().contains(trigger))
            .findFirst();
    }

    public Optional<SkillDefinition> getByNaturalLanguage(String text) {
        return skills.values().stream()
            .filter(s -> s.getNaturalLanguageTriggers().stream()
                .anyMatch(trigger -> text.contains(trigger)))
            .findFirst();
    }

    public Collection<SkillDefinition> listAllSkills() {
        return skills.values();
    }

    public Collection<SkillDefinition> getSkillsByCategory(String category) {
        return skills.values().stream()
            .filter(s -> category.equals(s.getCategory()))
            .toList();
    }
}
```

- [ ] **Step 2: 创建技能路由器**

```java
package com.aiagent.skill;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SkillRouter {

    private final SkillRegistry registry;

    public SkillRouter(SkillRegistry registry) {
        this.registry = registry;
    }

    public RouteResult route(String input) {
        // Check for command trigger first
        if (input.startsWith("/")) {
            String command = input.split("\\s+")[0];
            Optional<SkillDefinition> skill = registry.getByTrigger(command);
            if (skill.isPresent()) {
                return RouteResult.builder()
                    .found(true)
                    .skill(skill.get())
                    .routeType(RouteType.COMMAND)
                    .build();
            }
        }

        // Try natural language matching
        Optional<SkillDefinition> nlSkill = registry.getByNaturalLanguage(input);
        if (nlSkill.isPresent()) {
            return RouteResult.builder()
                .found(true)
                .skill(nlSkill.get())
                .routeType(RouteType.NATURAL_LANGUAGE)
                .build();
        }

        return RouteResult.builder()
            .found(false)
            .routeType(RouteType.NONE)
            .build();
    }

    public enum RouteType {
        COMMAND,
        NATURAL_LANGUAGE,
        NONE
    }

    @lombok.Builder
    @lombok.Data
    public static class RouteResult {
        private boolean found;
        private SkillDefinition skill;
        private RouteType routeType;
    }
}
```

- [ ] **Step 3: 创建请求 DTO**

```java
package com.aiagent.skill.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ExecuteSkillRequest {
    private String skillId;
    private Map<String, Object> parameters;
    private String sessionId;
    private String confirmationId;
    private Boolean confirmed;
}
```

- [ ] **Step 4: 创建控制器**

```java
package com.aiagent.skill;

import com.aiagent.skill.dto.ExecuteSkillRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRegistry registry;
    private final SkillRouter router;
    private final SkillExecutor executor;

    // Store pending confirmations
    private final Map<String, PendingConfirmation> pendingConfirmations = new HashMap<>();

    public SkillController(
            SkillRegistry registry,
            SkillRouter router,
            SkillExecutor executor) {
        this.registry = registry;
        this.router = router;
        this.executor = executor;
    }

    @GetMapping
    public List<SkillDefinition> listSkills() {
        return new ArrayList<>(registry.listAllSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillDefinition> getSkill(@PathVariable String id) {
        return registry.getSkillById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/route")
    public ResponseEntity<SkillRouter.RouteResult> route(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        SkillRouter.RouteResult result = router.route(input);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/execute")
    public ResponseEntity<SkillResult> execute(@RequestBody ExecuteSkillRequest request) {
        SkillDefinition skill = registry.getSkillById(request.getSkillId())
            .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + request.getSkillId()));

        SkillContext context = new SkillContext();
        context.setSessionId(request.getSessionId());
        if (request.getParameters() != null) {
            request.getParameters().forEach(context::setParameter);
        }

        SkillResult result = executor.execute(skill, context).join();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/confirm")
    public ResponseEntity<SkillResult> confirm(@RequestBody ExecuteSkillRequest request) {
        PendingConfirmation pending = pendingConfirmations.remove(request.getConfirmationId());
        if (pending == null) {
            return ResponseEntity.badRequest().build();
        }

        SkillResult result = executor.confirmAndContinue(
            pending.skill(),
            pending.context(),
            Boolean.TRUE.equals(request.getConfirmed())
        ).join();

        return ResponseEntity.ok(result);
    }

    private record PendingConfirmation(SkillDefinition skill, SkillContext context) {}
}
```

- [ ] **Step 5: 创建路由器测试**

```java
package com.aiagent.skill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkillRouterTests {

    @Autowired
    private SkillRouter router;

    @Test
    void route_findsCommandTrigger() {
        var result = router.route("/debug file=test.txt");

        assertTrue(result.isFound());
        assertEquals(SkillRouter.RouteType.COMMAND, result.getRouteType());
        assertEquals("debug", result.getSkill().getId());
    }

    @Test
    void route_findsNaturalLanguageTrigger() {
        var result = router.route("帮我调试这个问题");

        assertTrue(result.isFound());
        assertEquals(SkillRouter.RouteType.NATURAL_LANGUAGE, result.getRouteType());
    }

    @Test
    void route_returnsNotFound() {
        var result = router.route("random text that matches nothing");

        assertFalse(result.isFound());
        assertEquals(SkillRouter.RouteType.NONE, result.getRouteType());
    }
}
```

- [ ] **Step 6: 运行测试**

```bash
cd backend
mvn test -Dtest=SkillRouterTests
```
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/aiagent/skill/SkillRouter.java
git add backend/src/main/java/com/aiagent/skill/SkillRegistry.java
git add backend/src/main/java/com/aiagent/skill/SkillController.java
git add backend/src/main/java/com/aiagent/skill/dto/
git add backend/src/test/java/com/aiagent/skill/SkillRouterTests.java
git commit -m "feat: implement Skill router and REST controller"
```

---

## 验收标准

1. `GET /api/skills` 返回所有技能列表
2. `POST /api/skills/route` 正确路由命令和自然语言输入
3. `POST /api/skills/execute` 执行技能并返回结果
4. 需要确认的技能正确处理确认流程
5. Markdown 技能文件支持热重载

---

## 后续工作

1. 技能步骤的实际执行（集成 MCP 工具）
2. 自然语言路由的语义匹配（使用嵌入相似度）
3. 技能执行历史和日志
4. 前端技能管理界面
