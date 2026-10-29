package es.upm.miw.oneleft.plans.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The language model of HU-010 is off until it is chosen: without a key the service starts with no model, and with
 * Compose's setting the Anthropic one is created.
 */
class AiGatingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(AnthropicChatAutoConfiguration.class, ChatClientAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class));

    @Test
    void withoutAKeyTheServiceStartsWithNoModel() {
        runner.withPropertyValues("spring.ai.model.chat=none", "spring.ai.chat.client.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ChatModel.class);
            assertThat(context.getBeanProvider(ChatClient.Builder.class).getIfAvailable()).isNull();
        });
    }

    @Test
    void anEmptySettingFromComposeAlsoMeansNoModel() {
        runner.withPropertyValues("spring.ai.model.chat=", "spring.ai.chat.client.enabled=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeanProvider(ChatClient.Builder.class).getIfAvailable()).isNull();
        });
    }

    @Test
    void withTheKeyAndTheSettingTheAnthropicModelIsCreated() {
        runner.withPropertyValues("spring.ai.model.chat=anthropic", "spring.ai.anthropic.api-key=test-key",
                "spring.ai.chat.client.enabled=true", "spring.ai.anthropic.chat.options.model=claude-haiku-5-5").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AnthropicChatModel.class);
            assertThat(context.getBeanProvider(ChatClient.Builder.class).getIfAvailable()).isNotNull();
        });
    }
}
