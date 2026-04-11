/*
 * RedisDataConn.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi_redis;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * The DataConn implementation for Redis in standalone configuration.
 *
 * <p>This class manages a connection to a Redis server and provides ways to register handlers that
 * are executed at certain points in the connection's lifecycle.
 */
public class RedisDataConn implements DataConn {

  // Fields

  private final StatefulRedisConnection<String, String> connection;
  private final List<ConnectionHandler> preCommits = new ArrayList<>();
  private final List<ConnectionHandler> postCommits = new ArrayList<>();
  private final List<ConnectionHandler> forceBacks = new ArrayList<>();

  // Constructors

  /**
   * Constructs a new RedisDataConn with the given Redis connection.
   *
   * @param connection a Redis connection.
   */
  protected RedisDataConn(StatefulRedisConnection<String, String> connection) {
    this.connection = connection;
  }

  // Methods

  /**
   * Returns the Redis connection.
   *
   * @return a Redis connection.
   */
  public StatefulRedisConnection<String, String> getConnection() {
    return this.connection;
  }

  /**
   * Adds a handler to be executed before the connection is committed.
   *
   * @param handler a connection handler.
   */
  public void addPreCommit(ConnectionHandler handler) {
    this.preCommits.add(handler);
  }

  /**
   * Adds a handler to be executed after the connection is committed.
   *
   * @param handler a connection handler.
   */
  public void addPostCommit(ConnectionHandler handler) {
    this.postCommits.add(handler);
  }

  /**
   * Adds a handler to be executed when the connection is forced to roll back.
   *
   * @param handler a connection handler.
   */
  public void addForceBack(ConnectionHandler handler) {
    this.forceBacks.add(handler);
  }

  /**
   * Executes pre-commit handlers.
   *
   * @param ag an asynchronous group.
   * @throws Err if an error occurs during pre-commit.
   */
  @Override
  public void preCommit(AsyncGroup ag) throws Err {
    for (var h : this.preCommits) {
      h.handle(this.connection);
    }
  }

  /**
   * Commits the connection. (Currently does nothing as Redis doesn't have a direct commit for a
   * simple connection)
   *
   * @param ag an asynchronous group.
   * @throws Err if an error occurs during commit.
   */
  @Override
  public void commit(AsyncGroup ag) throws Err {}

  /**
   * Executes post-commit handlers.
   *
   * @param ag an asynchronous group.
   */
  @Override
  public void postCommit(AsyncGroup ag) {
    for (var h : this.postCommits) {
      try {
        h.handle(this.connection);
      } catch (Err e) {
      } // Err will be thrown for error notification
    }
  }

  /**
   * Returns whether this connection should force back.
   *
   * @return true.
   */
  @Override
  public boolean shouldForceBack() {
    return true;
  }

  /**
   * Rolls back the connection. (Currently does nothing as Redis doesn't have a direct rollback for
   * a simple connection)
   *
   * @param ag an asynchronous group.
   */
  @Override
  public void rollback(AsyncGroup ag) {}

  /**
   * Executes force-back handlers.
   *
   * @param ag an asynchronous group.
   */
  @Override
  public void forceBack(AsyncGroup ag) {
    for (var h : this.forceBacks) {
      try {
        h.handle(this.connection);
      } catch (Err e) {
      } // Err will be thrown for error notification
    }
  }

  /** Closes the connection. */
  @Override
  public void close() {
    this.connection.close();
  }
}
