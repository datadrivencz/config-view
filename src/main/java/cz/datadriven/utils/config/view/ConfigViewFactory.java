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

import com.typesafe.config.Config;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/** Factory responsible for creation of config views. */
public class ConfigViewFactory {

  private static final Set<TypeDescription> ANNOTATION_TYPE_DESCRIPTORS =
      ConfigViewProxy.ANNOTATIONS.stream()
          .map(TypeDescription.ForLoadedType::of)
          .collect(Collectors.toSet());

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
  public static <T> T create(Class<T> configViewClass, Config config) {
    if (!ConfigViewProxy.canProxy(configViewClass)) {
      throw new IllegalArgumentException(
          String.format(
              "Can not instantiate ConfigView for class [%s]. Did you forget @ConfigView annotation?",
              configViewClass));
    }
    try {
      return new ByteBuddy(ClassFileVersion.JAVA_V8)
          .subclass(configViewClass)
          .method(
              ElementMatchers.isAnnotatedWith(ANNOTATION_TYPE_DESCRIPTORS::contains)
                  .or(ElementMatchers.isDeclaredBy(RawConfigAware.class)))
          .intercept(
              InvocationHandlerAdapter.of(new ConfigViewProxy(new ConfigViewProxy.Factory(config))))
          .make()
          .load(
              ConfigViewFactory.class.getClassLoader(),
              determineBestClassLoadingStrategy(configViewClass))
          .getLoaded()
          .getDeclaredConstructor()
          .newInstance();
    } catch (Exception e) {
      throw new IllegalStateException(
          String.format("Unable to construct [%s] class.", configViewClass), e);
    }
  }

  private static ClassLoadingStrategy<ClassLoader> determineBestClassLoadingStrategy(
      Class<?> targetClass) throws Exception {
    if (ClassInjector.UsingLookup.isAvailable()) {
      Class<?> methodHandlesClass = Class.forName("java.lang.invoke.MethodHandles");
      Class<?> lookupClass = Class.forName("java.lang.invoke.MethodHandles$Lookup");
      Method lookupMethod = methodHandlesClass.getMethod("lookup");
      Method privateLookupInMethod =
          methodHandlesClass.getMethod("privateLookupIn", Class.class, lookupClass);
      Object lookup = lookupMethod.invoke(null);
      Object privateLookup = privateLookupInMethod.invoke(null, targetClass, lookup);
      return ClassLoadingStrategy.UsingLookup.of(privateLookup);
    }
    if (ClassInjector.UsingReflection.isAvailable()) {
      return ClassLoadingStrategy.Default.INJECTION;
    }
    return ClassLoadingStrategy.Default.WRAPPER;
  }
}
