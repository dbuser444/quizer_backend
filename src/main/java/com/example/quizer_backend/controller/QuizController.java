/*
package com.example.quizer_backend.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class QuizController {

    // Когда препод отправляет сюда данные, они рассылаются всем подписчикам топика
    @MessageMapping("/session/{pin}/next-question")
    @SendTo("/topic/session/{pin}")
    public Map<String, Object> broadcastQuestion(@DestinationVariable String pin, Map<String, Object> question) {
        System.out.println("Вопрос отправлен в сессию " + pin + ": " + question.get("questionText"));
        return question;
    }
}*/
