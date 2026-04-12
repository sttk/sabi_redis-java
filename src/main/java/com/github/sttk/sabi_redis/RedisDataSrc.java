/*
 * RedisDataSrc.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi_redis;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import java.net.URI;

/**
 * The DataSrc implementation for Redis in standalone configuration.
 *
 * <p>This class is a factory for {@link RedisDataConn} and manages the lifecycle of the {@link
 * RedisClient}.
 */
public class RedisDataSrc implements DataSrc {

  // Error reasons

  /** The error reason that indicates the {@link RedisDataSrc} is not setup yet. */
  public record NotSetupYet() {}

  /** The error reason that indicates the {@link RedisDataSrc} is already setup. */
  public record AlreadySetup() {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} with a {@link
   * ClientResources} object and a URI string.
   *
   * @param clientResources a ClientResources object.
   * @param uri a URI string.
   */
  public record FailToCreateClientWithUriString(ClientResources clientResources, String uri) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} with a {@link
   * ClientResources} object and a {@link RedisURI} object.
   *
   * @param clientResources a ClientResources object.
   * @param redisURI a RedisURI object.
   */
  public record FailToCreateClientWithRedisURI(
      ClientResources clientResources, RedisURI redisURI) {}

  /**
   * The error reason that indicates failing to connect to a Redis server in standalone
   * configuration.
   *
   * @param clientResources a ClientResources object.
   * @param redisURI a RedisURI object.
   */
  public record FailToConnectToRedis(ClientResources clientResources, RedisURI redisURI) {}

  // Fields

  private RedisClientFactory redisClientFactory;
  private RedisClient redisClient;

  // Constructors

  /**
   * Constructs a new RedisDataSrc with the given URI string.
   *
   * @param uri a URI string.
   */
  public RedisDataSrc(String uri) {
    this.redisClientFactory = new RedisClientFactoryWithUriString(null, uri);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link URI} object.
   *
   * @param uri a URI object.
   */
  public RedisDataSrc(URI uri) {
    this.redisClientFactory = new RedisClientFactoryWithUriString(null, uri.toString());
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link RedisURI} object.
   *
   * @param redisURI a RedisURI object.
   */
  public RedisDataSrc(RedisURI redisURI) {
    this.redisClientFactory = new RedisClientFactoryWithRedisURI(null, redisURI);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link ClientResources} object and URI string.
   *
   * @param cr a ClientResources object.
   * @param uri a URI string.
   */
  public RedisDataSrc(ClientResources cr, String uri) {
    this.redisClientFactory = new RedisClientFactoryWithUriString(cr, uri);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link ClientResources} object and {@link URI}
   * object.
   *
   * @param cr a ClientResources object.
   * @param uri a URI object.
   */
  public RedisDataSrc(ClientResources cr, URI uri) {
    this.redisClientFactory = new RedisClientFactoryWithUriString(cr, uri.toString());
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link ClientResources} object and {@link
   * RedisURI} object.
   *
   * @param cr a ClientResources object.
   * @param redisURI a RedisURI object.
   */
  public RedisDataSrc(ClientResources cr, RedisURI redisURI) {
    this.redisClientFactory = new RedisClientFactoryWithRedisURI(cr, redisURI);
  }

  // Methods

  /**
   * Sets up this data source.
   *
   * <p>This method creates a {@link RedisClient} based on the parameters passed to the constructor.
   *
   * @param ag an asynchronous group.
   * @throws Err if an error occurs during setup.
   */
  @Override
  public void setup(AsyncGroup ag) throws Err {
    if (this.redisClientFactory == null || this.redisClient != null) {
      throw new Err(new AlreadySetup());
    }
    var factory = this.redisClientFactory;
    this.redisClientFactory = null;
    this.redisClient = factory.create();
  }

  /** Closes this data source and shuts down the {@link RedisClient}. */
  @Override
  public void close() {
    if (this.redisClient != null) {
      var redisClient = this.redisClient;
      this.redisClient = null;
      redisClient.shutdown();
    }
  }

  /**
   * Creates a new {@link DataConn} for Redis.
   *
   * @return a new RedisDataConn.
   * @throws Err if the data source is not setup yet.
   */
  @Override
  public DataConn createDataConn() throws Err {
    if (this.redisClient == null) {
      throw new Err(new NotSetupYet());
    }
    return new RedisDataConn(this.redisClient.connect());
  }

  /// Inner classes

  private interface RedisClientFactory {
    RedisClient create() throws Err;
  }

  private class RedisClientFactoryWithUriString implements RedisClientFactory {
    final ClientResources cr;
    final String uri;

    RedisClientFactoryWithUriString(ClientResources cr, String uri) {
      this.cr = cr;
      this.uri = uri;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      RedisClient client = null;
      try {
        if (this.cr == null) {
          client = RedisClient.create(this.uri);
        } else {
          client = RedisClient.create(this.cr, this.uri);
        }
      } catch (Exception e) {
        throw new Err(new FailToCreateClientWithUriString(this.cr, this.uri), e);
      }
      try (var conn = client.connect()) {
      } catch (Exception e) {
        client.shutdown();
        var redisURL = RedisURI.create(this.uri);
        throw new Err(new FailToConnectToRedis(this.cr, redisURL), e);
      }
      return client;
    }
  }

  private class RedisClientFactoryWithRedisURI implements RedisClientFactory {
    final ClientResources cr;
    final RedisURI redisURI;

    RedisClientFactoryWithRedisURI(ClientResources cr, RedisURI redisURI) {
      this.cr = cr;
      this.redisURI = redisURI;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      RedisClient client = null;
      try {
        if (this.cr == null) {
          client = RedisClient.create(this.redisURI);
        } else {
          client = RedisClient.create(this.cr, this.redisURI);
        }
      } catch (Exception e) {
        throw new Err(new FailToCreateClientWithRedisURI(this.cr, this.redisURI), e);
      }
      try (var conn = client.connect()) {
      } catch (Exception e) {
        client.shutdown();
        throw new Err(new FailToConnectToRedis(this.cr, this.redisURI), e);
      }
      return client;
    }
  }
}
