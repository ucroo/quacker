# Quacker

A real-time service monitoring dashboard built with Scala and the Lift framework. Quacker continuously monitors the health and performance of distributed services and displays their status through a live web interface.

## Quick Start

### Local Development

Run locally with SBT
```bash
local.sh
```
Or use Docker
```bash
docker build -t quacker .
docker run -p 8080:8080 \
  -v $(pwd)/appConf:/appConf \
  -v $(pwd)/config:/config \
  -e QUACKER_CONFIG_DIRECTORY_LOCATION=/config \
  -e QUACKER_APP_CONFIG_DIRECTORY_LOCATION=/appConf \
  quacker
```

## What It Does

Quacker monitors infrastructure and services by running configurable health checks (sensors) against various system components:

- **HTTP/HTTPS endpoints** - API availability and response validation
- **Databases** - SQL queries, connection health (MySQL, PostgreSQL, MongoDB)
- **Network services** - ICMP ping, Telnet, LDAP, XMPP, Memcached
- **File systems** - Samba/SMB shares, SVN repositories
- **System metrics** - Munin integration
- **Dependencies** - Service interdependency monitoring
- **Custom scripts** - Execute arbitrary health check scripts

Health check results are pushed to connected web clients in real-time via Comet (server push), providing immediate visibility into system status.

## Architecture

- **Backend**: Scala 2.12, Lift web framework, Jetty server
- **Frontend**: JavaScript (D3.js, Masonry.js), live dashboard updates
- **Configuration**: XML-based service and sensor definitions
- **Authentication**: Supports GitHub OAuth or mock authentication
- **Deployment**: Docker containers, Kubernetes-ready with monitoring integration