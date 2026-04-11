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

				  	You have two sources of information, and you MUST pick the right one for each question:

				  	1. TOOLS — for live pet clinic data. Whenever the user asks about owners, pets, veterinarians,
				  	   or visits (e.g. "list the owners", "who are the vets", "add a pet"), you MUST call the
				  	   appropriate tool. The tools are the ONLY source of truth for this data. Do not answer
				  	   these questions from retrieved documents or from memory.

				  	2. RETRIEVED CONTEXT (veterinary guide) — for medical and pet-care knowledge only
				  	   (e.g. symptoms, treatments, general animal health). Use retrieved context ONLY for
				  	   veterinary medicine questions, never for clinic records.

				  	For personal or conversational questions (like the user's name, greetings), use ONLY the
				  	conversation history. Do NOT use retrieved context or tools for personal questions.

				  	You are required to answer in a professional manner. If a tool call returns no data, say so
				  	plainly. If you genuinely don't know the answer, say you don't know.
				  	""")
				.defaultTools(aiToolConfig)
				.defaultAdvisors(
					MessageChatMemoryAdvisor.builder(chatMemory).build(),
					new SimpleLoggerAdvisor(),
					new QuestionAnswerAdvisor(vectorStore)
					)
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
