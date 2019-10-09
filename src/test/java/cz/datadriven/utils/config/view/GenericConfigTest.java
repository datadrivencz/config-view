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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import org.junit.jupiter.api.Test;

public class GenericConfigTest {

  @ConfigView
  interface ShoppingItem {
    @ConfigView.String(path = "name")
    String name();

    @ConfigView.Double(path = "price")
    Double price();

    @ConfigView.TypesafeConfig(path = "properties")
    Config properties();
  }

  @ConfigView
  interface AppleProperties {
    @ConfigView.String(path = "type")
    String type();

    @ConfigView.String(path = "color")
    String color();

    @ConfigView.Integer(path = "category")
    int category();

    @ConfigView.Boolean(path = "crunchy")
    boolean isCrunchy();

    @ConfigView.Double(path = "weight")
    double weight();
  }

  @ConfigView
  interface WineProperties {
    @ConfigView.String(path = "type")
    String type();

    @ConfigView.String(path = "color")
    String color();

    @ConfigView.Integer(path = "sweetness")
    int sweetness();

    @ConfigView.Double(path = "volume")
    double volume();
  }

  @Test
  public void test() {
    Config listConfig = ConfigFactory.load("generic"); // load nested.conf from resources
    Double actualPrice = 0.0;
    for (Config itemConfig : listConfig.getConfigList("shopping-list")) {
      ShoppingItem item = ConfigViewFactory.create(ShoppingItem.class, itemConfig);
      actualPrice += item.price();
      switch (item.name()) {
        case "apple":
          {
            AppleProperties appleProperties =
                ConfigViewFactory.create(AppleProperties.class, item.properties());
            assertAppleProps(appleProperties);
          }
          break;
        case "wine":
          {
            WineProperties wineProperties =
                ConfigViewFactory.create(WineProperties.class, item.properties());
            assertWineProps(wineProperties);
          }
          break;
        default:
          {
            throw new IllegalArgumentException("Unknown type of shopping item");
          }
      }
    }
    assertEquals(13.5, actualPrice, 0.1);
  }

  private void assertAppleProps(AppleProperties appleProperties) {
    assertEquals("Red delicious", appleProperties.type());
    assertEquals("red", appleProperties.color());
    assertTrue(appleProperties.isCrunchy());
    assertEquals(1, appleProperties.category());
    assertEquals(0.33, appleProperties.weight(), 0.1);
  }

  private void assertWineProps(WineProperties wineProperties) {
    assertEquals("Chardonnay", wineProperties.type());
    assertEquals("white", wineProperties.color());
    assertEquals(2, wineProperties.sweetness());
    assertEquals(0.7, wineProperties.volume(), 0.1);
  }
}
