package com.example.demo.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
public class ChatMessage {


    public ChatMessage() {
    }

    @Builder
    public ChatMessage(MessageType type, Long roomId, String sender, String message, long userCount) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.userCount = userCount;
    }

    @Builder
    public ChatMessage(MessageType type, Long roomId, String sender, String message, long userCount, String candidate, String sdp) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.userCount = userCount;
        this.candidate = candidate;
        this.sdp = sdp;
    }

    // 메시지 타입 : 입장, 퇴장, 채팅
    public enum MessageType {
        ENTER, QUIT, TALK, TEXT, OFFER, ANSWER, ICE, LEAVE
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;


    private MessageType type; // 메시지 타입

    @Column
    private Long roomId; // 방번호

    @Column
    private String sender; // 메시지 보낸사람

    @Column
    private String message; // 메시지

    @Column
    private long userCount; // 채팅방 인원수, 채팅방 내에서 메시지가 전달될때 인원수 갱신시 사용


    private String candidate;

    private String sdp;
}
