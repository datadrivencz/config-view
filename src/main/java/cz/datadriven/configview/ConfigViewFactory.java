/*
 * Copyright 2018 Datadriven.cz
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
package cz.datadriven.configview;

import com.typesafe.config.Config;
import net.sf.cglib.proxy.Enhancer;

/** Factory responsible for creation of config views. */
public class ConfigViewFactory {

  /**
   * Create config view from a given config.
   *
   * @param configViewClass class to materialize view into
   * @param config config to create view from
   * @param basePath base path to extract from the config
   * @param <T> type of the view class to be created
   * @return the view
   */
  public static <T> T create(Class<T> configViewClass, Config config, String basePath) {
    return create(configViewClass, config.getConfig(basePath));
  }

  /**
   * Create config view from a given config.
   *
   * @param configViewClass class to materialize view into
   * @param config config to create view from
   * @param <T> type of the view class to be created
   * @return the view
   */
  @SuppressWarnings("unchecked")
  public static <T> T create(Class<T> configViewClass, Config config) {
    if (!ConfigViewProxy.canProxy(configViewClass)) {
      throw new IllegalArgumentException(
          "Can not instantiate metric group for "
              + "class [ "
              + configViewClass
              + " ]. Did you forgot @ConfigView annotation?");
    }
    final Enhancer enhancer = new Enhancer();
    enhancer.setCallback(new ConfigViewProxy(new ConfigViewProxy.Factory(config)));
    if (configViewClass.isInterface()) {
      enhancer.setInterfaces(new Class[] {configViewClass});
    } else {
      enhancer.setSuperclass(configViewClass);
    }
    return (T) enhancer.create();
  }
}
