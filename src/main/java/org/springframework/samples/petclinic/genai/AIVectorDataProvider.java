package org.springframework.samples.petclinic.genai;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Functions that are invoked by the LLM will use this bean to query the system of record
 * for information such as listing owners and vers, or adding pets to an owner.
 *
 * @author Oded Shopen
 */
@Service
@Profile("openai")
public class AIVectorDataProvider {

	private final VectorStore vectorStore;

	public AIVectorDataProvider(OwnerRepository ownerRepository, VetRepository vetRepository, VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	public List<String> getVets(Vet vet) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String vetAsJson = objectMapper.writeValueAsString(vet);

		SearchRequest sr = SearchRequest.builder().topK(20).query(vetAsJson).build();
		if (vet == null) {
			// Provide a limit of 50 results when zero parameters are sent
			sr = SearchRequest.from(sr).topK(50).build();
		}

		List<Document> topMatches = this.vectorStore.similaritySearch(sr);
		List<String> results = topMatches.stream().map(document -> document.getFormattedContent()).toList();
		return results;
	}

}
