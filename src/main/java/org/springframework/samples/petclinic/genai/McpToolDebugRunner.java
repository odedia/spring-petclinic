package org.springframework.samples.petclinic.genai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("openai")
public class McpToolDebugRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(McpToolDebugRunner.class);

	private final List<ToolCallback> toolCallbacks;

	public McpToolDebugRunner(List<ToolCallback> toolCallbacks) {
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("MCP DEBUG: Checking for registered ToolCallbacks...");
		if (toolCallbacks.isEmpty()) {
			logger.warn("MCP DEBUG: No ToolCallbacks found! Tools will not be available.");
		}
		else {
			logger.info("MCP DEBUG: Found {} ToolCallbacks:", toolCallbacks.size());
			for (ToolCallback tc : toolCallbacks) {
				logger.info(" - Tool Name: {}", tc.getToolDefinition().name());
				logger.info("   Description: {}", tc.getToolDefinition().description());
			}
		}
	}

}
