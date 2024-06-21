package com.cveg.springairag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Value("${static.resources.dir}")
    private String resourcesDir;

    /**
     * starting from index-user-template.html, index-template.html, ai-assistant-template.js existing in static.resources.dir directory
     * create a copy of each replacing "template" with userName in the files title and in their content. Replace them if already exist.
     * returns https://themain.ro/index-user-{userName}.html
     */
    public String generateUserScript(String userName) throws IOException {
        Path sourceDirectory = Paths.get(this.resourcesDir);
        Path targetIndexUserFile = sourceDirectory.resolve("index-user-" + userName + ".html");
        Path targetIndexFile = sourceDirectory.resolve("index-" + userName + ".html");
        Path targetAIScriptFile = sourceDirectory.resolve("ai-assistant-" + userName + ".js");

        replaceContentInFile(sourceDirectory.resolve("index-template.html"), targetIndexFile, "template", userName);
        replaceContentInFile(sourceDirectory.resolve("index-user-template.html"), targetIndexUserFile, "template", userName);
        replaceContentInFile(sourceDirectory.resolve("ai-assistant-template.js"), targetAIScriptFile, "template", userName);

        return "https://themain.ro/index-user-" + userName + ".html";
    }

    private void replaceContentInFile(Path sourceFile, Path targetFile, String searchText, String replacement) throws IOException {
        String content = new String(Files.readAllBytes(sourceFile));
        content = content.replaceAll(searchText, replacement);
        Files.write(targetFile, content.getBytes());
    }
}
