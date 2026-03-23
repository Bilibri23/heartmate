package org.rooms.roombuddy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class SecurityAuditService {
    public void logAction(String action, UUID actorId, UUID targetId, String resourceType) {
        log.info("AUDIT action={} actor={} target={} resourceType={}",
                action, actorId, targetId, resourceType);
    }
}
