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
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import cz.datadriven.configview.annotation.ConfigView;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigViewTest {

  @ConfigView
  interface TestConfigView {

    @ConfigView.String(path = "first")
    String first();

    @ConfigView.String(path = "second")
    String second();
  }

  @ConfigView
  abstract static class TestConfigView2 implements TestConfigView {

    @ConfigView.String(path = "third")
    abstract String third();

    String nonAnnotated() {
      return "non_annotated";
    }
  }

  @ConfigView
  interface AllAnnotationsConfigView {

    @ConfigView.String(path = "string")
    String string();

    @ConfigView.StringList(path = "string-list")
    List<String> stringList();

    @ConfigView.Boolean(path = "boolean")
    boolean booleanValue();

    @ConfigView.Duration(path = "duration")
    Duration duration();

    @ConfigView.Millis(path = "millis")
    long millis();
  }

  interface NonAnnotatedTestConfigView {

  }

  @Test
  public void test() {
    final Config config = ConfigFactory.empty()
        .withValue("first", ConfigValueFactory.fromAnyRef("first_value"))
        .withValue("second", ConfigValueFactory.fromAnyRef("second_value"));
    final TestConfigView wrap = ConfigViewFactory.create(TestConfigView.class, config);
    assertEquals("first_value", wrap.first());
    assertEquals("second_value", wrap.second());
  }

  @Test
  public void testClass() {
    final Config config = ConfigFactory.empty()
        .withValue("first", ConfigValueFactory.fromAnyRef("first_value"))
        .withValue("second", ConfigValueFactory.fromAnyRef("second_value"))
        .withValue("third", ConfigValueFactory.fromAnyRef("third_value"));
    final TestConfigView2 wrap = ConfigViewFactory.create(TestConfigView2.class, config);
    assertEquals("first_value", wrap.first());
    assertEquals("second_value", wrap.second());
    assertEquals("third_value", wrap.third());
    assertEquals("non_annotated", wrap.nonAnnotated());
  }

  @Test
  public void testAllAnnotationsWithPrefix() {
    final Config config = ConfigFactory.empty()
        .withValue("x.y.string", ConfigValueFactory.fromAnyRef("string"))
        .withValue("x.y.string-list", ConfigValueFactory.fromAnyRef(Arrays.asList("a", "b", "c")))
        .withValue("x.y.boolean", ConfigValueFactory.fromAnyRef(true))
        .withValue("x.y.duration", ConfigValueFactory.fromAnyRef("10 seconds"))
        .withValue("x.y.millis", ConfigValueFactory.fromAnyRef("5 seconds"));
    final AllAnnotationsConfigView wrap =
        ConfigViewFactory.create(AllAnnotationsConfigView.class, config, "x.y");
    assertEquals("string", wrap.string());
    assertEquals(Arrays.asList("a", "b", "c"), wrap.stringList());
    assertTrue(wrap.booleanValue());
    assertEquals(Duration.ofSeconds(10), wrap.duration());
    assertEquals(5000L, wrap.millis());
  }

  @Test()
  public void testMissingValue() {
    assertThrows(ConfigException.Missing.class, () -> {
      final Config config = ConfigFactory.empty();
      final TestConfigView wrap = ConfigViewFactory.create(TestConfigView.class, config);
      wrap.first();
    });
  }

  @Test()
  public void testWrapNonAnnotatedClass() {
    assertThrows(IllegalArgumentException.class, () -> {
      final Config config = ConfigFactory.empty();
      ConfigViewFactory.create(NonAnnotatedTestConfigView.class, config);
    });
  }
}
