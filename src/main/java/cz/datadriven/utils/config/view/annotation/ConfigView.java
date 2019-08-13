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

import com.typesafe.config.Config;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ConfigView {

  java.lang.String DEFAULT_FALLBACK_PATH = "";

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface String {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();

    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Configuration {

    /**
     * The name of the field.
     *
     * @return path to the config property
     */
    java.lang.String path();

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
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

    /**
     * Path to fallback value in optional fallback configuration.
     *
     * @see cz.datadriven.utils.config.view.ConfigViewFactory#create(Class, Config, Config...)
     *     Declaration of fallback configs.
     * @return path to fallback value of fallback configuration.
     */
    java.lang.String fallbackPath() default DEFAULT_FALLBACK_PATH;
  }
}
