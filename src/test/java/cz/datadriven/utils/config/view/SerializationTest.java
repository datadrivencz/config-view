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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SerializationTest {

  @ConfigView
  interface TestConfigViewSerializableInterface extends Serializable {

    @ConfigView.String(path = "first")
    String first();

    @ConfigView.String(path = "second")
    String second();
  }

  @ConfigView
  interface TestConfigViewNonSerializableInterface {

    @ConfigView.String(path = "first")
    String first();

    @ConfigView.String(path = "second")
    String second();
  }

  @ConfigView
  abstract static class TestConfigViewSerializableAbstractClass implements Serializable {

    @ConfigView.String(path = "first")
    abstract String first();

    @ConfigView.String(path = "second")
    abstract String second();
  }

  @ConfigView
  abstract static class TestConfigViewNonSerializableAbstractClass {

    @ConfigView.String(path = "first")
    abstract String first();

    @ConfigView.String(path = "second")
    abstract String second();
  }

  private static <T> void testSerialization(final Class<T> clazz, final Object originalConfig)
      throws IOException, ClassNotFoundException {
    // ~ serialize (must not fail)
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(originalConfig);
      oos.flush();
    }

    // ~ deserialize
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    Object deserializedConfig;
    try (ObjectInputStream ois = new ObjectInputStream(bais)) {
      deserializedConfig = ois.readObject();
    }
    assertNotNull(deserializedConfig);
    assertTrue(clazz.isAssignableFrom(deserializedConfig.getClass()));
  }

  @Test
  void testSerializableInterface() {
    final Config config =
        ConfigFactory.empty()
            .withValue("first", ConfigValueFactory.fromAnyRef("first_value"))
            .withValue("second", ConfigValueFactory.fromAnyRef("second_value"));
    final Class<TestConfigViewSerializableInterface> clazz =
        TestConfigViewSerializableInterface.class;
    final TestConfigViewSerializableInterface wrappedConfig =
        ConfigViewFactory.create(clazz, config);
    Assertions.assertDoesNotThrow(() -> testSerialization(clazz, wrappedConfig));
  }

  @Test
  void testNonSerializableInterface() {
    final Config config =
        ConfigFactory.empty()
            .withValue("first", ConfigValueFactory.fromAnyRef("first_value"))
            .withValue("second", ConfigValueFactory.fromAnyRef("second_value"));
    final Class<TestConfigViewNonSerializableInterface> clazz =
        TestConfigViewNonSerializableInterface.class;
    final TestConfigViewNonSerializableInterface wrappedConfig =
        ConfigViewFactory.create(clazz, config);
    Assertions.assertThrows(
        NotSerializableException.class, () -> testSerialization(clazz, wrappedConfig));
  }

  @Test
  void testSerializableAbstractClass() {
    final Config config =
        ConfigFactory.empty()
            .withValue("first", ConfigValueFactory.fromAnyRef("first_value"))
            .withValue("second", ConfigValueFactory.fromAnyRef("second_value"));
    final Class<TestConfigViewSerializableAbstractClass> clazz =
        TestConfigViewSerializableAbstractClass.class;
    final TestConfigViewSerializableAbstractClass wrappedConfig =
        ConfigViewFactory.create(clazz, config);
    Assertions.assertDoesNotThrow(() -> testSerialization(clazz, wrappedConfig));
  }

  @Test
  void testNonSerializableAbstractClass() {
    final Config config =
        ConfigFactory.empty()
            .withValue("first", ConfigValueFactory.fromAnyRef("first_value"))
            .withValue("second", ConfigValueFactory.fromAnyRef("second_value"));
    final Class<TestConfigViewNonSerializableAbstractClass> clazz =
        TestConfigViewNonSerializableAbstractClass.class;
    final TestConfigViewNonSerializableAbstractClass wrappedConfig =
        ConfigViewFactory.create(clazz, config);
    Assertions.assertThrows(
        NotSerializableException.class, () -> testSerialization(clazz, wrappedConfig));
  }
}
