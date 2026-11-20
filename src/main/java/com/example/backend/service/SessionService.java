package com.example.backend.service;

import com.example.backend.exception.BusinessException;
import com.example.backend.model.SessionInfo;
import com.example.backend.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String DEFAULT_TITLE = "New Chat";
    private static final int AUTO_TITLE_MAX_LENGTH = 30;

    private final SessionRepository sessionRepository;

    public List<SessionInfo> listSessions() {
        return sessionRepository.findAll();
    }

    public Optional<SessionInfo> findById(String id) {
        return sessionRepository.findById(id);
    }

    public SessionInfo requireSession(String id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("会话不存在"));
    }

    public SessionInfo createSession(String title) {
        String id = UUID.randomUUID().toString();
        sessionRepository.createIfNotExists(id);
        sessionRepository.updateTitle(id, title);
        log.info("[Session] created id={}, title={}", id, title);
        return sessionRepository.findById(id).orElseThrow();
    }

    public void deleteSession(String id) {
        log.info("[Session] delete id={}", id);
        sessionRepository.deleteById(id);
    }

    /**
     * 确保会话存在；若是默认标题的空会话，则用首条消息自动生成标题。
     */
    public void ensureSession(String sessionId, String firstMessage) {
        sessionRepository.createIfNotExists(sessionId);
        Optional<SessionInfo> session = sessionRepository.findById(sessionId);
        if (session.isPresent() && DEFAULT_TITLE.equals(session.get().getTitle())) {
            String title = firstMessage.length() > AUTO_TITLE_MAX_LENGTH
                    ? firstMessage.substring(0, AUTO_TITLE_MAX_LENGTH) + "..."
                    : firstMessage;
            sessionRepository.updateTitle(sessionId, title);
        }
    }

    public void touchUpdatedAt(String sessionId) {
        sessionRepository.touchUpdatedAt(sessionId);
    }
}
