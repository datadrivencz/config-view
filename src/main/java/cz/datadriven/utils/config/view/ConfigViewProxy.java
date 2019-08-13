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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

class ConfigViewProxy implements MethodInterceptor {

  public static class FallbackConfigMissingException extends ConfigException {

    private final List<ConfigException> configExceptions;

    private FallbackConfigMissingException(String message, List<ConfigException> configExceptions) {
      super(message);
      this.configExceptions = Collections.unmodifiableList(configExceptions);
    }

    public List<ConfigException> getConfigExceptions() {
      return configExceptions;
    }
  }

  private static final List<Class<? extends Annotation>> ANNOTATIONS =
      Arrays.asList(
          ConfigView.String.class,
          ConfigView.StringList.class,
          ConfigView.Boolean.class,
          ConfigView.Integer.class,
          ConfigView.Long.class,
          ConfigView.Double.class,
          ConfigView.Duration.class,
          ConfigView.Configuration.class,
          ConfigView.Bytes.class,
          ConfigView.Map.class);

  static class Factory {

    private final Config config;

    private final List<Config> fallbackConfigs;

    Factory(Config config, List<Config> fallbackConfigs) {
      this.config = config;
      this.fallbackConfigs = fallbackConfigs;
    }

    String createString(ConfigView.String annotation) {
      return getWithFallback(Config::getString, annotation.path(), annotation.fallbackPath());
    }

    List<String> createStringList(ConfigView.StringList annotation) {
      return getWithFallback(Config::getStringList, annotation.path(), annotation.fallbackPath());
    }

    boolean createBoolean(ConfigView.Boolean annotation) {
      return getWithFallback(Config::getBoolean, annotation.path(), annotation.fallbackPath());
    }

    int createInteger(ConfigView.Integer annotation) {
      return getWithFallback(Config::getInt, annotation.path(), annotation.fallbackPath());
    }

    long createLong(ConfigView.Long annotation) {
      return getWithFallback(Config::getLong, annotation.path(), annotation.fallbackPath());
    }

    double createDouble(ConfigView.Double annotation) {
      return getWithFallback(Config::getDouble, annotation.path(), annotation.fallbackPath());
    }

    <T> T createConfig(ConfigView.Configuration annotation, Class<T> claz) {
      Config[] fallbackConfigsArray = this.fallbackConfigs.toArray(new Config[] {});
      String fallbackPath = annotation.fallbackPath();
      String path = annotation.path();

      // Collect fallback config
      Config mergedFallbackConfig = getFallbackConfig(fallbackPath);

      // Create subconfig from fallback if the config has not given path and fallback config is not
      // empty
      if (!config.hasPath(path) && !mergedFallbackConfig.isEmpty()) {
        return ConfigViewFactory.create(claz, mergedFallbackConfig, fallbackConfigsArray);
      }

      // otherwise try to fallback config
      return ConfigViewFactory.create(
          claz, config.getConfig(path).withFallback(mergedFallbackConfig), fallbackConfigsArray);
    }

    Duration createDuration(ConfigView.Duration annotation) {
      return getWithFallback(Config::getDuration, annotation.path(), annotation.fallbackPath());
    }

    Map<String, Object> createMap(ConfigView.Map annotation) {
      String path = annotation.path();
      Config fallbackConfig = getFallbackConfig(annotation.fallbackPath());
      Config mapConfig;

      if (!config.hasPath(path) && !fallbackConfig.isEmpty()) {
        //compose the map from fallback configs if default does not have given path but fallback is not empty
        mapConfig = fallbackConfig;
      } else {
        // otherwise try to compose config from path and its fallbacks
        mapConfig = config.getConfig(path).withFallback(fallbackConfig);
      }

      return mapConfig
          .entrySet()
          .stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().unwrapped()));
    }

    long createBytes(ConfigView.Bytes annotation) {
      return getWithFallback(Config::getBytes, annotation.path(), annotation.fallbackPath());
    }

    private Config getFallbackConfig(String fallbackPath) {
      Config mergedFallbackConfig = ConfigFactory.empty();
      if (!ConfigView.DEFAULT_FALLBACK_PATH.equals(fallbackPath) && !fallbackConfigs.isEmpty()) {
        for (Config fallbackConfig : fallbackConfigs) {
          if (fallbackConfig.hasPath(fallbackPath)) {
            mergedFallbackConfig =
                mergedFallbackConfig.withFallback(fallbackConfig.getConfig(fallbackPath));
          }
        }
      }
      return mergedFallbackConfig;
    }

    private <T> T getWithFallback(
        BiFunction<Config, String, T> getFn, String mainPath, String fallbackPath) {
      ConfigException catchedException;

      try {
        return getFn.apply(config, mainPath);
      } catch (ConfigException e) {
        catchedException = e;
      }

      // produce default exception if there is not any fallback
      if (fallbackConfigs.isEmpty() || ConfigView.DEFAULT_FALLBACK_PATH.equals(fallbackPath)) {
        throw catchedException;
      }

      // collect all exceptions for later producing in group exception.
      ArrayList<ConfigException> configExceptions = new ArrayList<>();
      configExceptions.add(catchedException);

      // find fallback value
      Optional<T> maybeFallbackValue =
          fallbackConfigs
              .stream()
              .map(
                  fallbackConfig -> {
                    try {
                      return getFn.apply(fallbackConfig, fallbackPath);
                    } catch (ConfigException e) {
                      configExceptions.add(e);
                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .findFirst();

      // or throw exception
      return maybeFallbackValue.orElseThrow(
          () ->
              new FallbackConfigMissingException(
                  String.format(
                      "Value for path [%s] was not found neither in configuration "
                          + "nor in any fallback configurations under [%s] path",
                      mainPath, fallbackPath),
                  configExceptions));
    }
  }

  /**
   * Handler for a specific annotation
   *
   * @param <T>
   */
  @FunctionalInterface
  private interface AnnotationHandler<T> {

    T handle(Annotation annotation, Class<T> returnType);
  }

  private final ConcurrentHashMap<String, Object> trackedInstruments = new ConcurrentHashMap<>();
  private final Map<Class<?>, AnnotationHandler<?>> annotationHandlers;

  ConfigViewProxy(Factory factory) {
    this.annotationHandlers = createAnnotationHandlers(factory);
  }

  @Override
  public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy)
      throws Throwable {
    final Optional<Annotation> maybeAnnotation = getInstrumentAnnotation(method);
    if (maybeAnnotation.isPresent()) {
      return getOrCreateInstrument(method.getName(), method.getReturnType(), maybeAnnotation.get());
    } else {
      return methodProxy.invokeSuper(obj, args);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getOrCreateInstrument(String key, Class<T> returnType, Annotation annotation) {
    return (T)
        trackedInstruments.computeIfAbsent(
            key,
            x -> {
              final AnnotationHandler handler = annotationHandlers.get(annotation.annotationType());
              if (handler == null) {
                throw new IllegalStateException(
                    "Handler for annotation [ "
                        + annotation.annotationType()
                        + " ] is not registered.");
              }
              return handler.handle(annotation, returnType);
            });
  }

  private Optional<Annotation> getInstrumentAnnotation(Method method) {
    final List<Annotation> annotations =
        Arrays.stream(method.getDeclaredAnnotations())
            .filter(a -> ANNOTATIONS.contains(a.annotationType()))
            .collect(Collectors.toList());
    if (annotations.size() == 0) {
      return Optional.empty();
    } else if (annotations.size() == 1) {
      return Optional.of(annotations.get(0));
    } else {
      throw new RuntimeException(
          "Method [ " + method + " ] has more than one instrument annotation.");
    }
  }

  static boolean canProxy(Class clazz) {
    for (Annotation annotation : clazz.getDeclaredAnnotations()) {
      if (ConfigView.class.equals(annotation.annotationType())) {
        return true;
      }
    }
    return false;
  }

  private static Map<Class<?>, AnnotationHandler<?>> createAnnotationHandlers(Factory factory) {
    final Map<Class<?>, AnnotationHandler<?>> handlers = new HashMap<>();
    handlers.put(
        ConfigView.String.class,
        checkType(
            String.class,
            (key, returnType) -> {
              final ConfigView.String annotation = (ConfigView.String) key;
              return factory.createString(annotation);
            }));
    handlers.put(
        ConfigView.StringList.class,
        checkType(
            List.class,
            (key, returnType) -> {
              final ConfigView.StringList annotation = (ConfigView.StringList) key;
              return factory.createStringList(annotation);
            }));
    handlers.put(
        ConfigView.Boolean.class,
        checkType(
            Boolean.class,
            (key, returnType) -> {
              final ConfigView.Boolean annotation = (ConfigView.Boolean) key;
              return factory.createBoolean(annotation);
            }));
    handlers.put(
        ConfigView.Integer.class,
        checkType(
            Integer.class,
            (key, returnType) -> {
              final ConfigView.Integer annotation = (ConfigView.Integer) key;
              return factory.createInteger(annotation);
            }));
    handlers.put(
        ConfigView.Long.class,
        checkType(
            Long.class,
            (key, returnType) -> {
              final ConfigView.Long annotation = (ConfigView.Long) key;
              return factory.createLong(annotation);
            }));
    handlers.put(
        ConfigView.Double.class,
        checkType(
            Double.class,
            (key, returnType) -> {
              final ConfigView.Double annotation = (ConfigView.Double) key;
              return factory.createDouble(annotation);
            }));
    handlers.put(
        ConfigView.Duration.class,
        checkType(
            Duration.class,
            (key, returnType) -> {
              final ConfigView.Duration annotation = (ConfigView.Duration) key;
              return factory.createDuration(annotation);
            }));
    handlers.put(
        ConfigView.Map.class,
        checkType(
            Map.class,
            (key, returnType) -> {
              final ConfigView.Map annotation = (ConfigView.Map) key;
              return factory.createMap(annotation);
            }));
    handlers.put(
        ConfigView.Configuration.class,
        (key, returnType) -> {
          final ConfigView.Configuration annotation = (ConfigView.Configuration) key;
          return factory.createConfig(annotation, returnType);
        });
    handlers.put(
        ConfigView.Bytes.class,
        checkType(
            Long.class,
            (key, returnType) -> {
              final ConfigView.Bytes annotation = (ConfigView.Bytes) key;
              return factory.createBytes(annotation);
            }));
    return handlers;
  }

  private static <T> AnnotationHandler<T> checkType(
      Class<T> expectedType, AnnotationHandler<T> handler) {
    return (annotation, returnType) -> {
      if (!expectedType.equals(wrapPrimitiveClass(returnType))) {
        throw new IllegalArgumentException(
            "Annotation ["
                + annotation.toString()
                + "] expects ["
                + expectedType
                + "] return type, but returns ["
                + returnType
                + "].");
      }
      return handler.handle(annotation, returnType);
    };
  }

  /**
   * Wrap supported primitive class
   *
   * @param clazz to wrap
   * @return wrapped class if primitive, clazz otherwise
   */
  private static Class<?> wrapPrimitiveClass(Class<?> clazz) {
    if (!clazz.isPrimitive()) {
      return clazz;
    }
    switch (clazz.getName()) {
      case "boolean":
        return Boolean.class;
      case "int":
        return Integer.class;
      case "long":
        return Long.class;
      case "double":
        return Double.class;
      default:
        return clazz;
    }
  }
}
