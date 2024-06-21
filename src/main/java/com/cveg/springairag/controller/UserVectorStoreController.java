package com.cveg.springairag.controller;

import com.cveg.springairag.service.UserVectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vectorstore")
public class UserVectorStoreController {

    @Autowired
    private UserVectorStoreService userVectorStoreService;

    @GetMapping("/documents")
    public List<String> getUserDocuments(@RequestParam String user) {
        return userVectorStoreService.getUserDocuments(user);
    }

    @DeleteMapping("/documents")
    public void deleteUserDocuments(@RequestParam List<String> documents, @RequestParam String user) {
        userVectorStoreService.deleteUserDocuments(documents, user);
    }

    //@Secured("ROLE_USER")
    @DeleteMapping("/documents/all")
    public void deleteAllDocuments(@RequestParam String user) {
        userVectorStoreService.deleteAllDocuments(user);
    }
}
