# Quacker

A flexible, extensible monitoring and service check system built with Scala and the Lift web framework. Quacker provides real-time monitoring of various infrastructure components including databases, web services, network connectivity, and more.

## Overview

Quacker is a monitoring dashboard that allows you to configure and track the health of your services across multiple servers. It supports a wide variety of check types and provides a web-based interface for viewing service status in real-time.

## Features

- **Multiple Service Check Types**: Support for 18+ different types of service checks including:
  - Network checks (ICMP ping, HTTP/HTTPS)
  - Database monitoring (MySQL, Oracle, MongoDB)
  - Directory services (LDAP, XMPP)
  - File services (Samba/SMB, SVN)
  - Caching services (Memcached)
  - System monitoring (Munin integration)
  - Custom dependency chains

- **Flexible Configuration**: XML-based configuration system with support for:
  - Custom check periods and timeouts
  - Multiple severity levels (alert, warning, info)
  - Sequential failure thresholds
  - Complex threshold matching with logical operators

- **Real-time Dashboard**: Web-based interface built with Lift framework featuring:
  - Live status updates using Comet
  - Service grouping by server and service type
  - Historical tracking of check results
  - Detailed error reporting

- **Advanced Matching**: Powerful threshold system supporting:
  - Numeric comparisons (greater than, less than, equals)
  - String matching (exact, contains, regex, case-insensitive)
  - Boolean conditions
  - Logical combinators (all, some, none)

- **OpenTelemetry Integration**: Built-in support for OpenTelemetry metrics to track check success/failure rates

## Technology Stack

- **Scala 2.11.12**
- **Lift Framework 3.5.0** - Web framework
- **SBT** - Build tool
- **Jetty 9.4** - Embedded web server
- **Apache Shiro** - Security and authentication
- **OpenTelemetry 1.56.0** - Observability

## Prerequisites

- Java 8 or higher
- SBT (Scala Build Tool)

## Getting Started

### Building

```bash
./sbt.sh compile
```

### Running Locally

```bash
./local.sh
```

Or using SBT:

```bash
./sbt.sh
container:start
```

### Running Tests

```bash
./sbt.sh test
```

## Configuration

Service checks are configured via XML files. The main configuration file structure:

```xml
<configuration>
  <services>
    <service name="myService">
      <label>My Service</label>
      <server name="production">
        <label>Production Server</label>
        <serviceCheck name="httpCheck">
          <label>Web Service Health</label>
          <type>http</type>
          <url>https://example.com/health</url>
          <period>60</period>
          <thresholds>
            <matcher name="statusCode">
              <numericEquals value="200"/>
            </matcher>
          </thresholds>
        </serviceCheck>
      </server>
    </service>
  </services>
</configuration>
```

For comprehensive configuration documentation, see [Service Check Configuration Guide](src/ServiceCheckConfigurationGuide.md).

### Configuration Files

- `monitoringDashboardConfig/services.xml` - Service check definitions
- `config/services.xml` - Alternative configuration location
- `src/main/resources/services.xml` - Packaged configuration

## Project Structure

```
quacker/
├── src/
│   ├── main/
│   │   ├── scala/
│   │   │   ├── bootstrap/liftweb/     # Application bootstrap
│   │   │   └── metl/
│   │   │       ├── comet/             # Real-time UI components
│   │   │       ├── lib/               # Utilities
│   │   │       ├── model/             # Core models and sensors
│   │   │       │   └── sensor/        # Service check implementations
│   │   │       ├── snippet/           # Lift snippets
│   │   │       └── view/              # View layer
│   │   ├── resources/                 # Configuration resources
│   │   └── webapp/                    # Web application assets
│   └── test/                          # Test suite
├── config/                            # Configuration directory
├── monitoringDashboardConfig/         # Dashboard configuration
└── build.sbt                          # Build configuration
```

## Available Service Check Types

- **Network**: ICMP, HTTP, HTTP with authentication
- **Databases**: MySQL, Oracle, MongoDB
- **Directory Services**: LDAP, XMPP
- **File Services**: Samba, SVN
- **Caching**: Memcached
- **Monitoring**: Munin, Munin with thresholds
- **Custom**: Dependency checks, matcher checks, scripted checks
- **Testing**: Mock sensors for development

See the [Service Check Configuration Guide](src/ServiceCheckConfigurationGuide.md) for detailed information on each type.

## Authentication

Quacker supports multiple authentication methods through Apache Shiro:
- Basic authentication
- CAS (Central Authentication Service)
- LDAP integration
- OAuth (via pac4j)

Authentication configuration is managed in the `Boot.scala` bootstrap class.

## Docker Support

A Dockerfile is provided for containerized deployment:

```bash
docker build -t quacker .
docker run -p 8080:8080 quacker
```

## Publishing

To publish to Sonatype/Maven Central:

```bash
./publish.sh
```

Credentials should be configured in `~/.ivy2/ivy-credentials`.

## Development

### IDE Setup

The project includes configuration for:
- Metals (Scala Language Server)
- Visual Studio Code
- BSP (Build Server Protocol)

### Code Formatting

Scala code is formatted using Scalafmt. Configuration is in `.scalafmt.conf`.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

Copyright 2015 Monash University

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Version

Current version: 1.1.0

## Documentation

- [Service Check Configuration Guide](src/ServiceCheckConfigurationGuide.md) - Comprehensive guide to writing service check configurations
- [Lift Framework Documentation](https://liftweb.net/) - Framework documentation
- [Apache Shiro Documentation](https://shiro.apache.org/) - Security framework documentation
