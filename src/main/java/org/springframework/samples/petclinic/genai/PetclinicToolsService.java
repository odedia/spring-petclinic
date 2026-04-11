package org.springframework.samples.petclinic.genai;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Service;

/**
 * This class defines the @Bean functions that the LLM provider will invoke when it
 * requires more Information on a given topic. The currently available functions enable
 * the LLM to get the list of owners and their pets, get information about the
 * veterinarians, and add a pet to an owner.
 *
 * @author Oded Shopen
 */
@Service
@Profile({ "openai" })
public class PetclinicToolsService {

	private final Logger logger = LoggerFactory.getLogger(PetclinicToolsService.class);

	private final OwnerRepository ownerRepository;

	private final VetRepository vetRepository;

	public PetclinicToolsService(OwnerRepository ownerRepository, VetRepository vetRepository) {
		this.ownerRepository = ownerRepository;
		this.vetRepository = vetRepository;
		logger.info("PetclinicToolsService initialized! Tools should be available.");
	}

	@Tool(name = "listOwners",
			description = "List all pet clinic owners. Each owner includes their address, phone, and a list of their pets with each pet's name, birth date, and type (dog, cat, lizard, snake, bird, hamster). Use this tool to answer any question about which owners own which pets, which owners have a specific type of pet (e.g. \"who has dogs\"), or to look up owner contact details.")
	public List<Owner> listOwners() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<Owner> ownerPage = ownerRepository.findAll(pageable);
		return ownerPage.getContent();
	}

	@Tool(name = "addOwnerToPetclinic",
			description = "Add or update a pet owner in the pet clinic. It's ok to have zero pets. The Owner must include first name and last name as two separate words, an address and a 10‑digit phone number")
	public Owner addOwnerToPetclinic(@ToolParam(description = "The owner details to add") Owner owner) {
		logger.info("received owner \n\n{}\n\n", owner);
		ownerRepository.save(owner);
		return owner;
	}

	@Tool(name = "listVets", description = "List all veterinarians at the pet clinic")
	public List<Vet> listVets() {
		Pageable pageable = PageRequest.of(0, 100);
		Page<Vet> vetPage = vetRepository.findAll(pageable);
		return vetPage.getContent();
	}

	@Tool(name = "addPetToOwner", description = """
			Add a new pet to an existing owner. Requires the owner's ID, the pet's name, the pet's
			birth date in ISO format (yyyy-MM-dd), and the pet type ID. Valid pet type IDs are:
			1 = cat, 2 = dog, 3 = lizard, 4 = snake, 5 = bird, 6 = hamster.
			""")
	public Owner addPetToOwner(@ToolParam(description = "The pet owner's ID", required = true) Integer ownerId,
			@ToolParam(description = "The pet's name", required = true) String name,
			@ToolParam(description = "The pet's birth date in yyyy-MM-dd format", required = true) String birthDate,
			@ToolParam(description = "The pet type ID: 1=cat, 2=dog, 3=lizard, 4=snake, 5=bird, 6=hamster",
					required = true) Integer petTypeId) {

		logger.info("addPetToOwner: ownerId={}, name={}, birthDate={}, petTypeId={}", ownerId, name, birthDate,
				petTypeId);

		Owner owner = ownerRepository.findById(ownerId);
		if (owner == null) {
			throw new IllegalArgumentException("No owner found with id " + ownerId);
		}

		PetType petType = ownerRepository.findPetTypes()
			.stream()
			.filter(t -> petTypeId.equals(t.getId()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Invalid petTypeId " + petTypeId
					+ ". Valid IDs are 1-6 (cat, dog, lizard, snake, bird, hamster)."));

		Pet pet = new Pet();
		pet.setName(name);
		pet.setBirthDate(LocalDate.parse(birthDate));
		pet.setType(petType);

		owner.addPet(pet);
		this.ownerRepository.save(owner);
		return owner;
	}

}
