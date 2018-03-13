package cz.datadriven.utils.config.view;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedConfigTest {

  @ConfigView
  interface RootConfig {
    @ConfigView.Integer(path = "cabbage")
    int cabbage();
    @ConfigView.Configuration(path = "fruit")
    FruitsConfig fruits();
  }

  @ConfigView
  interface FruitsConfig { // Single nested
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

  @Test
  public void test() {
    Config config = ConfigFactory.load("nested"); // load nested.conf from resources
    RootConfig rootConfig = ConfigViewFactory.create(RootConfig.class, config);
    System.out.println(rootConfig.cabbage());
    assertEquals(CABBAGE_COUNT, rootConfig.cabbage());
    assertEquals(APPLE_COUNT, rootConfig.fruits().apples());
    assertEquals(ORANGES_COUNT, rootConfig.fruits().citrus().oranges());
    assertEquals(POMELO_COUNT, rootConfig.fruits().citrus().pomelo());
  }
}
