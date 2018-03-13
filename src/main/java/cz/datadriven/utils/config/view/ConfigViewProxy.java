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
package cz.datadriven.utils.config.view;

import com.typesafe.config.Config;
import cz.datadriven.utils.config.view.annotation.ConfigView;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

class ConfigViewProxy implements MethodInterceptor {

  private static final List<Class<? extends Annotation>> ANNOTATIONS =
      Arrays.asList(
          ConfigView.String.class,
          ConfigView.StringList.class,
          ConfigView.Boolean.class,
          ConfigView.Integer.class,
          ConfigView.Duration.class,
          ConfigView.Configuration.class);

  static class Factory {

    private final Config config;

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

    <T> T createConfig(ConfigView.Configuration annotation, Class<T> claz) {
      return ConfigViewFactory.create(claz, config.getConfig(annotation.path()));
    }

    Duration createDuration(ConfigView.Duration annotation) {
      return config.getDuration(annotation.path());
    }

    Map<String, Object> createMap(ConfigView.Map annotation) {
      return config
          .getConfig(annotation.path())
          .entrySet()
          .stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().unwrapped()));
    }
  }

  private final ConcurrentHashMap<String, Object> trackedInstruments = new ConcurrentHashMap<>();
  private final Map<Class, Function<Annotation, Object>> annotationHandlers;
  private final Factory factory;

  ConfigViewProxy(Factory factory) {
    this.factory = factory;
    annotationHandlers = createPrimitiveTypeAnnotationHandlers(factory);
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

  private Object getOrCreateInstrument(String key, Class<?> returnType, Annotation annotation) {
    return trackedInstruments.computeIfAbsent(
        key,
        x -> {
          if (ConfigView.Configuration.class.equals(annotation.annotationType())) {
            return factory.createConfig((ConfigView.Configuration) annotation, returnType);
          }

          final Function<Annotation, Object> handler =
              annotationHandlers.get(annotation.annotationType());
          if (handler == null) {
            throw new IllegalStateException(
                "Handler for annotation [ "
                    + annotation.annotationType()
                    + " ] is not registered.");
          }
          return handler.apply(annotation);
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

  private static Map<Class, Function<Annotation, Object>> createPrimitiveTypeAnnotationHandlers(
      Factory factory) {
    final Map<Class, Function<Annotation, Object>> handlers = new HashMap<>();
    handlers.put(
        ConfigView.String.class,
        key -> {
          final ConfigView.String annotation = (ConfigView.String) key;
          return factory.createString(annotation);
        });
    handlers.put(
        ConfigView.StringList.class,
        key -> {
          final ConfigView.StringList annotation = (ConfigView.StringList) key;
          return factory.createStringList(annotation);
        });
    handlers.put(
        ConfigView.Boolean.class,
        key -> {
          final ConfigView.Boolean annotation = (ConfigView.Boolean) key;
          return factory.createBoolean(annotation);
        });
    handlers.put(
        ConfigView.Integer.class,
        key -> {
          final ConfigView.Integer annotation = (ConfigView.Integer) key;
          return factory.createInteger(annotation);
        });
    handlers.put(
        ConfigView.Duration.class,
        key -> {
          final ConfigView.Duration annotation = (ConfigView.Duration) key;
          return factory.createDuration(annotation);
        });
    handlers.put(
        ConfigView.Map.class,
        key -> {
          final ConfigView.Map annotation = (ConfigView.Map) key;
          return factory.createMap(annotation);
        });
    return handlers;
  }
}
