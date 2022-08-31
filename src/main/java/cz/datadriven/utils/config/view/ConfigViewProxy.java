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
import cz.datadriven.utils.config.view.annotation.ConfigView;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
class ConfigViewProxy implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -8983369061952970985L;

  static final List<Class<? extends Annotation>> ANNOTATIONS =
      Collections.unmodifiableList(
          Arrays.asList(
              ConfigView.String.class,
              ConfigView.StringList.class,
              ConfigView.Boolean.class,
              ConfigView.Integer.class,
              ConfigView.Long.class,
              ConfigView.Double.class,
              ConfigView.Duration.class,
              ConfigView.Configuration.class,
              ConfigView.View.class,
              ConfigView.ViewList.class,
              ConfigView.TypesafeConfig.class,
              ConfigView.Bytes.class,
              ConfigView.Map.class));

  /**
   * Unquote string (if it starts and end with a quote)
   *
   * @param key Maybe quoted key.
   * @return Unquoted key.
   */
  private static String unquote(String key) {
    if (key.length() > 2 && key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
      return key.substring(1, key.length() - 1);
    }
    return key;
  }

  static class Factory implements Serializable {

    private static final long serialVersionUID = 62698747501317112L;

    private final SerializableConfig config;

    Factory(Config config) {
      this.config = new SerializableConfig(config);
    }

    String createString(ConfigView.String annotation) {
      return getConfig().getString(annotation.path());
    }

    List<String> createStringList(ConfigView.StringList annotation) {
      return getConfig().getStringList(annotation.path());
    }

    boolean createBoolean(ConfigView.Boolean annotation) {
      return getConfig().getBoolean(annotation.path());
    }

    int createInteger(ConfigView.Integer annotation) {
      return getConfig().getInt(annotation.path());
    }

    long createLong(ConfigView.Long annotation) {
      return getConfig().getLong(annotation.path());
    }

    double createDouble(ConfigView.Double annotation) {
      return getConfig().getDouble(annotation.path());
    }

    <T> T createConfig(ConfigView.Configuration annotation, Class<T> clazz) {
      return ConfigViewFactory.create(clazz, getConfig().getConfig(annotation.path()));
    }

    <T> T createConfig(ConfigView.View annotation, Class<T> clazz) {
      return ConfigViewFactory.create(clazz, getConfig().getConfig(annotation.path()));
    }

    Config createTypeSafeConfig(ConfigView.TypesafeConfig annotation) {
      return getConfig().getConfig(annotation.path());
    }

    Duration createDuration(ConfigView.Duration annotation) {
      return getConfig().getDuration(annotation.path());
    }

    Map<String, Object> createMap(ConfigView.Map annotation) {
      return getConfig().getConfig(annotation.path()).entrySet().stream()
          .collect(Collectors.toMap(e -> unquote(e.getKey()), e -> e.getValue().unwrapped()));
    }

    long createBytes(ConfigView.Bytes annotation) {
      return getConfig().getBytes(annotation.path());
    }

    <T> List<T> createConfigViewList(ConfigView.ViewList annotation, Class<T> clazz) {
      return getConfig().getConfigList(annotation.path()).stream()
          .map(c -> ConfigViewFactory.create(clazz, c))
          .collect(Collectors.toList());
    }

    Config getConfig() {
      return config.get();
    }
  }

  /**
   * Handler for a specific annotation
   *
   * @param <T>
   */
  @FunctionalInterface
  private interface AnnotationHandler<T> extends Serializable {

    T handle(Annotation annotation, Class<T> rawType, Type genericType);
  }

  private final ConcurrentHashMap<String, Object> trackedInstruments = new ConcurrentHashMap<>();
  private final Map<Class<?>, AnnotationHandler<?>> annotationHandlers;
  private final Factory factory;

  ConfigViewProxy(Factory factory) {
    this.factory = factory;
    this.annotationHandlers = createAnnotationHandlers(factory);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    final Optional<Annotation> maybeAnnotation = getInstrumentAnnotation(method);
    if (maybeAnnotation.isPresent()) {
      return getOrCreateInstrument(
          method.getName(),
          method.getReturnType(),
          method.getGenericReturnType(),
          maybeAnnotation.get());
    } else if (proxy instanceof RawConfigAware
        && RawConfigAware.GET_RAW_CONFIG_METHOD_NAME.equals(method.getName())) {
      return factory.getConfig();
    } else {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getOrCreateInstrument(
      String key, Class<T> returnTypeRaw, Type returnType, Annotation annotation) {
    return (T)
        trackedInstruments.computeIfAbsent(
            key,
            x -> {
              final AnnotationHandler<T> handler =
                  (AnnotationHandler<T>) annotationHandlers.get(annotation.annotationType());
              if (handler == null) {
                throw new IllegalStateException(
                    "Handler for annotation [ "
                        + annotation.annotationType()
                        + " ] is not registered.");
              }
              return handler.handle(annotation, returnTypeRaw, returnType);
            });
  }

  private Optional<Annotation> getInstrumentAnnotation(Method method) {
    final List<Annotation> annotations =
        Arrays.stream(method.getDeclaredAnnotations())
            .filter(a -> ANNOTATIONS.contains(a.annotationType()))
            .collect(Collectors.toList());
    if (annotations.isEmpty()) {
      return Optional.empty();
    } else if (annotations.size() == 1) {
      return Optional.of(annotations.get(0));
    } else {
      throw new IllegalArgumentException(
          "Method [ " + method + " ] has more than one instrument annotation.");
    }
  }

  static boolean canProxy(Class<?> clazz) {
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
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.String annotation = (ConfigView.String) key;
              return factory.createString(annotation);
            }));
    handlers.put(
        ConfigView.StringList.class,
        checkType(
            List.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.StringList annotation = (ConfigView.StringList) key;
              return factory.createStringList(annotation);
            }));
    handlers.put(
        ConfigView.Boolean.class,
        checkType(
            Boolean.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Boolean annotation = (ConfigView.Boolean) key;
              return factory.createBoolean(annotation);
            }));
    handlers.put(
        ConfigView.Integer.class,
        checkType(
            Integer.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Integer annotation = (ConfigView.Integer) key;
              return factory.createInteger(annotation);
            }));
    handlers.put(
        ConfigView.Long.class,
        checkType(
            Long.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Long annotation = (ConfigView.Long) key;
              return factory.createLong(annotation);
            }));
    handlers.put(
        ConfigView.Double.class,
        checkType(
            Double.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Double annotation = (ConfigView.Double) key;
              return factory.createDouble(annotation);
            }));
    handlers.put(
        ConfigView.Duration.class,
        checkType(
            Duration.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Duration annotation = (ConfigView.Duration) key;
              return factory.createDuration(annotation);
            }));
    handlers.put(
        ConfigView.Map.class,
        checkType(
            Map.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Map annotation = (ConfigView.Map) key;
              return factory.createMap(annotation);
            }));
    handlers.put(
        ConfigView.Configuration.class,
        (key, returnTypeRaw, returnType) -> {
          final ConfigView.Configuration annotation = (ConfigView.Configuration) key;
          return factory.createConfig(annotation, returnTypeRaw);
        });
    handlers.put(
        ConfigView.View.class,
        (key, returnTypeRaw, returnType) -> {
          final ConfigView.View annotation = (ConfigView.View) key;
          return factory.createConfig(annotation, returnTypeRaw);
        });
    handlers.put(
        ConfigView.ViewList.class,
        checkType(
            List.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.ViewList annotation = (ConfigView.ViewList) key;
              final ParameterizedType parameterizedType = (ParameterizedType) returnType;
              if (parameterizedType.getActualTypeArguments().length != 1) {
                throw new IllegalStateException(
                    String.format(
                        "Expected exactly one type parameter for [%s] return type at [%s].",
                        returnTypeRaw, annotation.path()));
              }
              final Class<?> elementClass =
                  (Class<?>) parameterizedType.getActualTypeArguments()[0];
              return factory.createConfigViewList(annotation, elementClass);
            }));
    handlers.put(
        ConfigView.TypesafeConfig.class,
        (key, returnTypeRaw, returnType) -> {
          final ConfigView.TypesafeConfig annotation = (ConfigView.TypesafeConfig) key;
          return factory.createTypeSafeConfig(annotation);
        });
    handlers.put(
        ConfigView.Bytes.class,
        checkType(
            Long.class,
            (key, returnTypeRaw, returnType) -> {
              final ConfigView.Bytes annotation = (ConfigView.Bytes) key;
              return factory.createBytes(annotation);
            }));
    return handlers;
  }

  private static <T> AnnotationHandler<T> checkType(
      Class<T> expectedType, AnnotationHandler<T> handler) {
    return (annotation, returnTypeRaw, returnType) -> {
      if (!expectedType.equals(wrapPrimitiveClass(returnTypeRaw))) {
        throw new IllegalArgumentException(
            "Annotation ["
                + annotation.toString()
                + "] expects ["
                + expectedType
                + "] return type, but returns ["
                + returnType
                + "].");
      }
      return handler.handle(annotation, returnTypeRaw, returnType);
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
