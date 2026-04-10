/*
 * module-info.java
 * Copyright (C) 2026 Takayuki Sato. All Rights Reserved.
 */

/**
 * Defines the APIs of Redis data access for Sabi framework.
 *
 * <p>This module contains the implementations of Sabi's {@code DataSrc} and {@code DataConn}
 * required to connect to and operate Redis servers running in various configurations.
 *
 * @version 0.1
 */
module com.github.sttk.sabi_redis {
  exports com.github.sttk.sabi_redis;

  requires transitive com.github.sttk.sabi;
  requires transitive com.github.sttk.errs;
  requires transitive lettuce.core;
}
