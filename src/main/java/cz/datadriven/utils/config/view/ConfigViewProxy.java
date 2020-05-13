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
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

class ConfigViewProxy implements MethodInterceptor, Serializable {

  private static final long serialVersionUID = -8983369061952970985L;

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
          ConfigView.TypesafeConfig.class,
          ConfigView.Bytes.class,
          ConfigView.Map.class);

  static class Factory implements Serializable {

    private static final long serialVersionUID = 62698747501317112L;

    private transient Config config;

    Factory(Config config) {
      this.config = config;
    }

    String createString(ConfigView.String annotation) {
      return config.getString(annotation.path());
    }

    List<String> createStringList(ConfigView.StringList annotation) {
      return config.getStringList(annotation.path());
    }

    boolean createBoolean(ConfigView.Boolean annotation) {
      return config.getBoolean(annotation.path());
    }

    int createInteger(ConfigView.Integer annotation) {
      return config.getInt(annotation.path());
    }

    long createLong(ConfigView.Long annotation) {
      return config.getLong(annotation.path());
    }

    double createDouble(ConfigView.Double annotation) {
      return config.getDouble(annotation.path());
    }

    <T> T createConfig(ConfigView.Configuration annotation, Class<T> claz) {
      return ConfigViewFactory.create(claz, config.getConfig(annotation.path()));
    }

    Config createTypeSafeConfig(ConfigView.TypesafeConfig annotation) {
      return config.getConfig(annotation.path());
    }

    Duration createDuration(ConfigView.Duration annotation) {
      return config.getDuration(annotation.path());
    }

    Map<String, Object> createMap(ConfigView.Map annotation) {
      return config.getConfig(annotation.path()).entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().unwrapped()));
    }

    long createBytes(ConfigView.Bytes annotation) {
      return config.getBytes(annotation.path());
    }

    Config getConfig() {
      return config;
    }

    private void readObject(ObjectInputStream aInputStream)
        throws ClassNotFoundException, IOException {
      config = ConfigFactory.parseString(aInputStream.readUTF());
    }

    private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
      aOutputStream.writeUTF(config.root().render(ConfigRenderOptions.concise()));
    }
  }
  /**
   * Handler for a specific annotation
   *
   * @param <T>
   */
  @FunctionalInterface
  private interface AnnotationHandler<T> extends Serializable {

    T handle(Annotation annotation, Class<T> returnType);
  }

  private final ConcurrentHashMap<String, Object> trackedInstruments = new ConcurrentHashMap<>();
  private final Map<Class<?>, AnnotationHandler<?>> annotationHandlers;
  private final Factory factory;

  ConfigViewProxy(Factory factory) {
    this.factory = factory;
    this.annotationHandlers = createAnnotationHandlers(factory);
  }

  @Override
  public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy)
      throws Throwable {
    final Optional<Annotation> maybeAnnotation = getInstrumentAnnotation(method);
    if (maybeAnnotation.isPresent()) {
      return getOrCreateInstrument(method.getName(), method.getReturnType(), maybeAnnotation.get());
    } else if (obj instanceof RawConfigAware
        && RawConfigAware.GET_RAW_CONFIG_METHOD_NAME.equals(method.getName())) {
      return factory.getConfig();
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
        ConfigView.TypesafeConfig.class,
        (key, returnType) -> {
          final ConfigView.TypesafeConfig annotation = (ConfigView.TypesafeConfig) key;
          return factory.createTypeSafeConfig(annotation);
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
