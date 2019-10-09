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
package cz.datadriven.utils.config.view.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ConfigView {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface String {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface StringList {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Boolean {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Integer {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Long {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Double {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Duration {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  /** Handle for obtaining an instance of ConfigView annotated class. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Configuration {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  /** Handle for obtaining an instance of typesafe Config class. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface TypesafeConfig {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Map {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Bytes {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();
  }
}
