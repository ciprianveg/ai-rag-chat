package com.cveg.springairag.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserVectorStoreService {

    public static final String ANONYMOUS = "anonymous";
    public static final String JSON = ".json";
    public static final String DOCUMENTS_FOLDER = "documents";
    Map<String, SimpleVectorStore> userVectorStoreMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(UserVectorStoreService.class);

    @Autowired
    EmbeddingModel embeddingModel;

    public void addToUserVectorStore(Resource resource, String user) {
        if (StringUtils.isEmpty(user)) {
            user = ANONYMOUS;
        }
        if (resource == null) {
            return;
        }

        // Save the resource file to the user's documents folder
        boolean isNewResource = saveResourceFile(resource, user);

        File vectorStoreFile = new File(getVectorStorePath(user));
        SimpleVectorStore vectorStore = getOrBuildVectorStore(user, vectorStoreFile);
        if (isNewResource) {
            //  load the new documents and save the vector store
            loadResourceInVectorStore(resource, vectorStore);
            vectorStore.save(vectorStoreFile);

        }
    }

    private static void loadResourceInVectorStore(Resource resource, SimpleVectorStore vectorStore) {
        TikaDocumentReader documentReader = new TikaDocumentReader(resource);
        List<Document> documents = documentReader.get();
        TextSplitter textSplitter = new TokenTextSplitter();
        List<Document> splitDocuments = textSplitter.apply(documents);
        vectorStore.add(splitDocuments);
    }

    private SimpleVectorStore getOrBuildVectorStore(String user, File vectorStoreFile) {
        SimpleVectorStore vectorStore = userVectorStoreMap.get(user);
        if (vectorStore == null) {
            vectorStore = new SimpleVectorStore(embeddingModel);
            userVectorStoreMap.put(user, vectorStore);

            if (vectorStoreFile.exists()) {
                vectorStore.load(vectorStoreFile);
            } else {
                rebuildAndSaveUserVectorStore(user);
            }
        }
        return vectorStore;
    }

    public VectorStore getUserVectorStore(String user) {
        user = getUserOrAnonymous(user);
        File vectorStoreFile = new File(getVectorStorePath(user));
        return getOrBuildVectorStore(user, vectorStoreFile);

    }

    public List<String> getUserDocuments(String user) {
        user = getUserOrAnonymous(user);
        Path userDocsPath = Paths.get(System.getProperty("user.home"), "vectorStore", user, DOCUMENTS_FOLDER);
        List<String> documentsList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDocsPath)) {
            for (Path entry : stream) {
                documentsList.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return documentsList;
    }

    public void deleteUserDocuments(List<String> documents, String user) {
        user = getUserOrAnonymous(user);
        Path userDocsPath = Paths.get(System.getProperty("user.home"), "vectorStore", user, DOCUMENTS_FOLDER);
        for (String doc : documents) {
            try {
                Files.deleteIfExists(userDocsPath.resolve(doc));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        userVectorStoreMap.remove(user);
        rebuildAndSaveUserVectorStore(user);
    }

    public void deleteAllDocuments(String user) {
        user = getUserOrAnonymous(user);
        Path userDocsPath = Paths.get(System.getProperty("user.home"), "vectorStore", user, DOCUMENTS_FOLDER);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDocsPath)) {
            for (Path entry : stream) {
                Files.deleteIfExists(entry);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        userVectorStoreMap.remove(user);
        deleteVectorStoreFile(user);
    }

    private void rebuildAndSaveUserVectorStore(String user) {
        Path userDocsPath = Paths.get(System.getProperty("user.home"), "vectorStore", user, DOCUMENTS_FOLDER);
        File vectorStoreFile = new File(getVectorStorePath(user));
        userVectorStoreMap.remove(user);
        deleteVectorStoreFile(user);
        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
        userVectorStoreMap.put(user, vectorStore);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDocsPath)) {
            for (Path entry : stream) {
                Resource resource = new FileSystemResource(entry.toFile());
                loadResourceInVectorStore(resource, vectorStore);
            }
            vectorStore.save(vectorStoreFile);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private boolean saveResourceFile(Resource resource, String user) {
        user = getUserOrAnonymous(user);
        Path userDocsPath = Paths.get(System.getProperty("user.home"), "vectorStore", user, DOCUMENTS_FOLDER);
        if (!Files.exists(userDocsPath)) {
            try {
                Files.createDirectories(userDocsPath);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        try {
            Files.copy(resource.getInputStream(), userDocsPath.resolve(resource.getFilename()));
        } catch (FileAlreadyExistsException e) {
            logger.warn("File already exists: " + resource.getFilename());
            return false;

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return true;
    }

    private String getVectorStorePath(String user) {
        user = getUserOrAnonymous(user);
        String userHome = System.getProperty("user.home");

        // Get the Path object for the directory and file relative to the user's home directory
        Path directoryPath = Paths.get(userHome, "vectorStore", user);

        // Create the directory if it does not exist
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectories(directoryPath);
                System.out.println("Directory created: " + directoryPath.toString());
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        return directoryPath.resolve(user + "VectorStore" + JSON).toString();
    }

    private String getUserOrAnonymous(String user) {
        return StringUtils.isEmpty(user) ? ANONYMOUS : user;
    }

    private void deleteVectorStoreFile(String user) {
        Path vectorStorePath = Paths.get(System.getProperty("user.home"), "vectorStore", user, user + "VectorStore" + JSON);
        try {
            Files.deleteIfExists(vectorStorePath);
        } catch (IOException e) {
            logger.error("Failed to delete vector store file for user {}: {}", user, e.getMessage());
        }
    }
}
