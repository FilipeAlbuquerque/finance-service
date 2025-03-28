package com.example.financeservice.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginAttemptService {

  private LoadingCache<String, Integer> attemptsCache;
  private static final int MAX_ATTEMPTS = 5;

  @PostConstruct
  public void init() {
    attemptsCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(new CacheLoader<>() {
          @Override
          public Integer load(String key) {
            return 0;
          }
        });
  }

  public void loginSucceeded(String username) {
    attemptsCache.invalidate(username);
  }

  public void loginFailed(String username) {
    int attempts = 0;
    try {
      attempts = attemptsCache.get(username);
    } catch (ExecutionException e) {
      log.error("Error getting login attempts", e);
    }
    attempts++;
    attemptsCache.put(username, attempts);
    log.warn("Failed login attempt {} for user {}", attempts, username);
  }

  public boolean isBlocked(String username) {
    try {
      return attemptsCache.get(username) >= MAX_ATTEMPTS;
    } catch (ExecutionException e) {
      return false;
    }
  }
}
