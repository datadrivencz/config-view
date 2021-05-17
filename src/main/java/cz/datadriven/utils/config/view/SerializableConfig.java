/*
 * Copyright 2020 Datadriven.cz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.datadriven.utils.config.view;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** {@link Serializable} holder for typesafe {@link Config}. */
public class SerializableConfig implements Serializable {

  private static class SerializedConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ConcurrentMap<String, SerializableConfig> cache =
        new ConcurrentHashMap<>();

    private final String rawConfig;

    SerializedConfig(String rawConfig) {
      this.rawConfig = rawConfig;
    }

    protected Object readResolve() {
      return cache.computeIfAbsent(
          rawConfig, key -> new SerializableConfig(ConfigFactory.parseString(key)));
    }
  }

  private static final long serialVersionUID = 6938678382576938868L;

  private final transient Config config;

  public SerializableConfig(Config config) {
    this.config = config;
  }

  /**
   * Get the underlying config.
   *
   * @return Config.
   */
  public Config get() {
    return config;
  }

  protected Object readResolve() {
    return this;
  }

  protected Object writeReplace() {
    return new SerializedConfig(config.root().render(ConfigRenderOptions.concise()));
  }
}
