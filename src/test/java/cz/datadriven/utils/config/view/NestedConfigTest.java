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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NestedConfigTest {

  private static final String TEST_CONFIG =
      "cabbage: 98\n"
          + "fruit {\n"
          + "  apple: 31\n"
          + "  citrus: {\n"
          + "    orange:45\n"
          + "    pomelo:63\n"
          + "  }\n"
          + "}\n"
          + "farmers: [\n"
          + "  { name: \"David\", country: \"CZ\" }\n"
          + "  { name: \"Helmut\", country: \"AT\" }\n"
          + "  { name: \"Charles\", country: \"US\" }\n"
          + "]\n"
          + "totalWeight: 366.6\n";

  @ConfigView
  interface RootConfig extends RawConfigAware {
    @ConfigView.Integer(path = "cabbage")
    int cabbage();

    @ConfigView.View(path = "fruit")
    FruitsConfig fruits();

    @ConfigView.ViewList(path = "farmers")
    List<FarmerConfig> farmers();

    @ConfigView.Double(path = "totalWeight")
    double totalWeight();
  }

  /** First level nesting. */
  @ConfigView
  interface FruitsConfig extends RawConfigAware { // Single nested
    @ConfigView.Integer(path = "apple")
    int apples();

    @ConfigView.View(path = "citrus")
    CitrusConfig citrus();
  }

  /** Second level nesting. */
  @ConfigView
  interface CitrusConfig {

    @ConfigView.Integer(path = "orange")
    int oranges();

    @ConfigView.Integer(path = "pomelo")
    int pomelo();
  }

  /** First level nesting - collection. */
  @ConfigView
  interface FarmerConfig {

    @ConfigView.String(path = "name")
    String name();

    @ConfigView.String(path = "country")
    String country();
  }

  @Test
  void test() {
    final Config config = ConfigFactory.parseString(TEST_CONFIG);
    final RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, config);
    assertEquals(98, rootConfig.cabbage());
    assertEquals(31, rootConfig.fruits().apples());
    assertEquals(45, rootConfig.fruits().citrus().oranges());
    assertEquals(63, rootConfig.fruits().citrus().pomelo());
    assertEquals(366.6d, rootConfig.totalWeight());
    final List<FarmerConfig> farmers = rootConfig.farmers();
    Assertions.assertEquals(3, farmers.size());
    Assertions.assertEquals("David", farmers.get(0).name());
    Assertions.assertEquals("CZ", farmers.get(0).country());
    Assertions.assertEquals("Helmut", farmers.get(1).name());
    Assertions.assertEquals("AT", farmers.get(1).country());
    Assertions.assertEquals("Charles", farmers.get(2).name());
    Assertions.assertEquals("US", farmers.get(2).country());
  }

  @Test
  void testNestedCaching() {
    final Config config = ConfigFactory.parseString(TEST_CONFIG);
    final RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, config);
    assertEquals(
        System.identityHashCode(rootConfig.farmers().get(0)),
        System.identityHashCode(rootConfig.farmers().get(0)));
  }

  @Test
  void testRawConfig() {
    final Config config = ConfigFactory.parseString(TEST_CONFIG);
    final RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, config);
    assertEquals(31, rootConfig.getRawConfig().getInt("fruit.apple"));
    assertEquals(31, rootConfig.fruits().getRawConfig().getInt("apple"));
  }
}
