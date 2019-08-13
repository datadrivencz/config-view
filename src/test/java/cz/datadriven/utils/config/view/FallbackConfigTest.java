/*
 * Copyright 2019 Datadriven.cz
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import org.junit.jupiter.api.Test;

class FallbackConfigTest {

  @ConfigView
  interface RootConfig {
    @ConfigView.Integer(path = "cabbage", fallbackPath = "fallbacks.cabbage")
    int cabbage();

    @ConfigView.Configuration(path = "fruit")
    FruitsConfig fruits();

    @ConfigView.Double(path = "totalWeight", fallbackPath = "fallbacks.totalWeight")
    double totalWeight();
  }

  @ConfigView
  interface FruitsConfig { // Single nested
    @ConfigView.Integer(path = "apple", fallbackPath = "fallbacks.fruit.apple")
    int apples();

    @ConfigView.Configuration(path = "citrus", fallbackPath = "fallbacks.fruit.citrus")
    CitrusConfig citrus();
  }

  @ConfigView
  interface CitrusConfig { // Double-nested
    @ConfigView.Integer(path = "orange")
    int oranges();

    @ConfigView.Integer(path = "pomelo")
    int pomelo();
  }

  @Test
  public void testFallbackValue() {
    Config defaultConf =
        ConfigFactory.load("fallback-default"); // load fallback-default.conf from resources
    Config fallbackConf =
        ConfigFactory.load("fallback-fallback"); // load fallback-fallback.conf from resources

    RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, defaultConf, fallbackConf);

    assertEquals(rootConfig.cabbage(), 105);
  }

  @Test
  public void testFallbackNestedConfig() {
    Config defaultConf =
        ConfigFactory.load("fallback-default"); // load fallback-default.conf from resources
    Config fallbackConf1 =
        ConfigFactory.load("fallback-fallback"); // load fallback-fallback.conf from resources
    Config fallbackConf2 =
        ConfigFactory.load("fallback-fallback2"); // load fallback-fallback2.conf from resources

    RootConfig rootConfig =
        ConfigViewFactory.create(RootConfig.class, defaultConf, fallbackConf1, fallbackConf2);

    // in default config we do not have  citrus at all but in fallback they are
    assertNotNull(rootConfig.fruits().citrus());
    // first falback contains only pomelo
    assertEquals(115, rootConfig.fruits().citrus().pomelo());
    // second fallback contains pomelo and oranges
    assertEquals(145, rootConfig.fruits().citrus().oranges());
    assertEquals(366.6, rootConfig.totalWeight());
  }

  @Test
  public void testValueNotFoundAnywhere() {
    //we will work only with totalWeight from fallbacks because any of them does not contains apple.
    Config fallbackConf1 =
        ConfigFactory.load("fallback-fallback"); // load fallback-fallback.conf from resources
    Config fallbackConf2 =
        ConfigFactory.load("fallback-fallback2"); // load fallback-fallback2.conf from resources

    RootConfig rootConfig =
        ConfigViewFactory.create(RootConfig.class, fallbackConf1, fallbackConf2);

    assertThrows(ConfigViewProxy.FallbackConfigMissingException.class, rootConfig::totalWeight);
  }
}
