package com.github.sttk.sabi_redis;

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

public class StandaloneTest {
  private StandaloneTest() {}

  @Test
  void test_NewRedisDataSrcWithUriString() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc("redis://127.0.0.1:6379/0"));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithUriStringButInvalidAddr() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc("xxxx"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientWithUriString reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.clientResources()).isNull();
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
  void test_NewRedisDataSrcWithUriStringButNotFoundAddr() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc("redis://127.0.0.1:9999/0"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.redisURI().getDatabase()).isEqualTo(0);
              assertThat(reason2.clientResources()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Unable to connect to 127.0.0.1/<unresolved>:9999");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisDataSrcWithURI() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(new URI("redis://127.0.0.1:6379/0")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } catch (URISyntaxException e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithURIButInvalidAddr() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(new URI("xxxx")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientWithUriString reason2 -> {
              assertThat(reason2.uri().toString()).isEqualTo("xxxx");
              assertThat(reason2.clientResources()).isNull();
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
  void test_NewRedisDataSrcWithURIButNotFoundAddr() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(new URI("redis://127.0.0.1:9999/0")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.redisURI().getDatabase()).isEqualTo(0);
              assertThat(reason2.clientResources()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Unable to connect to 127.0.0.1/<unresolved>:9999");
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
  void test_NewRedisDataSrcWithRedisURI() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(RedisURI.create("redis://127.0.0.1:6379/0")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithRedisURIButInvalidAddr() {
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(RedisURI.create("redis://127.0.0.1:9999/1")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.redisURI().getDatabase()).isEqualTo(1);
              assertThat(reason2.clientResources()).isNull();
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Unable to connect to 127.0.0.1/<unresolved>:9999");
            }
            default -> fail(err);
          }
        }
        default -> fail(err);
      }
    }
  }

  @Test
  void test_NewRedisDataSrcWithClientResourcesAndUriString() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, "redis://127.0.0.1:6379/0"));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisDataSrcWithClientResourcesAndUriStringButInvalidAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, "xxxx"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientWithUriString reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
              assertThat(reason2.clientResources()).isEqualTo(cr);
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
  void test_NewRedisDataSrcWithClientResourcesAndUriStringButNotFoundAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, "redis://127.0.0.1:9999/0"));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.redisURI().getDatabase()).isEqualTo(0);
              assertThat(reason2.clientResources()).isEqualTo(cr);
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Unable to connect to 127.0.0.1/<unresolved>:9999");
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
  void test_NewRedisDataSrcWithClientResourcesAndURI() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, new URI("redis://127.0.0.1:6379/0")));
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
  void test_NewRedisDataSrcWithClientResourcesAndURIButInvalidAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, new URI("xxxx")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientWithUriString reason2 -> {
              assertThat(reason2.uri().toString()).isEqualTo("xxxx");
              assertThat(reason2.clientResources()).isEqualTo(cr);
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
  void test_NewRedisDataSrcWithClientResourcesAndURIButNotFoundAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, new URI("redis://127.0.0.1:9999/0")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.redisURI().getDatabase()).isEqualTo(0);
              assertThat(reason2.clientResources()).isEqualTo(cr);
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Unable to connect to 127.0.0.1/<unresolved>:9999");
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
  void test_NewRedisDataSrcWithClientResourcesAndRedisURI() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, RedisURI.create("redis://127.0.0.1:6379/0")));
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    } finally {
      cr.shutdown();
    }
  }

  @Test
  void test_NewRedisDataSrcWithClientResourcesAndRedisURIButNotFoundAddr() {
    var cr = DefaultClientResources.create();
    try (var data = new DataHub()) {
      data.uses("redis", new RedisDataSrc(cr, RedisURI.create("redis://127.0.0.1:9999/1")));
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToConnectToRedis reason2 -> {
              assertThat(reason2.redisURI().getHost()).isEqualTo("127.0.0.1");
              assertThat(reason2.redisURI().getPort()).isEqualTo(9999);
              assertThat(reason2.redisURI().getDatabase()).isEqualTo(1);
              assertThat(reason2.clientResources()).isEqualTo(cr);
              assertThat(err2.getCause().toString())
                  .isEqualTo(
                      "io.lettuce.core.RedisConnectionException: Unable to connect to 127.0.0.1/<unresolved>:9999");
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
    void testRedisDataConn() {
      var conn = new RedisDataConn(null);
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
      var ds = new RedisDataSrc("redis://127.0.0.1:6379/0");
      try {
        ds.createDataConn();
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isEqualTo(new RedisDataSrc.NotSetupYet());
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
        assertThat(err.getReason()).isEqualTo(new RedisDataSrc.AlreadySetup());
      }
      ds.close();
    }
  }
}
