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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import org.junit.jupiter.api.Test;

public class NestedConfigTest {

  @ConfigView
  interface RootConfig extends RawConfigAware {
    @ConfigView.Integer(path = "cabbage")
    int cabbage();

    @ConfigView.Configuration(path = "fruit")
    FruitsConfig fruits();

    @ConfigView.Double(path = "totalWeight")
    double totalWeight();
  }

  @ConfigView
  interface FruitsConfig extends RawConfigAware { // Single nested
    @ConfigView.Integer(path = "apple")
    int apples();

    @ConfigView.Configuration(path = "citrus")
    CitrusConfig citrus();
  }

  @ConfigView
  interface CitrusConfig { // Double-nested
    @ConfigView.Integer(path = "orange")
    int oranges();

    @ConfigView.Integer(path = "pomelo")
    int pomelo();
  }

  // Values must match resources/nested.conf
  private static final int CABBAGE_COUNT = 98;
  private static final int APPLE_COUNT = 31;
  private static final int ORANGES_COUNT = 45;
  private static final int POMELO_COUNT = 63;
  private static final double TOTAL_WEIGHT = 366.6d;

  @Test
  public void test() {
    Config config = ConfigFactory.load("nested"); // load nested.conf from resources
    RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, config);
    assertEquals(CABBAGE_COUNT, rootConfig.cabbage());
    assertEquals(APPLE_COUNT, rootConfig.fruits().apples());
    assertEquals(ORANGES_COUNT, rootConfig.fruits().citrus().oranges());
    assertEquals(POMELO_COUNT, rootConfig.fruits().citrus().pomelo());
    assertEquals(TOTAL_WEIGHT, rootConfig.totalWeight());
  }

  @Test
  public void testRawConfig() {
    Config config = ConfigFactory.load("nested"); // load nested.conf from resources
    RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, config);

    assertEquals(APPLE_COUNT, rootConfig.getRawConfig().getInt("fruit.apple"));
    assertEquals(APPLE_COUNT, rootConfig.fruits().getRawConfig().getInt("apple"));
  }
}
