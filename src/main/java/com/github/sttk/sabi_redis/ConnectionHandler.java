/*
 * ConnectionHandler.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi_redis;

import com.github.sttk.errs.Err;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * The functional interface to handle a Redis connection.
 *
 * <p>This interface is used to register handlers that are executed at certain points in the
 * lifecycle of a {@link RedisDataConn}, such as pre-commit, post-commit, or force-back.
 */
@FunctionalInterface
public interface ConnectionHandler {
  /**
   * Handles the given Redis connection.
   *
   * @param connection a Redis connection.
   * @throws Err if an error occurs during handling.
   */
  void handle(StatefulRedisConnection<String, String> connection) throws Err;
}
