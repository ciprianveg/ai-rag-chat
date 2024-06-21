package com.cveg.springairag.controller;

import com.cveg.springairag.Question;
import com.cveg.springairag.service.UserVectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/ai")
public class AskController {

    @Autowired
    private ChatClient chatClient;
    @Autowired
    private UserVectorStoreService userVectorStoreService;
    @Autowired
    private OllamaChatModel chatModel;


    @Value("classpath:/rag-prompt-template.st")
    private Resource ragPromptTemplate;
    private static final Logger logger = LoggerFactory.getLogger(AskController.class);


    @PostMapping("/document-chat-stream2")
    public Flux<String> askStream2(@RequestBody Question question, @RequestParam String user) {
        long t0 = System.currentTimeMillis();
        if (user != null) user = user.trim().toLowerCase();
        try {
            VectorStore vectorStore = userVectorStoreService.getUserVectorStore(user);
            List<Document> similarDocuments = vectorStore
                    .similaritySearch(SearchRequest.query(question.question())
                            .withTopK(3));
            logger.info("vectorStore search time: {}ms", System.currentTimeMillis() - t0);

            List<String> contentList = similarDocuments.stream()
                    .map(Document::getContent)
                    .toList();
            // Create the prompt with the necessary parameters
            String prompt = question.question();
            logger.info("stream docs search time: {}ms", System.currentTimeMillis() - t0);

            prompt = ragPromptTemplate.getContentAsString(StandardCharsets.UTF_8)
                    .replace("{input}", question.question())
                    .replace("{documents}", String.join("\n", contentList));


            // Return the stream of responses from chatModel
            return chatModel.stream(prompt);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return new Flux<String>() {
            @Override
            public void subscribe(CoreSubscriber<? super String> coreSubscriber) {

            }
        };
    }

    @PostMapping("/document-chat-stream3")
    public String askStream3(@RequestBody Question question, @RequestParam String user) {
        long t0 = System.currentTimeMillis();
        if (user != null) user = user.trim().toLowerCase();
        try {
            VectorStore vectorStore = userVectorStoreService.getUserVectorStore(user);
            List<Document> similarDocuments = vectorStore
                    .similaritySearch(SearchRequest.query(question.question())
                            .withTopK(3));
            String information = similarDocuments.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining(System.lineSeparator()));
            logger.info("vectorStore search time: {}ms", System.currentTimeMillis() - t0);
            var systemPromptTemplate = new SystemPromptTemplate("""
                     You are a helpful assistant.  You answer using the Romanian language. Raspunde succint si la obiect.
                     If you do not find the required information in the text below, answer: Nu am gasit informatia ceruta in documentatia mea.
                                  
                    Information:
                     {information}
                     """);
            var systemMessage = systemPromptTemplate.createMessage(
                    Map.of("information", information));

            var userMessage = new UserMessage(question.question());
            var prompt = new Prompt(List.of(systemMessage, userMessage));

            return chatClient.prompt(prompt).call().content();

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return " ";
    }

    @PostMapping("/document-chat-stream")
    public Flux<String> askStream(@RequestBody Question question, @RequestParam String user) {
        long t0 = System.currentTimeMillis();
        if (user != null) user = user.trim().toLowerCase();
        try {
            VectorStore vectorStore = userVectorStoreService.getUserVectorStore(user);
            List<Document> similarDocuments = vectorStore
                    .similaritySearch(SearchRequest.query(question.question())
                            .withTopK(5));
            String information = similarDocuments.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining(System.lineSeparator()));
            logger.info("vectorStore search time: {}ms", System.currentTimeMillis() - t0);
            var systemPromptTemplate = new SystemPromptTemplate("""
                     You are a helpful assistant.  You answer using the Romanian language. Raspunde succint si la obiect.
                     If you do not find the required information in the text below, answer: Nu am gasit informatia ceruta in documentatia mea.
                                  
                    Information:
                     {information}
                     """);
            var systemMessage = systemPromptTemplate.createMessage(
                    Map.of("information", information));

            var userMessage = new UserMessage(question.question());
            var prompt = new Prompt(List.of(systemMessage, userMessage));

            return chatClient.prompt(prompt).stream().chatResponse()
                    .map(response -> response.getResult().getOutput().getContent());

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new Flux<String>() {
            @Override
            public void subscribe(CoreSubscriber<? super String> coreSubscriber) {

            }
        };
    }

    @GetMapping("/chat")
    public Map generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Map.of("generation", chatModel.call(message));
    }

    @GetMapping("/chat-stream")
    public Flux<String> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        try {
            Prompt prompt = new Prompt(new UserMessage(message));
            return chatModel.stream(prompt.toString());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new Flux<String>() {
            @Override
            public void subscribe(CoreSubscriber<? super String> coreSubscriber) {

            }
        };
    }

    @Secured("ROLE_USER")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("user") String user) {
        try {
            if (user != null) user = user.trim().toLowerCase();

            if (file.isEmpty()) {
                logger.warn("File upload failed: empty file provided by user {}", user);
                return new ResponseEntity<>("File is empty", HttpStatus.BAD_REQUEST);
            }

            Resource resource = file.getResource();
            userVectorStoreService.addToUserVectorStore(resource, user);
            logger.info("File uploaded successfully for user {}", user);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("File upload failed for user {}: {}", user, e.getMessage());
            return new ResponseEntity<>("File upload failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Secured("ROLE_USER")
    @PostMapping(value = "/upload-url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> uploadUrl(@RequestParam("url") String urlString, @RequestParam("user") String user) {
        try {
            if (user != null) user = user.trim().toLowerCase();

            URL url = new URL(urlString);
            UrlResource resource = new UrlResource(url);

            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("URL resource creation failed: resource not found or not readable for URL provided by user {}", user);
                return new ResponseEntity<>("URL resource is not accessible", HttpStatus.BAD_REQUEST);
            }

            userVectorStoreService.addToUserVectorStore(resource, user);
            logger.info("URL resource uploaded successfully for user {}", user);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (MalformedURLException e) {
            logger.error("Invalid URL provided by user {}: {}", user, e.getMessage());
            return new ResponseEntity<>("Invalid URL", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("URL resource upload failed for user {}: {}", user, e.getMessage());
            return new ResponseEntity<>("URL resource upload failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/user-page")
    public String createUserPage(@RequestBody Question question, @RequestParam String user) {
        long t0 = System.currentTimeMillis();
        if (user != null) user = user.trim().toLowerCase();
        try {
            VectorStore vectorStore = userVectorStoreService.getUserVectorStore(user);
            List<Document> similarDocuments = vectorStore
                    .similaritySearch(SearchRequest.query(question.question())
                            .withTopK(3));
            String information = similarDocuments.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining(System.lineSeparator()));
            logger.info("vectorStore search time: {}ms", System.currentTimeMillis() - t0);
            var systemPromptTemplate = new SystemPromptTemplate("""
                     You are a helpful assistant.  You answer using the Romanian language. Raspunde succint si la obiect.
                     If you do not find the required information in the text below, answer: Nu am gasit informatia ceruta in documentatia mea.
                                  
                    Information:
                     {information}
                     """);
            var systemMessage = systemPromptTemplate.createMessage(
                    Map.of("information", information));

            var userMessage = new UserMessage(question.question());
            var prompt = new Prompt(List.of(systemMessage, userMessage));


            //return chatClient.prompt(prompt).stream().content();
            // return chatClient.prompt(prompt).call().content();
            return chatClient.prompt(prompt).call().content();

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return " ";
    }

}


