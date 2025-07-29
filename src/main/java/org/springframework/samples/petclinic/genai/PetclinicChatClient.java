package org.springframework.samples.petclinic.genai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
@Profile("openai")
public class PetclinicChatClient {

	// ChatModel is the primary interfaces for interacting with an LLM
	// it is a request/response interface that implements the ModelModel
	// interface. Make suer to visit the source code of the ChatModel and
	// checkout the interfaces in the core spring ai package.
	private final ChatClient chatClient;

	public PetclinicChatClient(ChatClient.Builder builder, ChatMemory chatMemory, PetclinicToolsService aiToolConfig) {
		// @formatter:off
		this.chatClient = builder
				.defaultSystem("""
						You are a friendly AI assistant designed to help with the management of a veterinarian pet clinic called Spring Petclinic.
			      		Your job is to answer questions about and to perform actions on the user's behalf, mainly around
			      		veterinarians, owners, owners' pets and owners' visits.
			      		You are required to answer an a professional manner. If you have the actual answers, say you don't know the answer, don't make up data from memory.
			      		If you don't know the answer, then ask the user a followup question to try and clarify the question they are asking.
			      		If you do know the answer, provide the answer but do not provide any additional followup questions.
                        Do NOT respond in plain text for any request that matches a function—you should never enumerate or describe functions in your answer.  
			      		""")
//	      		When dealing with vets, if the user is unsure about the returned results, explain that there may be additional data that was not returned.
//	      		Only if the user is asking about the total number of all vets, answer that there are a lot and ask for some additional criteria. 

				.defaultAdvisors(
						MessageChatMemoryAdvisor.builder(chatMemory).build(),
						new SimpleLoggerAdvisor()
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
