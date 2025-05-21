package org.springframework.samples.petclinic.genai;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * This class defines the @Bean functions that the LLM provider will invoke when it
 * requires more Information on a given topic. The currently available functions enable
 * the LLM to get the list of owners and their pets, get information about the
 * veterinarians, and add a pet to an owner.
 *
 * @author Oded Shopen
 */
@Service
@Profile("openai")
class PetclinicToolsService {

	private final AIVectorDataProvider petclinicVectorProvider;

	private OwnerRepository ownerRepository;

	public PetclinicToolsService(AIVectorDataProvider petclinicAiProvider, OwnerRepository ownerRepository,
			VetRepository vetRepository, VectorStore vectorStore) {
		this.ownerRepository = ownerRepository;
		this.petclinicVectorProvider = petclinicAiProvider;
	}

	@Tool(name = "listOwners", description = "List all pet clinic owners, returning their basic information")
	public List<Owner> listOwners() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<Owner> ownerPage = ownerRepository.findAll(pageable);
		return ownerPage.getContent();
	}

	@Tool(name = "addOwnerToPetclinic",
			description = "Add a new pet owner to the pet clinic. The Owner must include first name and last name as two separate words, an address and a 10‑digit phone number")
	public Owner addOwnerToPetclinic(@ToolParam(description = "The owner details to add") Owner owner) {
		ownerRepository.save(owner);
		return owner;
	}

	@Tool(name = "listVets", description = "List all veterinarians at the pet clinic")
	public List<String> listVets(@ToolParam(description = "Optional filter criteria for veterinarians") Vet vet)
			throws JsonProcessingException {
		return petclinicVectorProvider.getVets(vet);
	}

	@Tool(name = "addPetToOwner",
			description = "Add a pet (with specified petTypeId) to an owner by ownerId. Allowed petTypeIds: 1=cat,2=dog,3=lizard,4=snake,5=bird,6=hamster")
	public Owner addPetToOwner(@ToolParam(description = "The pet details", required = true) Pet pet,
			@ToolParam(description = "The id of the Owner", required = true) Integer ownerId) {
		Owner owner = ownerRepository.findById(ownerId);
		owner.addPet(pet);
		return owner;
	}

}
