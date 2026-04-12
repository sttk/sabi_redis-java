package com.github.sttk.sabi_redis.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataHub;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.DefaultClientResources;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
public class SentinelTest {
  private SentinelTest() {}

  @Test
  void test_NewRedisSentinelDataSrcWithUriStrings() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481"));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithUriStringsButInvalidAddr_indexIs0() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster", "xxxx", "redis://127.0.0.1:26480", "redis://127.0.0.1:26481"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToCreateRedisURI reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.index()).isEqualTo(0);
              assertThat(err2.getCause().toString())
                  .isEqualTo("java.lang.IllegalArgumentException: URI scheme must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithUriStringsButInvalidAddr_indexIs1() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster", "redis://127.0.0.1:26479", "xxxx", "redis://127.0.0.1:26481"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToCreateRedisURI reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.index()).isEqualTo(1);
              assertThat(err2.getCause().toString())
                  .isEqualTo("java.lang.IllegalArgumentException: URI scheme must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithUriStringsButAddrIsZero() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisSentinelDataSrc("mymaster", new String[0]));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToBuildSentinelRedisURI reason2 -> {
              assertThat(reason2.builder()).isNotNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "java.lang.IllegalStateException: Cannot build a RedisURI. One of the following must be provided Host, Socket or Sentinel");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithUriStringsButNotFoundAddr() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              "redis://127.0.0.1:9999",
              "redis://127.0.0.1:9998",
              "redis://127.0.0.1:9997"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.clientResources()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Cannot connect to a Redis Sentinel: [redis://127.0.0.1:9998, redis://127.0.0.1:9997]");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithUriStringsButMasterIdIsNull() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              (String) null,
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.BadMasterId reason2 -> {
              assertThat(reason2.masterId()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "java.lang.IllegalArgumentException: Sentinel master id must not empty");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithURIs() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              new URI("redis://127.0.0.1:26479"),
              new URI("redis://127.0.0.1:26480"),
              new URI("redis://127.0.0.1:26481")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } catch (URISyntaxException e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithURIsButInvalidAddr() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              new URI("redis://127.0.0.1:26479"),
              new URI("xxxx"),
              new URI("redis://127.0.0.1:26481")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToCreateRedisURI reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.index()).isEqualTo(1);
              assertThat(err2.getCause().toString())
                  .isEqualTo("java.lang.IllegalArgumentException: URI scheme must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } catch (URISyntaxException e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithURIsButNotFoundAddr() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              new URI("redis://127.0.0.1:9999"),
              new URI("redis://127.0.0.1:9998"),
              new URI("redis://127.0.0.1:9997")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.clientResources()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Cannot connect to a Redis Sentinel: [redis://127.0.0.1:9998, redis://127.0.0.1:9997]");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } catch (URISyntaxException e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithRedisURIs() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              RedisURI.create("redis://127.0.0.1:26479"),
              RedisURI.create("redis://127.0.0.1:26480"),
              RedisURI.create("redis://127.0.0.1:26481")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithRedisURIsButInvalidAddr_indexIs0() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              (RedisURI) null,
              RedisURI.create("redis://127.0.0.1:26480"),
              RedisURI.create("redis://127.0.0.1:26481")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.BadRedisURI reason2 -> {
              assertThat(reason2.redisURI()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "java.lang.IllegalArgumentException: Source RedisURI must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithRedisURIsButInvalidAddr_indexIs1() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              RedisURI.create("redis://127.0.0.1:26479"),
              (RedisURI) null,
              RedisURI.create("redis://127.0.0.1:26481")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.BadRedisURI reason2 -> {
              assertThat(reason2.redisURI()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo("java.lang.IllegalArgumentException: Redis URI must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithRedisURIsButAddrIsZero() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisSentinelDataSrc("mymaster", new RedisURI[0]));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToBuildSentinelRedisURI reason2 -> {
              assertThat(reason2.builder()).isNotNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "java.lang.IllegalStateException: Cannot build a RedisURI. One of the following must be provided Host, Socket or Sentinel");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithRedisURIsButNotFoundAddr() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              RedisURI.create("redis://127.0.0.1:9999"),
              RedisURI.create("redis://127.0.0.1:9998"),
              RedisURI.create("redis://127.0.0.1:9997")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.clientResources()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Cannot connect to a Redis Sentinel: [redis://127.0.0.1:9998, redis://127.0.0.1:9997]");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithRedisURIsButMasterIdIsNull() {
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              (String) null,
              RedisURI.create("redis://127.0.0.1:26479"),
              RedisURI.create("redis://127.0.0.1:26480"),
              RedisURI.create("redis://127.0.0.1:26481")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.BadMasterId reason2 -> {
              assertThat(reason2.masterId()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "java.lang.IllegalArgumentException: Sentinel master id must not empty");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndUriStrings() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481"));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndUriStringsButInvalidAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr, "mymaster", "redis://127.0.0.1:26479", "xxxx", "redis://127.0.0.1:26481"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToCreateRedisURI reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.index()).isEqualTo(1);
              assertThat(err2.getCause().toString())
                  .isEqualTo("java.lang.IllegalArgumentException: URI scheme must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndUriStringsButNotFoundAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              "redis://127.0.0.1:9999",
              "redis://127.0.0.1:9998",
              "redis://127.0.0.1:9997"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.clientResources()).isNotNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Cannot connect to a Redis Sentinel: [redis://127.0.0.1:9998, redis://127.0.0.1:9997]");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndURIs() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              new URI("redis://127.0.0.1:26479"),
              new URI("redis://127.0.0.1:26480"),
              new URI("redis://127.0.0.1:26481")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } catch (URISyntaxException e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndURIsButInvalidAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              new URI("redis://127.0.0.1:26479"),
              new URI("xxxx"),
              new URI("redis://127.0.0.1:26481")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToCreateRedisURI reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.index()).isEqualTo(1);
              assertThat(err2.getCause().toString())
                  .isEqualTo("java.lang.IllegalArgumentException: URI scheme must not be null");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } catch (URISyntaxException e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndURIsButNotFoundAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              new URI("redis://127.0.0.1:9999"),
              new URI("redis://127.0.0.1:9998"),
              new URI("redis://127.0.0.1:9997")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.clientResources()).isNotNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Cannot connect to a Redis Sentinel: [redis://127.0.0.1:9998, redis://127.0.0.1:9997]");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } catch (URISyntaxException e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndRedisURIs() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              RedisURI.create("redis://127.0.0.1:26479"),
              RedisURI.create("redis://127.0.0.1:26480"),
              RedisURI.create("redis://127.0.0.1:26481")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisSentinelDataSrcWithClientResourcesAndRedisURIsButNotFoundAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              cr,
              "mymaster",
              RedisURI.create("redis://127.0.0.1:9999"),
              RedisURI.create("redis://127.0.0.1:9998"),
              RedisURI.create("redis://127.0.0.1:9997")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisSentinelDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.clientResources()).isNotNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Cannot connect to a Redis Sentinel: [redis://127.0.0.1:9998, redis://127.0.0.1:9997]");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    } finally {
      cr.shutdown();
    }
  }

  @Nested
  class ForCoverage {
    @Test
    void testRedisSentinelDataConn() {
      var conn = new RedisSentinelDataConn(null);
      conn.rollback(null);

      conn.addPreCommit(
          _conn -> {
            throw new Err("bad");
          });
      try {
        conn.preCommit(null);
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isEqualTo("bad");
      }

      conn.addPostCommit(
          _conn -> {
            throw new Err("bad");
          });
      conn.postCommit(null);

      conn.addForceBack(
          _conn -> {
            throw new Err("bad");
          });
      conn.forceBack(null);
    }

    @Test
    void testRedisDataSrc() {
      var ds =
          new RedisSentinelDataSrc(
              "mymaster",
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481");
      try {
        ds.createDataConn();
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isEqualTo(new RedisSentinelDataSrc.NotSetupYet());
      }
      ds.close();

      try {
        ds.setup(null);
      } catch (Err err) {
        fail(err);
      }
      try {
        ds.setup(null);
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isEqualTo(new RedisSentinelDataSrc.AlreadySetup());
      }
      ds.close();
    }
  }
}
