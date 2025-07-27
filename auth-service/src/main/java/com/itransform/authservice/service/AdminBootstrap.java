package com.itransform.authservice.service;

import com.itransform.authservice.config.DemoUserProperties;
import com.itransform.authservice.model.User;
import com.itransform.authservice.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;


@Component
public class AdminBootstrap {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DemoUserProperties demoUserProperties;

    @PostConstruct
    public void seedDemoUsers() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        for (DemoUserProperties.User demoUser : demoUserProperties.getUsers()) {
            if (userRepository.findByUsername(demoUser.getUsername()).isEmpty()) {
                User user = User.builder()
                        .username(demoUser.getUsername())
                        .password(encoder.encode(demoUser.getPassword()))
                        .role(demoUser.getRole())
                        .build();
                userRepository.save(user);
            }
        }
    }
}
