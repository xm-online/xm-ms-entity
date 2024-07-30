package com.icthh.xm.ms.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Test {

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);

        // Configure ObjectMapper to manage circular references
        mapper.enable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        mapper.enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL);

        Person alice = new Person("Alice", null);
        Friend bob = new Friend("Bob", alice);
        alice.friend = bob;  // Create a self-reference

        String jsonString = mapper.writeValueAsString(bob);

        System.out.println("------------- RESULT -------------");
        System.out.println("Bob: " + mapper.writeValueAsString(bob));
        System.out.println("Alice: " + mapper.writeValueAsString(alice));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private Friend friend;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name")
    public static class Friend {
        private String name;
        private Person friend;
    }
}
