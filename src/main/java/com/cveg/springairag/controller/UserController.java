package com.cveg.springairag.controller;

import com.cveg.springairag.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/page")
    public String createUserPage(@RequestParam String user) {
        if (user != null) user = user.trim().toLowerCase();
        else {
            return "User is madatory!";
        }
        try {
            return userService.generateUserScript(user);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return " ";
    }

}


