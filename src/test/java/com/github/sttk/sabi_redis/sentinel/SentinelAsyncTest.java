package com.github.sttk.sabi_redis.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataAcc;
import com.github.sttk.sabi.DataHub;
import com.github.sttk.sabi.Logic;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
public class SentinelAsyncTest {
  private SentinelAsyncTest() {}

  record FailToGetValue() {}

  record FailToSetValue() {}

  record FailToDelValue() {}

  interface RedisSampleDataAcc extends DataAcc, SampleData {
    default Future<String> getSampleKey() throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.async();
      try {
        return commands.get("sample/sentinel/async");
      } catch (Exception e) {
        throw new Err(new FailToGetValue(), e);
      }
    }

    default Future<String> setSampleKey(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.async();
      try {
        return commands.set("sample/sentinel/async", val);
      } catch (Exception e) {
        throw new Err(new FailToSetValue(), e);
      }
    }

    default Future<Long> delSampleKey() throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.async();
      try {
        return commands.del("sample/sentinel/async");
      } catch (Exception e) {
        throw new Err(new FailToDelValue(), e);
      }
    }

    default List<Future<String>> setSampleKeyWithForceBack(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();
      var commands = redisConn.async();

      var futureList = new ArrayList<Future<String>>();

      try {
        futureList.add(commands.set("sample_force_back/sentinel/async", val));
      } catch (Exception e) {
        throw new Err(new FailToSetValue(), e);
      }

      dc.addForceBack(
          redisConn1 -> {
            var commands1 = redisConn1.async();
            try {
              var future = commands1.del("sample_force_back/sentinel/async");
              future.get();
            } catch (Exception e) {
              throw new Err("fail to force back", e);
            }
          });

      try {
        futureList.add(commands.set("sample_force_back_2/sentinel/async", val));
      } catch (Exception e) {
        throw new Err(new FailToSetValue(), e);
      }

      dc.addForceBack(
          redisConn1 -> {
            var commands1 = redisConn1.async();
            try {
              var future = commands1.del("sample_force_back_2/sentinel/async");
              future.get();
            } catch (Exception e) {
              throw new Err("fail to force back", e);
            }
          });

      return futureList;
    }

    default void setSampleKeyWithPreCommit(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();

      dc.addPreCommit(
          redisConn1 -> {
            var commands1 = redisConn1.async();
            var future = commands1.set("sample_pre_commit/sentinel/async", val);
            try {
              future.get();
            } catch (Exception e) {
              fail(e);
            }
          });
    }

    default void setSampleKeyWithPostCommit(String val) throws Err {
      var dc = getDataConn("redis", RedisSentinelDataConn.class);
      var redisConn = dc.getConnection();

      dc.addPreCommit(
          redisConn1 -> {
            var commands1 = redisConn1.async();
            var future = commands1.set("sample_post_commit/sentinel/async", val);
            try {
              future.get();
            } catch (Exception e) {
              fail(e);
            }
          });
    }
  }

  interface SampleData {
    Future<String> getSampleKey() throws Err;

    Future<String> setSampleKey(String val) throws Err;

    Future<Long> delSampleKey() throws Err;

    List<Future<String>> setSampleKeyWithForceBack(String val) throws Err;

    void setSampleKeyWithPreCommit(String val) throws Err;

    void setSampleKeyWithPostCommit(String val) throws Err;
  }

  Logic<SampleData> sampleLogic =
      (SampleData data) -> {
        try {
          var f = data.getSampleKey();
          assertThat(f.get()).isNull();

          data.setSampleKey("Hello").get();

          var fut = data.getSampleKey();
          assertThat(fut.get()).isEqualTo("Hello");

          data.delSampleKey().get();
        } catch (Exception e) {
          fail(e);
        }
      };

  Logic<SampleData> sampleLogicWithForceBackOk =
      (SampleData data) -> {
        try {
          var futureList = data.setSampleKeyWithForceBack("Good Morning");
          for (var f : futureList) {
            f.get();
          }
        } catch (Exception e) {
          fail(e);
        }
      };

  Logic<SampleData> sampleLogicWithForceBackErr =
      (SampleData data) -> {
        try {
          var futureList = data.setSampleKeyWithForceBack("Good Afternoon");
          for (var f : futureList) {
            f.get();
          }
        } catch (Exception e) {
          fail(e);
        }
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

        var s = cmd.get("sample_force_back/sentinel/async");
        cmd.del("sample_force_back/sentinel/async");
        assertThat(s).isEqualTo("Good Morning");

        s = cmd.get("sample_force_back_2/sentinel/async");
        cmd.del("sample_force_back_2/sentinel/async");
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

        var s = cmd.get("sample_force_back/sentinel/async");
        cmd.del("sample_force_back/sentinel/async");
        assertThat(s).isNull();

        s = cmd.get("sample_force_back_2/sentinel/async");
        cmd.del("sample_force_back_2/sentinel/async");
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

        var s = cmd.get("sample_pre_commit/sentinel/async");
        cmd.del("sample_pre_commit/sentinel/async");
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

        var s = cmd.get("sample_post_commit/sentinel/async");
        cmd.del("sample_post_commit/sentinel/async");
        assertThat(s).isEqualTo("Good Night");
      }
    }
  }
}
