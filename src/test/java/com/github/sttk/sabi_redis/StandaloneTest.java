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
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc("redis://127.0.0.1:6379/0"));
    try {
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithUriStringButInvalidAddr() {
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc("xxxx"));
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromUriString reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
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
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc("redis://127.0.0.1:9999/0"));
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromUriString reason2 -> {
              assertThat(reason2.uri()).isEqualTo("redis://127.0.0.1:9999/0");
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
    var data = new DataHub();
    try {
      data.uses("redis", new RedisDataSrc(new URI("redis://127.0.0.1:6379/0")));
    } catch (URISyntaxException e) {
      fail(e);
    }
    try {
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithURIButInvalidAddr() {
    var data = new DataHub();
    try {
      data.uses("redis", new RedisDataSrc(new URI("xxxx")));
    } catch (URISyntaxException e) {
      fail(e);
    }
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromURI reason2 -> {
              assertThat(reason2.uri().toString()).isEqualTo("xxxx");
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
  void test_NewRedisDataSrcWithURIButNotFoundAddr() {
    var data = new DataHub();
    try {
      data.uses("redis", new RedisDataSrc(new URI("redis://127.0.0.1:9999/0")));
    } catch (URISyntaxException e) {
      fail(e);
    }
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromURI reason2 -> {
              assertThat(reason2.uri().toString()).isEqualTo("redis://127.0.0.1:9999/0");
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
  void test_NewRedisDataSrcWithRedisURI() {
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(RedisURI.create("redis://127.0.0.1:6379/0")));
    try {
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithRedisURIButInvalidAddr() {
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(RedisURI.create("redis://127.0.0.1:9999/1")));
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromRedisURI reason2 -> {
              assertThat(reason2.redisURI().toString()).isEqualTo("redis://127.0.0.1:9999/1");
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
    var res = DefaultClientResources.create();
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(res, "redis://127.0.0.1:6379/0"));
    try {
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithClientResourcesAndUriStringButInvalidAddr() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(res, "xxxx"));
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromClientResourcesAndUriString reason2 -> {
              assertThat(reason2.uri()).isEqualTo("xxxx");
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
  void test_NewRedisDataSrcWithClientResourcesAndUriStringButNotFoundAddr() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(res, "redis://127.0.0.1:9999/0"));
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromClientResourcesAndUriString reason2 -> {
              assertThat(reason2.uri()).isEqualTo("redis://127.0.0.1:9999/0");
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
  void test_NewRedisDataSrcWithClientResourcesAndURI() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    try {
      data.uses("redis", new RedisDataSrc(res, new URI("redis://127.0.0.1:6379/0")));
    } catch (URISyntaxException e) {
      fail(e);
    }
    try {
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithClientResourcesAndURIButInvalidAddr() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    try {
      data.uses("redis", new RedisDataSrc(res, new URI("xxxx")));
    } catch (URISyntaxException e) {
      fail(e);
    }
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromClientResourcesAndURI reason2 -> {
              assertThat(reason2.uri().toString()).isEqualTo("xxxx");
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
  void test_NewRedisDataSrcWithClientResourcesAndURIButNotFoundAddr() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    try {
      data.uses("redis", new RedisDataSrc(res, new URI("redis://127.0.0.1:9999/0")));
    } catch (URISyntaxException e) {
      fail(e);
    }
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromClientResourcesAndURI reason2 -> {
              assertThat(reason2.uri().toString()).isEqualTo("redis://127.0.0.1:9999/0");
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
  void test_NewRedisDataSrcWithClientResourcesAndRedisURI() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(res, RedisURI.create("redis://127.0.0.1:6379/0")));
    try {
      data.run(d -> {});
    } catch (Err e) {
      fail(e);
    }
  }

  @Test
  void test_NewRedisDataSrcWithClientResourcesAndRedisURIButInvalidAddr() {
    var res = DefaultClientResources.create();
    var data = new DataHub();
    data.uses("redis", new RedisDataSrc(res, RedisURI.create("redis://127.0.0.1:9999/1")));
    try {
      data.run(d -> {});
      fail();
    } catch (Err err) {
      switch (err.getReason()) {
        case DataHub.FailToSetupLocalDataSrcs reason -> {
          assertThat(reason.errors()).hasSize(1);
          var err2 = reason.errors().get("redis");
          switch (err2.getReason()) {
            case RedisDataSrc.FailToCreateClientFromClientResourcesAndRedisURI reason2 -> {
              assertThat(reason2.redisURI().toString()).isEqualTo("redis://127.0.0.1:9999/1");
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
