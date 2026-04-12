/*
 * RedisSentinelDataSrc.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */
package com.github.sttk.sabi_redis.sentinel;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.AsyncGroup;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.DataSrc;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import java.net.URI;
import java.util.Arrays;

/**
 * The DataSrc implementation for Redis in Sentinel configuration.
 *
 * <p>This class is a factory for {@link RedisSentinelDataConn} and manages the lifecycle of the
 * {@link RedisClient}.
 */
public class RedisSentinelDataSrc implements DataSrc {

  // Error reasons

  /** The error reason that indicates the {@link RedisSentinelDataSrc} is already setup. */
  public record AlreadySetup() {}

  /** The error reason that indicates the {@link RedisSentinelDataSrc} is not setup yet. */
  public record NotSetupYet() {}

  /**
   * The error reason that indicates failing to create a {@link RedisURI} object from a URI string.
   *
   * @param uri a URI string.
   * @param index an index of the URI string in the array.
   */
  public record FailToCreateRedisURI(String uri, int index) {}

  /**
   * The error reason that indicates failing to use a {@link RedisURI} object.
   *
   * @param redisURI a RedisURI object.
   * @param index an index of the RedisURI object in the array.
   */
  public record BadRedisURI(RedisURI redisURI, int index) {}

  /**
   * The error reason that indicates the master ID is bad.
   *
   * @param masterId a master ID.
   */
  public record BadMasterId(String masterId) {}

  /**
   * The error reason that indicates failing to build a {@link RedisURI} object for Sentinel.
   *
   * @param builder a RedisURI builder.
   */
  public record FailToBuildSentinelRedisURI(RedisURI.Builder builder) {}

  /**
   * The error reason that indicates failing to create a {@link RedisClient} object.
   *
   * @param clientResources a ClientResources object.
   * @param redisURI a RedisURI object.
   */
  public record FailToCreateClient(ClientResources clientResources, RedisURI redisURI) {}

  /**
   * The error reason that indicates failing to connect to a Redis Sentinel.
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
   * Constructs a new RedisSentinelDataSrc with the given master ID and URI strings.
   *
   * @param masterId a master ID.
   * @param uris URI strings.
   */
  public RedisSentinelDataSrc(String masterId, String... uris) {
    this.redisClientFactory = new RedisClientFactoryWithUriStrings(null, masterId, uris);
  }

  /**
   * Constructs a new RedisSentinelDataSrc with the given master ID and {@link URI} objects.
   *
   * @param masterId a master ID.
   * @param uris URI objects.
   */
  public RedisSentinelDataSrc(String masterId, URI... uris) {
    var uriStrings = Arrays.stream(uris).map(uri -> uri.toString()).toArray(String[]::new);
    this.redisClientFactory = new RedisClientFactoryWithUriStrings(null, masterId, uriStrings);
  }

  /**
   * Constructs a new RedisSentinelDataSrc with the given master ID and {@link RedisURI} objects.
   *
   * @param masterId a master ID.
   * @param redisURIs RedisURI objects.
   */
  public RedisSentinelDataSrc(String masterId, RedisURI... redisURIs) {
    this.redisClientFactory = new RedisClientFactoryWithRedisURIs(null, masterId, redisURIs);
  }

  /**
   * Constructs a new RedisSentinelDataSrc with the given {@link ClientResources} object, master ID,
   * and URI strings.
   *
   * @param cr a ClientResources object.
   * @param masterId a master ID.
   * @param uris URI strings.
   */
  public RedisSentinelDataSrc(ClientResources cr, String masterId, String... uris) {
    this.redisClientFactory = new RedisClientFactoryWithUriStrings(cr, masterId, uris);
  }

  /**
   * Constructs a new RedisSentinelDataSrc with the given {@link ClientResources} object, master ID,
   * and {@link URI} objects.
   *
   * @param cr a ClientResources object.
   * @param masterId a master ID.
   * @param uris URI objects.
   */
  public RedisSentinelDataSrc(ClientResources cr, String masterId, URI... uris) {
    var uriStrings = Arrays.stream(uris).map(uri -> uri.toString()).toArray(String[]::new);
    this.redisClientFactory = new RedisClientFactoryWithUriStrings(cr, masterId, uriStrings);
  }

  /**
   * Constructs a new RedisSentinelDataSrc with the given {@link ClientResources} object, master ID,
   * and {@link RedisURI} objects.
   *
   * @param cr a ClientResources object.
   * @param masterId a master ID.
   * @param redisURIs RedisURI objects.
   */
  public RedisSentinelDataSrc(ClientResources cr, String masterId, RedisURI... redisURIs) {
    this.redisClientFactory = new RedisClientFactoryWithRedisURIs(cr, masterId, redisURIs);
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
   * Creates a new {@link DataConn} for Redis Sentinel.
   *
   * @return a new RedisSentinelDataConn.
   * @throws Err if the data source is not setup yet.
   */
  @Override
  public DataConn createDataConn() throws Err {
    if (this.redisClient == null) {
      throw new Err(new NotSetupYet());
    }
    return new RedisSentinelDataConn(this.redisClient.connect());
  }

  // Inner classes

  private interface RedisClientFactory {
    RedisClient create() throws Err;
  }

  private class RedisClientFactoryWithUriStrings implements RedisClientFactory {
    final ClientResources cr;
    final String masterId;
    final String[] uris;

    RedisClientFactoryWithUriStrings(ClientResources cr, String masterId, String[] uris) {
      this.cr = cr;
      this.masterId = masterId;
      this.uris = uris;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      RedisURI.Builder builder = null;
      if (this.uris.length == 0) {
        builder = RedisURI.builder();
      } else {
        RedisURI ru0 = null;
        try {
          ru0 = RedisURI.create(this.uris[0]);
        } catch (Exception e) {
          throw new Err(new FailToCreateRedisURI(this.uris[0], 0), e);
        }
        builder = RedisURI.builder(ru0);
        for (int i = 1; i < this.uris.length; i++) {
          RedisURI ru = null;
          try {
            ru = RedisURI.create(this.uris[i]);
          } catch (Exception e) {
            throw new Err(new FailToCreateRedisURI(this.uris[i], i), e);
          }
          builder.withSentinel(ru);
        }
      }
      try {
        builder.withSentinelMasterId(this.masterId);
      } catch (Exception e) {
        throw new Err(new BadMasterId(this.masterId), e);
      }

      RedisURI redisURI = null;
      try {
        redisURI = builder.build();
      } catch (Exception e) {
        throw new Err(new FailToBuildSentinelRedisURI(builder), e);
      }

      RedisClient client = null;
      try {
        if (this.cr == null) {
          client = RedisClient.create(redisURI);
        } else {
          client = RedisClient.create(this.cr, redisURI);
        }
      } catch (Exception e) {
        throw new Err(new FailToCreateClient(this.cr, redisURI), e);
      }

      try (var conn = client.connect()) {
      } catch (Exception e) {
        client.shutdown();
        throw new Err(new FailToConnectToRedis(this.cr, redisURI), e);
      }

      return client;
    }
  }

  private class RedisClientFactoryWithRedisURIs implements RedisClientFactory {
    final ClientResources cr;
    final String masterId;
    final RedisURI[] redisURIs;

    RedisClientFactoryWithRedisURIs(ClientResources cr, String masterId, RedisURI[] redisURIs) {
      this.cr = cr;
      this.masterId = masterId;
      this.redisURIs = redisURIs;
    }

    @Override
    @SuppressWarnings("try")
    public RedisClient create() throws Err {
      RedisURI.Builder builder = null;
      if (this.redisURIs.length == 0) {
        builder = RedisURI.builder();
      } else {
        try {
          builder = RedisURI.builder(this.redisURIs[0]);
        } catch (Exception e) {
          throw new Err(new BadRedisURI(this.redisURIs[0], 0), e);
        }
        for (int i = 1; i < this.redisURIs.length; i++) {
          try {
            builder.withSentinel(this.redisURIs[i]);
          } catch (Exception e) {
            throw new Err(new BadRedisURI(this.redisURIs[i], i), e);
          }
        }
      }
      try {
        builder.withSentinelMasterId(this.masterId);
      } catch (Exception e) {
        throw new Err(new BadMasterId(this.masterId), e);
      }

      RedisURI redisURI = null;
      try {
        redisURI = builder.build();
      } catch (Exception e) {
        throw new Err(new FailToBuildSentinelRedisURI(builder), e);
      }

      RedisClient client = null;
      try {
        if (this.cr == null) {
          client = RedisClient.create(redisURI);
        } else {
          client = RedisClient.create(this.cr, redisURI);
        }
      } catch (Exception e) {
        throw new Err(new FailToCreateClient(this.cr, redisURI), e);
      }

      try (var conn = client.connect()) {
      } catch (Exception e) {
        client.shutdown();
        throw new Err(new FailToConnectToRedis(this.cr, redisURI), e);
      }

      return client;
    }
  }
}
