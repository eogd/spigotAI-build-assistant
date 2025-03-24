import java.nio.file.*;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GeminiPluginGenerator {
    // AI API，因为他是免费的，我很喜欢
    private static final String API_URL = "URL";
    private static final String API_KEY = "YOUR_API_KEY_HERE"; 
    
    // 项目配置
    private static final String PROJECT_ROOT = "./generated-plugin";
    private static final String POM_CONTENT = """
        <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.gemini</groupId>
            <artifactId>auto-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            
            <properties>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                <maven.compiler.encoding>UTF-8</maven.compiler.encoding>
            </properties>

            <repositories>
                <repository>
                    <id>spigot-repo</id>
                    <url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url>
                </repository>
            </repositories>
            
            <dependencies>
                <dependency>
                    <groupId>org.spigotmc</groupId>
                    <artifactId>spigot-api</artifactId>
                    <version>1.20.1-R0.1-SNAPSHOT</version>
                    <scope>provided</scope>
                </dependency>
            </dependencies>
            
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.11.0</version>
                        <configuration>
                            <source>17</source>
                            <target>17</target>
                            <encoding>UTF-8</encoding>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </project>
        """;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in, "UTF-8")) {
            System.out.print("请输入插件需求：");
            String requirement = scanner.nextLine();
            
            // 生成代码
            String code = generateCode(requirement);
            System.out.println("代码生成完成");
            
            // 创建项目结构
            Path projectPath = createProjectStructure();
            writeFiles(projectPath, code);
            buildProject(projectPath);
            System.out.println("构建完成！插件位于 target 目录");
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
        }
    }

    private static String generateCode(String prompt) throws Exception {
        // 增强提示词
        String systemPrompt = """
            作为Spigot插件专家，生成符合以下要求的Java代码：
            1. 必须继承JavaPlugin并实现onEnable/onDisable
            2. 包含完整的事件监听器
            3. 使用Java 17语法
            4. 类名为GeminiPlugin
            5. 代码文件使用UTF-8编码""";

        // 构建API请求
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s\\n用户需求：%s"
                    }]
                }]
            }""", systemPrompt, prompt.replace("\"", "\\\""));
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?key=" + API_KEY))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // 处理响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException("API调用失败: " + response.body());
        }
        return parseResponse(response.body());
    }

    private static String parseResponse(String json) {
        int start = json.indexOf("\"text\": \"") + 8;
        int end = json.lastIndexOf("\"}]}}");
        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }

    private static Path createProjectStructure() throws Exception {
        Path path = Paths.get(PROJECT_ROOT);
        Files.createDirectories(path.resolve("src/main/java/com/gemini"));
        Files.createDirectories(path.resolve("src/main/resources"));
        return path;
    }

    private static void writeFiles(Path path, String code) throws Exception {
        // UTF-8
        Files.write(path.resolve("src/main/java/com/gemini/GeminiPlugin.java"), 
                   code.getBytes(StandardCharsets.UTF_8));
        
        Files.write(path.resolve("pom.xml"), 
                   POM_CONTENT.getBytes(StandardCharsets.UTF_8));
        
        String pluginYml = """
            name: GeminiPlugin
            version: 1.0
            main: com.gemini.GeminiPlugin
            api-version: 1.20
            commands: {}
            """;
        Files.write(path.resolve("src/main/resources/plugin.yml"), 
                   pluginYml.getBytes(StandardCharsets.UTF_8));
    }

    private static void buildProject(Path path) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package")
                .directory(path.toFile())
                .inheritIO();
        
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("构建失败，退出码：" + exitCode);
        }
    }
}
