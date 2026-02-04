package org.springframework.samples.petclinic.books;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for the Books tab, allowing users to view and embed the veterinary guide
 * PDF.
 *
 * @author Oded Shopen
 */
@Controller
@Profile({ "openai" })
public class BooksController {

	private static final String PDF_RESOURCE_PATH = "AH_vet_guide.cleaned.pdf";

	private static final String SOURCE_METADATA_KEY = "source";

	private static final String SOURCE_METADATA_VALUE = "vet_guide";

	private final Logger logger = LoggerFactory.getLogger(BooksController.class);

	private final VectorStore vectorStore;

	public BooksController(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	/**
	 * Check if the vet guide is already embedded in the vector store by querying for
	 * documents with the vet_guide source metadata.
	 */
	private boolean isAlreadyEmbedded() {
		try {
			FilterExpressionBuilder builder = new FilterExpressionBuilder();
			SearchRequest searchRequest = SearchRequest.builder()
				.query("veterinary guide")
				.topK(1)
				.filterExpression(builder.eq(SOURCE_METADATA_KEY, SOURCE_METADATA_VALUE).build())
				.build();

			List<Document> results = vectorStore.similaritySearch(searchRequest);
			return !results.isEmpty();
		}
		catch (Exception e) {
			logger.warn("Could not check if already embedded: {}", e.getMessage());
			return false;
		}
	}

	@GetMapping("/books")
	public String showBooks(Model model) {
		Resource pdfResource = new ClassPathResource(PDF_RESOURCE_PATH);
		model.addAttribute("pdfExists", pdfResource.exists());
		model.addAttribute("pdfName", PDF_RESOURCE_PATH);
		model.addAttribute("isEmbedded", isAlreadyEmbedded());
		return "books/books";
	}

	@GetMapping(value = "/books/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	@ResponseBody
	public ResponseEntity<Resource> servePdf() {
		Resource pdfResource = new ClassPathResource(PDF_RESOURCE_PATH);
		if (!pdfResource.exists()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdfResource);
	}

	@PostMapping("/books/embed")
	@ResponseBody
	public ResponseEntity<String> embedPdf() {
		try {
			// Check vector store to see if already embedded
			if (isAlreadyEmbedded()) {
				logger.info("PDF already embedded in vector store, skipping re-embedding");
				return ResponseEntity.ok("Document already embedded - no action needed");
			}

			Resource pdfResource = new ClassPathResource(PDF_RESOURCE_PATH);
			if (!pdfResource.exists()) {
				return ResponseEntity.badRequest().body("PDF file not found");
			}

			logger.info("Starting PDF embedding for: {}", PDF_RESOURCE_PATH);

			// Read the PDF document
			PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
			List<Document> documents = pdfReader.get();

			// Split into chunks for better embedding
			TokenTextSplitter textSplitter = new TokenTextSplitter();
			List<Document> chunks = textSplitter.apply(documents);

			logger.info("PDF split into {} chunks", chunks.size());

			// Add metadata to identify these documents as coming from the vet guide
			for (Document chunk : chunks) {
				chunk.getMetadata().put(SOURCE_METADATA_KEY, SOURCE_METADATA_VALUE);
				chunk.getMetadata().put("filename", PDF_RESOURCE_PATH);
			}

			// Add to vector store
			vectorStore.add(chunks);

			logger.info("Successfully embedded {} document chunks to vector store", chunks.size());

			return ResponseEntity.ok("Successfully embedded " + chunks.size() + " document chunks");
		}
		catch (Exception e) {
			logger.error("Error embedding PDF", e);
			return ResponseEntity.internalServerError().body("Error embedding PDF: " + e.getMessage());
		}
	}

}
