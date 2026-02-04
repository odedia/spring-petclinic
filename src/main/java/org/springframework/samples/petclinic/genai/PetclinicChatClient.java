package org.springframework.samples.petclinic.genai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This REST controller is being invoked by the in order to interact with the LLM
 *
 * @author Oded Shopen
 */
@RestController
@RequestMapping("/")

@Profile({ "openai" })
public class PetclinicChatClient {

	// ChatModel is the primary interfaces for interacting with an LLM
	// it is a request/response interface that implements the ModelModel
	// interface. Make suer to visit the source code of the ChatModel and
	// checkout the interfaces in the core spring ai package.
	private final ChatClient chatClient;

	public PetclinicChatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore,
			PetclinicToolsService aiToolConfig) {
// @formatter:off
		this.chatClient = builder
				.defaultSystem("""
					You are a friendly AI assistant designed to help with the management of a veterinarian pet clinic called Spring Petclinic.
				  	Your job is to answer questions about and to perform actions on the user's behalf, mainly around
				  	veterinarians, owners, owners' pets and owners' visits.
				  	You also have access to a veterinary guide that contains medical information about pet care.

				  	IMPORTANT: For personal or conversational questions (like asking about the user's name, greetings, etc.),
				  	use ONLY the conversation history. Do NOT use the retrieved context documents for personal questions.
				  	Only use the retrieved context documents when answering questions about pet clinic data, veterinarians,
				  	owners, pets, visits, or veterinary medicine topics.

				  	You are required to answer in a professional manner. If you don't have the actual answers, say you don't know the answer.
				  	""")
				.defaultAdvisors(
					MessageChatMemoryAdvisor.builder(chatMemory).build(),
					new SimpleLoggerAdvisor(),
					new QuestionAnswerAdvisor(vectorStore)
					)
				.defaultTools(aiToolConfig)
				.build();
  }


@PostMapping("/chatclient")
public String exchange(@RequestBody String query) {
	  //All chatbot messages go through this endpoint
	  //and are passed to the LLM
	  return
	  this.chatClient
	  .prompt()
      .user(
          u ->
              u.text(query)
              )
      .call()
      .content();
  }
}
