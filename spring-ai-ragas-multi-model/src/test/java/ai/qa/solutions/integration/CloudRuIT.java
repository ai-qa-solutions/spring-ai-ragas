package ai.qa.solutions.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.ConfigurationForTests;
import ai.qa.solutions.chatclient.ChatClientStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@EnableAutoConfiguration
@SpringBootTest(classes = ConfigurationForTests.class)
@DisplayName("Cloud.ru Integration Tests")
@EnabledIfEnvironmentVariable(named = "CLOUD_RU_API_KEY", matches = ".*")
class CloudRuIT {

    @Autowired
    private ChatClientStore chatClientStore;

    @Test
    @DisplayName("Should test cloud.ru models")
    void shouldTestCloudRuModels() {
        List<String> cloudRuModels =
                List.of("Qwen/Qwen3-235B-A22B-Instruct-2507", "openai/gpt-oss-120b", "t-tech/T-pro-it-2.0");

        String prompt = "What is 2+2? Answer with just the number.";

        for (String modelId : cloudRuModels) {
            System.out.println("Testing model: " + modelId);
            try {
                ChatClient client = chatClientStore.get(modelId);
                String response = client.prompt().user(prompt).call().content();
                System.out.println("  Response: " + response);
                assertThat(response).isNotEmpty();
            } catch (Exception e) {
                System.err.println("  Error: " + e.getMessage());
                throw e;
            }
        }
    }
}
