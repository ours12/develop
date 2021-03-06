package com.example.demo.config.handler;

import com.example.demo.model.ChatMessage;
import com.example.demo.repository.RedisRepository;
import com.example.demo.security.jwt.JwtDecoder;
import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final ChatService chatService;
    private final RedisRepository redisRepository;

    // websocket을 통해 들어온 요청이 처리 되기전 실행된다.
//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
//        if (StompCommand.CONNECT == accessor.getCommand()) { // websocket 연결요청
//            String jwtToken = accessor.getFirstNativeHeader("token");
//            log.info("CONNECT {}", jwtToken);
//            // Header의 jwt token 검증
//            jwtTokenProvider.validateToken(jwtToken);
//
//
//        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) { // 채팅룸 구독요청
//            // header정보에서 구독 destination정보를 얻고, roomId를 추출한다.
//            String roomId = chatService.getRoomId(Optional.ofNullable((String) message.getHeaders().get("simpDestination")).orElse("InvalidRoomId"));
//            // 채팅방에 들어온 클라이언트 sessionId를 roomId와 맵핑해 놓는다.(나중에 특정 세션이 어떤 채팅방에 들어가 있는지 알기 위함)
//            String sessionId = (String) message.getHeaders().get("simpSessionId");
//            redisRepository.setUserEnterInfo(sessionId, roomId);
//            // 채팅방의 인원수를 +1한다.
//            redisRepository.plusUserCount(roomId);
//            // 클라이언트 입장 메시지를 채팅방에 발송한다.(redis publish)
//            String name = Optional.ofNullable((Principal) message.getHeaders().get("simpUser")).map(Principal::getName).orElse("UnknownUser");
//            chatService.sendChatMessage(ChatMessage.builder().type(ChatMessage.MessageType.ENTER).roomId(roomId).sender(name).build());
//            log.info("SUBSCRIBED {}, {}", name, roomId);
//        } else if (StompCommand.DISCONNECT == accessor.getCommand()) { // Websocket 연결 종료
//            // 연결이 종료된 클라이언트 sesssionId로 채팅방 id를 얻는다.
//            String sessionId = (String) message.getHeaders().get("simpSessionId");
//            String roomId = redisRepository.getUserEnterRoomId(sessionId);
//            // 채팅방의 인원수를 -1한다.
//            redisRepository.minusUserCount(roomId);
//            // 클라이언트 퇴장 메시지를 채팅방에 발송한다.(redis publish)
//            String name = Optional.ofNullable((Principal) message.getHeaders().get("simpUser")).map(Principal::getName).orElse("UnknownUser");
//            chatService.sendChatMessage(ChatMessage.builder().type(ChatMessage.MessageType.QUIT).roomId(roomId).sender(name).build());
//            // 퇴장한 클라이언트의 roomId 맵핑 정보를 삭제한다.
//            redisRepository.removeUserEnterInfo(sessionId);
//            log.info("DISCONNECTED {}, {}", sessionId, roomId);
//        }
//        return message;
//    }
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        // websocket 연결시 헤더의 jwt token 검증
        if (StompCommand.CONNECT == accessor.getCommand()) {
            System.out.println("커넥션 진입");
            jwtDecoder.decodeUsername(accessor.getFirstNativeHeader("Authorization").substring(7));

        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            Long roomId = chatService.getRoomId(
                    Optional.ofNullable((String) message.getHeaders().get("simpDestination")).orElse("InvalidRoomId")
            );

            if (roomId != null) {
                String sessionId = (String) message.getHeaders().get("simpSessionId");
                redisRepository.plusUserCount(roomId);
                String name = jwtDecoder.decodeUsername(accessor.getFirstNativeHeader("Authorization").substring(7));
                chatService.sendChatMessage(ChatMessage.builder().type(ChatMessage.MessageType.ENTER).roomId(roomId).sender(name).build());
                redisRepository.setSessionUserInfo(sessionId, roomId, name);
                redisRepository.setUserChatRoomInOut(roomId + "_" + name, true);
                System.out.println("SUBSCRIBE 클라이언트 헤더" + message.getHeaders());
                System.out.println("SUBSCRIBE 클라이언트 세션 아이디" + sessionId);
                System.out.println("SUBSCRIBE 클라이언트 유저 이름: " + name);
//                chatService.sendChatMessage(ChatMessage.builder().type(ChatMessage.MessageType.ENTER).roomId(roomId).sender(name).build());


            }
        } else if (StompCommand.DISCONNECT == accessor.getCommand()) {
            String sessionId = (String) message.getHeaders().get("simpSessionId");
            String findInOutKey = redisRepository.getSessionUserInfo(sessionId);
            System.out.println("DISCONNECT 클라이언트 sessionId: " + sessionId);
            System.out.println("DISCONNECT 클라이언트 inoutKey: " + findInOutKey);
            Long roomId = chatService.getRoomId(
                    Optional.ofNullable((String) message.getHeaders().get("simpDestination")).orElse("InvalidRoomId")
            );

            redisRepository.minusUserCount(roomId);

            if (findInOutKey != null) {
                redisRepository.setUserChatRoomInOut(findInOutKey, false);
            }


            String name = Optional.ofNullable((Principal) message.getHeaders().get("simpUser")).map(Principal::getName).orElse("UnknownUser");
//            chatService.sendChatMessage(ChatMessage.builder().type(ChatMessage.MessageType.QUIT).roomId(roomId).sender(name).build());

            redisRepository.removeUserEnterInfo(sessionId);

        }

        return message;
    }
}
