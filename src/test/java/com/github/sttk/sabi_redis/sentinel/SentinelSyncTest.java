package com.github.sttk.sabi_redis.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataAcc;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.Logic;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
public class SentinelSyncTest {
  private SentinelSyncTest() {}

  record FailToGetValue() {}

  record FailToSetValue() {}

  record FailToDelValue() {}

  interface RedisSampleDataAcc extends DataAcc, SampleData {
    default String getSampleKey() throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.sync();
      try {
        return commands.get("sample/sentinel");
      } catch (Exception e) {
        throw new Err(new FailToGetValue(), e);
      }
    }

    default void setSampleKey(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.sync();
      try {
        commands.set("sample/sentinel", val);
      } catch (Exception e) {
        throw new Err(new FailToSetValue(), e);
      }
    }

    default void delSampleKey() throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.sync();
      try {
        commands.del("sample/sentinel");
      } catch (Exception e) {
        throw new Err(new FailToDelValue(), e);
      }
    }

    default void setSampleKeyWithForceBack(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.sync();

      try {
        commands.set("sample_force_back/sentinel", val);
      } catch (Exception e) {
        throw new Err(new FailToSetValue(), e);
      }

      dc.addForceBack(
          redisConn1 -> {
            var commands1 = redisConn1.sync();
            try {
              commands1.del("sample_force_back/sentinel");
            } catch (Exception e) {
              throw new Err("fail to force back", e);
            }
          });

      try {
        commands.set("sample_force_back_2/sentinel", val);
      } catch (Exception e) {
        throw new Err(new FailToSetValue(), e);
      }

      dc.addForceBack(
          redisConn1 -> {
            var commands1 = redisConn1.sync();
            try {
              commands1.del("sample_force_back_2/sentinel");
            } catch (Exception e) {
              throw new Err("fail to force back", e);
            }
          });
    }

    default void setSampleKeyWithPreCommit(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();

      dc.addPreCommit(
          redisConn1 -> {
            var commands1 = redisConn1.sync();
            commands1.set("sample_pre_commit/sentinel", val);
          });
    }

    default void setSampleKeyWithPostCommit(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();

      dc.addPostCommit(
          redisConn1 -> {
            var commands1 = redisConn1.sync();
            commands1.set("sample_post_commit/sentinel", val);
          });
    }
  }

  interface SampleData {
    String getSampleKey() throws Err;

    void setSampleKey(String val) throws Err;

    void delSampleKey() throws Err;

    void setSampleKeyWithForceBack(String val) throws Err;

    void setSampleKeyWithPreCommit(String val) throws Err;

    void setSampleKeyWithPostCommit(String val) throws Err;
  }

  Logic<SampleData> sampleLogic =
      (SampleData data) -> {
        var val = data.getSampleKey();
        assertThat(val).isNull();

        data.setSampleKey("Hello");

        val = data.getSampleKey();
        assertThat(val).isEqualTo("Hello");

        data.delSampleKey();
      };

  Logic<SampleData> sampleLogicWithForceBackOk =
      (SampleData data) -> {
        data.setSampleKeyWithForceBack("Good Morning");
      };

  Logic<SampleData> sampleLogicWithForceBackErr =
      (SampleData data) -> {
        data.setSampleKeyWithForceBack("Good Afternoon");
        throw new Err("XXX");
      };

  Logic<SampleData> sampleLogicWithPreCommit =
      (SampleData data) -> {
        data.setSampleKeyWithPreCommit("Good Evening");
      };

  Logic<SampleData> sampleLogicWithPostCommit =
      (SampleData data) -> {
        data.setSampleKeyWithPostCommit("Good Night");
      };

  class SampleDataHub extends DataHub implements SampleData, RedisSampleDataAcc {}

  //

  @Test
  void test_TxnAndForceBack() {
    try (var data = new SampleDataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481"));
      try {
        data.txn(sampleLogicWithForceBackOk);
      } catch (Err err) {
        fail(err);
      }

      {
        var client =
            RedisClient.create(
                RedisURI.Builder.sentinel("127.0.0.1", 26479, "mymaster")
                    .withSentinel("redis://127.0.0.1:26480")
                    .withSentinel("redis://127.0.0.1:26481")
                    .build());
        var conn = client.connect();
        var cmd = conn.sync();

        var s = cmd.get("sample_force_back/sentinel");
        cmd.del("sample_force_back/sentinel");
        assertThat(s).isEqualTo("Good Morning");

        s = cmd.get("sample_force_back_2/sentinel");
        cmd.del("sample_force_back_2/sentinel");
        assertThat(s).isEqualTo("Good Morning");
      }

      try {
        data.txn(sampleLogicWithForceBackErr);
        fail();
      } catch (Err err) {
        assertThat(err.getReason()).isEqualTo("XXX");
      }

      {
        var client =
            RedisClient.create(
                RedisURI.Builder.sentinel("127.0.0.1", 26479, "mymaster")
                    .withSentinel("redis://127.0.0.1:26480")
                    .withSentinel("redis://127.0.0.1:26481")
                    .build());
        var conn = client.connect();
        var cmd = conn.sync();

        var s = cmd.get("sample_force_back/sentinel");
        cmd.del("sample_force_back/sentinel");
        assertThat(s).isNull();

        s = cmd.get("sample_force_back_2/sentinel");
        cmd.del("sample_force_back_2/sentinel");
        assertThat(s).isNull();
      }
    }
  }

  @Test
  void test_TxnAndPreCommit() {
    try (var data = new SampleDataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481"));
      try {
        data.txn(sampleLogicWithPreCommit);
      } catch (Err err) {
        fail(err);
      }

      {
        var client =
            RedisClient.create(
                RedisURI.Builder.sentinel("127.0.0.1", 26479, "mymaster")
                    .withSentinel("redis://127.0.0.1:26480")
                    .withSentinel("redis://127.0.0.1:26481")
                    .build());
        var conn = client.connect();
        var cmd = conn.sync();

        var s = cmd.get("sample_pre_commit/sentinel");
        cmd.del("sample_pre_commit/sentinel");
        assertThat(s).isEqualTo("Good Evening");
      }
    }
  }

  @Test
  void test_TxnAndPostCommit() {
    try (var data = new SampleDataHub()) {
      data.uses(
          "redis",
          new RedisSentinelDataSrc(
              "mymaster",
              "redis://127.0.0.1:26479",
              "redis://127.0.0.1:26480",
              "redis://127.0.0.1:26481"));
      try {
        data.txn(sampleLogicWithPostCommit);
      } catch (Err err) {
        fail(err);
      }

      {
        var client =
            RedisClient.create(
                RedisURI.Builder.sentinel("127.0.0.1", 26479, "mymaster")
                    .withSentinel("redis://127.0.0.1:26480")
                    .withSentinel("redis://127.0.0.1:26481")
                    .build());
        var conn = client.connect();
        var cmd = conn.sync();

        var s = cmd.get("sample_post_commit/sentinel");
        cmd.del("sample_post_commit/sentinel");
        assertThat(s).isEqualTo("Good Night");
      }
    }
  }
}
