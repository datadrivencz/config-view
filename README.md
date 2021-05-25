[![Build Status](https://travis-ci.org/datadrivencz/config-view.svg?branch=master)](https://travis-ci.org/github/datadrivencz/config-view)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=datadrivencz_config-view&metric=alert_status)](https://sonarcloud.io/dashboard?id=datadrivencz_config-view)
[![Maven Version](https://maven-badges.herokuapp.com/maven-central/cz.datadriven.utils/config-view/badge.svg)](https://search.maven.org/search?q=g:cz.datadriven.utils&a=config-view")

# DataDriven Config View

Simple tool for creating views over the [typesafe config](https://github.com/lightbend/config).

## Usage

Lets take the following config as an example:

```java
myapp {
  kafka {
    brokers = ["broker1:9092", "broker2:9092", "broker3.9092"]
    group-id = "my-group-id"
    shutdown-timeout = 1 second
  }
}
```

If you want this config in your app, you will meet a lot of obstacles along the way. The main
problem is, that the config contract is not clearly defined and mocking configuration in yours tests
will be painful.

To make the configuration less painful to use, let's define a clear contract for our application.

```java
@ConfigView
interface MyConfigView {

  @ConfigView.StringList(path = "brokers")
  List<String> brokers();
  
  @ConfigView.String(path = "my-group-id")
  String groupId();
  
  @ConfigView.Duration(path = "shutdown-timeout")
  Duration shutdownTimeout();
}
```

Now we can simply create a view over our config and make the application great again ;)

```java
final MyConfigView view = ConfigViewFactory.create(MyConfigView.class, config, "myapp.kafka");
```

## Building

To build the Config View artifacts, the following dependencies are required:
- Git
- Java 8

Building the project itself is a matter of:

```
git clone https://github.com/datadrivencz/configview
cd configview
./gradlew publishToMavenLocal
```

## Contact us

Feel free to open an issue in the [issue tracker](https://github.com/datadrivencz/configview/issues).

## License

Config View is licensed under the terms of the Apache License 2.0.

## Project information
- Config View is built on top of [cglib](https://github.com/cglib/cglib)
- Java code is formatted using [google-format-java](https://github.com/google/google-java-format)
- Code is built using [gradle](https://gradle.org/)