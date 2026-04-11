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
   * The error reason that indicates failing to create a {@link RedisClient} from a URI string.
   *
   * @param uri a URI string.
   */
  public record FailToCreateClientFromUriString(String uri) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} from a {@link URI}
   * object.
   *
   * @param uri a URI object.
   */
  public record FailToCreateClientFromURI(URI uri) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} from a {@link RedisURI}
   * object.
   *
   * @param redisURI a RedisURI object.
   */
  public record FailToCreateClientFromRedisURI(RedisURI redisURI) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} from a {@link
   * ClientResources} object and a URI string.
   *
   * @param clientResources a ClientResources object.
   * @param uri a URI string.
   */
  public record FailToCreateClientFromClientResourcesAndUriString(
      ClientResources clientResources, String uri) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} from a {@link
   * ClientResources} object and a {@link URI} object.
   *
   * @param clientResources a ClientResources object.
   * @param uri a URI object.
   */
  public record FailToCreateClientFromClientResourcesAndURI(
      ClientResources clientResources, URI uri) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} from a {@link
   * ClientResources} object and a {@link RedisURI} object.
   *
   * @param clientResources a ClientResources object.
   * @param redisURI a RedisURI object.
   */
  public record FailToCreateClientFromClientResourcesAndRedisURI(
      ClientResources clientResources, RedisURI redisURI) {}

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
    this.redisClientFactory = new RedisClientFactoryByUriString(uri);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link URI} object.
   *
   * @param uri a URI object.
   */
  public RedisDataSrc(URI uri) {
    this.redisClientFactory = new RedisClientFactoryByUriObject(uri);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link RedisURI} object.
   *
   * @param redisURI a RedisURI object.
   */
  public RedisDataSrc(RedisURI redisURI) {
    this.redisClientFactory = new RedisClientFactoryByRedisURI(redisURI);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link ClientResources} object and URI string.
   *
   * @param cr a ClientResources object.
   * @param uri a URI string.
   */
  public RedisDataSrc(ClientResources cr, String uri) {
    this.redisClientFactory = new RedisClientFactoryByClientResourcesAndUriString(cr, uri);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link ClientResources} object and {@link URI}
   * object.
   *
   * @param cr a ClientResources object.
   * @param uri a URI object.
   */
  public RedisDataSrc(ClientResources cr, URI uri) {
    this.redisClientFactory = new RedisClientFactoryByClientResourcesAndUriObject(cr, uri);
  }

  /**
   * Constructs a new RedisDataSrc with the given {@link ClientResources} object and {@link
   * RedisURI} object.
   *
   * @param cr a ClientResources object.
   * @param redisURI a RedisURI object.
   */
  public RedisDataSrc(ClientResources cr, RedisURI redisURI) {
    this.redisClientFactory = new RedisClientFactoryByClientResourcesAndRedisURI(cr, redisURI);
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

  private class RedisClientFactoryByUriString implements RedisClientFactory {
    final String uri;

    RedisClientFactoryByUriString(String uri) {
      this.uri = uri;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      try {
        var client = RedisClient.create(this.uri);
        try (var conn = client.connect()) {}
        return client;
      } catch (Exception e) {
        throw new Err(new FailToCreateClientFromUriString(this.uri), e);
      }
    }
  }

  private class RedisClientFactoryByUriObject implements RedisClientFactory {
    final URI uri;

    RedisClientFactoryByUriObject(URI uri) {
      this.uri = uri;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      try {
        var client = RedisClient.create(this.uri.toString());
        try (var conn = client.connect()) {}
        return client;
      } catch (Exception e) {
        throw new Err(new FailToCreateClientFromURI(this.uri), e);
      }
    }
  }

  private class RedisClientFactoryByRedisURI implements RedisClientFactory {
    final RedisURI redisURI;

    RedisClientFactoryByRedisURI(RedisURI redisURI) {
      this.redisURI = redisURI;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      try {
        var client = RedisClient.create(this.redisURI);
        try (var conn = client.connect()) {}
        return client;
      } catch (Exception e) {
        throw new Err(new FailToCreateClientFromRedisURI(this.redisURI), e);
      }
    }
  }

  private class RedisClientFactoryByClientResourcesAndUriString implements RedisClientFactory {
    final ClientResources clientResources;
    final String uri;

    RedisClientFactoryByClientResourcesAndUriString(ClientResources cr, String uri) {
      this.clientResources = cr;
      this.uri = uri;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      try {
        var client = RedisClient.create(this.clientResources, this.uri);
        try (var conn = client.connect()) {}
        return client;
      } catch (Exception e) {
        throw new Err(
            new FailToCreateClientFromClientResourcesAndUriString(this.clientResources, this.uri),
            e);
      }
    }
  }

  private class RedisClientFactoryByClientResourcesAndUriObject implements RedisClientFactory {
    final ClientResources clientResources;
    final URI uri;

    RedisClientFactoryByClientResourcesAndUriObject(ClientResources cr, URI uri) {
      this.clientResources = cr;
      this.uri = uri;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      try {
        var client = RedisClient.create(this.clientResources, this.uri.toString());
        try (var conn = client.connect()) {}
        return client;
      } catch (Exception e) {
        throw new Err(
            new FailToCreateClientFromClientResourcesAndURI(this.clientResources, this.uri), e);
      }
    }
  }

  private class RedisClientFactoryByClientResourcesAndRedisURI implements RedisClientFactory {
    final ClientResources clientResources;
    final RedisURI redisURI;

    RedisClientFactoryByClientResourcesAndRedisURI(ClientResources cr, RedisURI redisURI) {
      this.clientResources = cr;
      this.redisURI = redisURI;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      try {
        var client = RedisClient.create(this.clientResources, this.redisURI);
        try (var conn = client.connect()) {}
        return client;
      } catch (Exception e) {
        throw new Err(
            new FailToCreateClientFromClientResourcesAndRedisURI(
                this.clientResources, this.redisURI),
            e);
      }
    }
  }
}
