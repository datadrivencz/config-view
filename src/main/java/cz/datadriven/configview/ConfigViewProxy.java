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
import cz.datadriven.configview.annotation.ConfigView;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

class ConfigViewProxy implements MethodInterceptor {
  
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

    Duration createDuration(ConfigView.Duration annotation) {
      return config.getDuration(annotation.path());
    }

    long createMillis(ConfigView.Millis annotation) {
      return config.getDuration(annotation.path(), TimeUnit.MILLISECONDS);
    }
  }

  private final ConcurrentHashMap<String, Object> trackedInstruments = new ConcurrentHashMap<>();
  private final Map<Class, Function<Annotation, Object>> annotationHandlers;

  ConfigViewProxy(Factory factory) {
    annotationHandlers = createAnnotationHandlers(factory);
  }

  @Override
  public Object intercept(
      Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
    final Optional<Annotation> maybeAnnotation = getInstrumentAnnotation(method);
    if (maybeAnnotation.isPresent()) {
      return getOrCreateInstrument(method.getName(), maybeAnnotation.get());
    } else {
      return methodProxy.invokeSuper(obj, args);
    }
  }

  private Object getOrCreateInstrument(String key, Annotation annotation) {
    return trackedInstruments.computeIfAbsent(key, x -> {
      final Function<Annotation, Object> handler =
          annotationHandlers.get(annotation.annotationType());
      if (handler == null) {
        throw new IllegalStateException(
            "Handler for annotation [ " + annotation.annotationType() + " ] is not registered.");
      }
      return handler.apply(annotation);
    });
  }

  private Optional<Annotation> getInstrumentAnnotation(Method method) {
    final List<Annotation> annotations = Arrays.stream(method.getDeclaredAnnotations())
        .filter(a -> annotationHandlers.containsKey(a.annotationType()))
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

  private static Map<Class, Function<Annotation, Object>> createAnnotationHandlers(
      Factory factory) {
    final Map<Class, Function<Annotation, Object>> handlers = new HashMap<>();
    handlers.put(ConfigView.String.class, x -> {
      final ConfigView.String annotation = (ConfigView.String) x;
      return factory.createString(annotation);
    });
    handlers.put(ConfigView.StringList.class, x -> {
      final ConfigView.StringList annotation = (ConfigView.StringList) x;
      return factory.createStringList(annotation);
    });
    handlers.put(ConfigView.Boolean.class, x -> {
      final ConfigView.Boolean annotation = (ConfigView.Boolean) x;
      return factory.createBoolean(annotation);
    });
    handlers.put(ConfigView.Duration.class, x -> {
      final ConfigView.Duration annotation = (ConfigView.Duration) x;
      return factory.createDuration(annotation);
    });
    handlers.put(ConfigView.Millis.class, x -> {
      final ConfigView.Millis annotation = (ConfigView.Millis) x;
      return factory.createMillis(annotation);
    });
    return handlers;
  }

}
