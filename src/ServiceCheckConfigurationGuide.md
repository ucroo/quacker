# Service Check Configuration Guide

This document explains how to write service check configurations for the monitoring system based on the XML parser implementation in `ServiceCheckConfigurator.scala` and `Matching.scala`.

## Table of Contents
- [Configuration Structure](#configuration-structure)
- [Common Service Check Elements](#common-service-check-elements)
- [Service Check Types](#service-check-types)
- [Matchers and Thresholds](#matchers-and-thresholds)
- [Examples](#examples)

## Configuration Structure

The configuration file follows this XML structure:

```xml
<configuration>
  <services>
    <service name="serviceName">
      <label>Service Display Label</label>
      <server name="serverName">
        <label>Server Display Label</label>
        <serviceCheck name="checkName">
          <!-- Service check configuration here -->
        </serviceCheck>
      </server>
    </service>
  </services>
</configuration>
```

## Common Service Check Elements

All service checks share the following common elements:

### Required Elements

- **`<type>`** - The type of service check to perform (see [Service Check Types](#service-check-types))
- **`<label>`** - Display label for the service check
- **`name` attribute** - Unique identifier for the service check

### Optional Elements

- **`<mode>`** - Service check mode (default: "test")
  - Values: `test`, `production`, `disabled`

- **`<severity>`** - Alert severity level (default: "alert")
  - Values: `alert`, `warning`, `info`

- **`<period>`** - Check execution period in seconds (default: 60)

- **`<timeout>`** - Check timeout in milliseconds

- **`<requiredSequentialFailures>`** - Number of consecutive failures before alerting

- **`<expectFail>`** - Boolean flag to invert success/failure logic (default: false)

## Service Check Types

### 1. Information Types

#### `information`
Displays static information without performing checks.

```xml
<serviceCheck name="infoCheck">
  <label>System Information</label>
  <type>information</type>
  <html><![CDATA[<div>Information content here</div>]]></html>
</serviceCheck>
```

#### `endpoint_information`
Displays endpoint information with optional HTML description.

```xml
<serviceCheck name="endpoints">
  <label>API Endpoints</label>
  <type>endpoint_information</type>
  <html><![CDATA[<div>API Documentation</div>]]></html>
  <endpoint>
    <name>User API</name>
    <url>https://api.example.com/users</url>
    <description>User management endpoint</description>
  </endpoint>
  <endpoint>
    <name>Auth API</name>
    <url>https://api.example.com/auth</url>
    <description>Authentication endpoint</description>
  </endpoint>
</serviceCheck>
```

### 2. Mock Types (for testing)

#### `counting_mock`
A counter-based mock sensor for testing.

```xml
<serviceCheck name="mockCounter">
  <label>Mock Counter</label>
  <type>counting_mock</type>
  <period>5</period>
</serviceCheck>
```

#### `waiting_mock`
A mock sensor with configurable wait times.

```xml
<serviceCheck name="mockWait">
  <label>Mock Wait</label>
  <type>waiting_mock</type>
  <minWait>1000</minWait>
  <variance>2000</variance>
  <period>5</period>
</serviceCheck>
```

### 3. Network Checks

#### `icmp`
Ping check using ICMP protocol.

```xml
<serviceCheck name="pingCheck">
  <label>Ping Server</label>
  <type>icmp</type>
  <host>192.168.1.1</host>
  <ipv6>false</ipv6>
  <period>30</period>
</serviceCheck>
```

#### `http`
HTTP endpoint check with optional matchers.

```xml
<serviceCheck name="httpCheck">
  <label>Web Service Check</label>
  <type>http</type>
  <url>https://example.com/health</url>
  <period>60</period>
  <header>
    <name>Authorization</name>
    <value>Bearer token123</value>
  </header>
  <thresholds>
    <matcher name="statusCode">
      <numericEquals value="200"/>
    </matcher>
    <matcher name="responseBody">
      <stringContains value="healthy"/>
    </matcher>
  </thresholds>
</serviceCheck>
```

#### `http_with_credentials`
HTTP check with basic authentication.

```xml
<serviceCheck name="authHttpCheck">
  <label>Authenticated API Check</label>
  <type>http_with_credentials</type>
  <url>https://api.example.com/status</url>
  <username>apiuser</username>
  <password>apipass</password>
  <period>60</period>
  <thresholds>
    <matcher name="statusCode">
      <numericEquals value="200"/>
    </matcher>
  </thresholds>
</serviceCheck>
```

### 4. Database Checks

#### `mysql`
MySQL database connectivity and query check.

```xml
<serviceCheck name="mysqlCheck">
  <label>MySQL Database Check</label>
  <type>mysql</type>
  <host>localhost</host>
  <database>mydb</database>
  <username>dbuser</username>
  <password>dbpass</password>
  <query>SELECT COUNT(*) as count FROM users</query>
  <period>120</period>
  <thresholds rows="all">
    <matcher name="count">
      <greaterThan value="0"/>
    </matcher>
  </thresholds>
</serviceCheck>
```

The `rows` attribute on `<thresholds>` can be:
- `all` - All rows must match
- `any` - At least one row must match
- `first` - Only the first row is checked

#### `oracle`
Oracle database check.

```xml
<serviceCheck name="oracleCheck">
  <label>Oracle Database Check</label>
  <type>oracle</type>
  <connectionUri>jdbc:oracle:thin:@localhost:1521:ORCL</connectionUri>
  <username>dbuser</username>
  <password>dbpass</password>
  <query>SELECT status FROM v$instance</query>
  <period>120</period>
  <thresholds rows="first">
    <matcher name="status">
      <stringEquals value="OPEN"/>
    </matcher>
  </thresholds>
</serviceCheck>
```

#### `mongo`
MongoDB connectivity check.

```xml
<serviceCheck name="mongoCheck">
  <label>MongoDB Check</label>
  <type>mongo</type>
  <host>localhost</host>
  <port>27017</port>
  <database>mydb</database>
  <table>mycollection</table>
  <period>60</period>
</serviceCheck>
```

### 5. Directory and Authentication Services

#### `ldap`
LDAP server check with search capability.

```xml
<serviceCheck name="ldapCheck">
  <label>LDAP Server Check</label>
  <type>ldap</type>
  <host>ldap.example.com</host>
  <username>cn=admin,dc=example,dc=com</username>
  <password>adminpass</password>
  <searchBase>dc=example,dc=com</searchBase>
  <searchTerm>(objectClass=person)</searchTerm>
  <period>300</period>
</serviceCheck>
```

#### `xmpp`
XMPP (Jabber) server connectivity check.

```xml
<serviceCheck name="xmppCheck">
  <label>XMPP Server Check</label>
  <type>xmpp</type>
  <host>xmpp.example.com</host>
  <domain>example.com</domain>
  <period>60</period>
</serviceCheck>
```

### 6. File and Storage Services

#### `samba`
Samba/SMB file share check.

```xml
<serviceCheck name="sambaCheck">
  <label>File Share Check</label>
  <type>samba</type>
  <host>fileserver.example.com</host>
  <domain>WORKGROUP</domain>
  <file>serverStatus</file>
  <username>fileuser</username>
  <password>filepass</password>
  <period>120</period>
</serviceCheck>
```

#### `svn`
Subversion repository check.

```xml
<serviceCheck name="svnCheck">
  <label>SVN Repository Check</label>
  <type>svn</type>
  <host>svn.example.com/repo</host>
  <username>svnuser</username>
  <password>svnpass</password>
  <period>300</period>
</serviceCheck>
```

### 7. Caching Services

#### `memcached`
Memcached server connectivity check.

```xml
<serviceCheck name="memcachedCheck">
  <label>Memcached Check</label>
  <type>memcached</type>
  <host>cache.example.com</host>
  <period>60</period>
</serviceCheck>
```

### 8. Monitoring Integration

#### `munin`
Basic Munin monitoring system integration.

```xml
<serviceCheck name="muninCheck">
  <label>Munin Monitoring</label>
  <type>munin</type>
  <host>localhost</host>
  <port>4949</port>
  <period>60</period>
</serviceCheck>
```

#### `munin_threshold`
Munin monitoring with threshold checking.

```xml
<serviceCheck name="muninThresholdCheck">
  <label>CPU Usage Monitor</label>
  <type>munin_threshold</type>
  <host>localhost</host>
  <port>4949</port>
  <period>60</period>
  <thresholds name="cpu" type="counter">
    <matcher name="user">
      <lessThan value="80"/>
    </matcher>
    <matcher name="system">
      <lessThan value="50"/>
    </matcher>
  </thresholds>
  <thresholds name="memory" type="gauge">
    <matcher name="used">
      <lessThan value="90"/>
    </matcher>
  </thresholds>
</serviceCheck>
```

Munin field types:
- `counter` - For percentage-based counters
- `gauge` - For absolute value measurements

### 9. Dependency Checks

#### `dependency`
Check dependencies on other service checks.

```xml
<serviceCheck name="dependencyCheck">
  <label>Service Dependencies</label>
  <type>dependency</type>
  <period>30</period>
  <thresholds check="databaseCheck" service="backend" server="prod">
    <matcher name="lastStatus">
      <booleanEquals value="true"/>
    </matcher>
    <matcher name="checkIsRunning">
      <booleanEquals value="false"/>
    </matcher>
  </thresholds>
  <thresholds check="cacheCheck">
    <matcher name="lastStatus">
      <booleanEquals value="true"/>
    </matcher>
  </thresholds>
</serviceCheck>
```

Dependency matcher attributes:
- `check` - Name of the service check to depend on (required)
- `service` - Service name (optional)
- `server` - Server name (optional)
- `serviceCheckMode` - Mode filter (optional)

Available dependency matchers:
- `lastCheckBegin` - Timestamp of last check start
- `lastUptime` - Timestamp of last successful check
- `lastCheckCompleted` - Timestamp of last check completion
- `lastWhy` - String reason for last status
- `lastDetails` - String details from last check
- `lastStatus` - Boolean success/failure status
- `checkIsRunning` - Boolean indicating if check is currently running

### 10. Advanced Check Types

#### `matcher`
Generic matcher-based check.

```xml
<serviceCheck name="customMatcher">
  <label>Custom Matcher Check</label>
  <type>matcher</type>
  <period>60</period>
  <matcher>
    <stringEquals value="expected_value"/>
  </matcher>
</serviceCheck>
```

#### `script`
Execute a scripted sequence of operations.

```xml
<serviceCheck name="scriptCheck">
  <label>Scripted Check</label>
  <type>script</type>
  <period>120</period>
  <interpolator>
    <!-- Interpolator configuration -->
  </interpolator>
  <step>
    <!-- Step configuration -->
  </step>
</serviceCheck>
```

## Matchers and Thresholds

Matchers define conditions that must be met for a check to succeed. They are used within `<thresholds>` elements.

### Numeric Matchers

#### `<lessThan>`
Value must be less than the specified number.

```xml
<matcher name="responseTime">
  <lessThan value="500"/>
</matcher>
```

#### `<greaterThan>`
Value must be greater than the specified number.

```xml
<matcher name="rowCount">
  <greaterThan value="0"/>
</matcher>
```

#### `<numericEquals>`
Value must equal the specified number.

```xml
<matcher name="statusCode">
  <numericEquals value="200"/>
</matcher>
```

### String Matchers

#### `<stringEquals>`
String must match exactly (case-sensitive).

```xml
<matcher name="status">
  <stringEquals value="RUNNING"/>
</matcher>
```

#### `<stringEqualsIgnoreCase>`
String must match (case-insensitive).

```xml
<matcher name="status">
  <stringEqualsIgnoreCase value="running"/>
</matcher>
```

#### `<stringContains>`
String must contain the specified substring (case-sensitive).

```xml
<matcher name="responseBody">
  <stringContains value="success"/>
</matcher>
```

#### `<stringContainsIgnoreCase>`
String must contain the specified substring (case-insensitive).

```xml
<matcher name="message">
  <stringContainsIgnoreCase value="error"/>
</matcher>
```

#### `<regexMatches>`
String must match the specified regular expression.

```xml
<matcher name="version">
  <regexMatches value="^\d+\.\d+\.\d+$"/>
</matcher>
```

### Boolean Matchers

#### `<booleanEquals>`
Boolean value must match.

```xml
<matcher name="isEnabled">
  <booleanEquals value="true"/>
</matcher>
```

### Logical Combinators

Matchers can be combined using logical operators:

#### `<all>`
All sub-matchers must pass (AND logic).

```xml
<matcher name="complexCheck">
  <all>
    <greaterThan value="10"/>
    <lessThan value="100"/>
  </all>
</matcher>
```

#### `<some>`
At least one sub-matcher must pass (OR logic).

```xml
<matcher name="acceptableStatus">
  <some>
    <stringEquals value="OK"/>
    <stringEquals value="DEGRADED"/>
  </some>
</matcher>
```

#### `<none>`
No sub-matchers should pass (NOT logic).

```xml
<matcher name="noErrors">
  <none>
    <stringContains value="ERROR"/>
    <stringContains value="FATAL"/>
  </none>
</matcher>
```

#### `<someNot>`
At least one sub-matcher must fail.

```xml
<matcher name="someNotPassing">
  <someNot>
    <lessThan value="50"/>
    <greaterThan value="100"/>
  </someNot>
</matcher>
```

#### `<notSome>`
Not all sub-matchers should pass (NAND logic).

```xml
<matcher name="notAllConditions">
  <notSome>
    <booleanEquals value="true"/>
    <greaterThan value="10"/>
  </notSome>
</matcher>
```

## Examples

### Example 1: Complete Service Configuration

```xml
<configuration>
  <services>
    <service name="webService">
      <label>Web Application Service</label>
      <server name="production">
        <label>Production Server</label>

        <!-- Ping Check -->
        <serviceCheck name="pingServer">
          <label>Server Reachability</label>
          <mode>production</mode>
          <severity>alert</severity>
          <type>icmp</type>
          <host>192.168.1.100</host>
          <period>30</period>
          <requiredSequentialFailures>3</requiredSequentialFailures>
        </serviceCheck>

        <!-- HTTP Health Check -->
        <serviceCheck name="httpHealth">
          <label>Application Health</label>
          <mode>production</mode>
          <severity>alert</severity>
          <type>http</type>
          <url>https://app.example.com/health</url>
          <timeout>5000</timeout>
          <period>60</period>
          <thresholds>
            <matcher name="statusCode">
              <numericEquals value="200"/>
            </matcher>
            <matcher name="responseBody">
              <all>
                <stringContains value="status"/>
                <stringContains value="healthy"/>
              </all>
            </matcher>
          </thresholds>
        </serviceCheck>

        <!-- Database Check -->
        <serviceCheck name="dbCheck">
          <label>Database Connectivity</label>
          <mode>production</mode>
          <severity>alert</severity>
          <type>mysql</type>
          <host>db.example.com</host>
          <database>production</database>
          <username>monitor</username>
          <password>secure_password</password>
          <query>SELECT COUNT(*) as active_users FROM sessions WHERE last_activity > NOW() - INTERVAL 5 MINUTE</query>
          <period>120</period>
          <thresholds rows="first">
            <matcher name="active_users">
              <greaterThan value="0"/>
            </matcher>
          </thresholds>
        </serviceCheck>

        <!-- Cache Check -->
        <serviceCheck name="cacheCheck">
          <label>Cache Service</label>
          <mode>production</mode>
          <severity>warning</severity>
          <type>memcached</type>
          <host>cache.example.com</host>
          <period>60</period>
        </serviceCheck>

        <!-- Dependency Check -->
        <serviceCheck name="overallHealth">
          <label>Overall System Health</label>
          <mode>production</mode>
          <severity>alert</severity>
          <type>dependency</type>
          <period>30</period>
          <thresholds check="httpHealth">
            <matcher name="lastStatus">
              <booleanEquals value="true"/>
            </matcher>
          </thresholds>
          <thresholds check="dbCheck">
            <matcher name="lastStatus">
              <booleanEquals value="true"/>
            </matcher>
            <matcher name="lastCheckCompleted">
              <greaterThan value="0"/>
            </matcher>
          </thresholds>
        </serviceCheck>

      </server>
    </service>
  </services>
</configuration>
```

### Example 2: Advanced HTTP Check with Custom Headers

```xml
<serviceCheck name="apiCheck">
  <label>REST API Endpoint</label>
  <mode>production</mode>
  <severity>alert</severity>
  <type>http</type>
  <url>https://api.example.com/v1/status</url>
  <timeout>3000</timeout>
  <period>45</period>
  <header>
    <name>X-API-Key</name>
    <value>your-api-key-here</value>
  </header>
  <header>
    <name>Accept</name>
    <value>application/json</value>
  </header>
  <thresholds>
    <matcher name="statusCode">
      <some>
        <numericEquals value="200"/>
        <numericEquals value="201"/>
      </some>
    </matcher>
    <matcher name="responseBody">
      <all>
        <stringContains value="status"/>
        <none>
          <stringContainsIgnoreCase value="error"/>
          <stringContainsIgnoreCase value="down"/>
        </none>
      </all>
    </matcher>
  </thresholds>
</serviceCheck>
```

### Example 3: Complex Database Query with Thresholds

```xml
<serviceCheck name="orderProcessing">
  <label>Order Processing Monitor</label>
  <mode>production</mode>
  <severity>warning</severity>
  <type>mysql</type>
  <host>db.example.com</host>
  <database>orders</database>
  <username>monitoring</username>
  <password>monitoring_password</password>
  <query>
    SELECT
      COUNT(*) as pending_orders,
      AVG(processing_time) as avg_time,
      MAX(created_at) as last_order
    FROM orders
    WHERE status = 'pending'
    AND created_at > NOW() - INTERVAL 1 HOUR
  </query>
  <period>180</period>
  <thresholds rows="first">
    <matcher name="pending_orders">
      <lessThan value="1000"/>
    </matcher>
    <matcher name="avg_time">
      <all>
        <greaterThan value="0"/>
        <lessThan value="300"/>
      </all>
    </matcher>
  </thresholds>
</serviceCheck>
```

## Configuration Best Practices

1. **Use meaningful names**: Service check names should be descriptive and unique within a server.

2. **Set appropriate periods**: Balance between timely detection and system load. Common values:
   - Critical checks: 30-60 seconds
   - Standard checks: 120-300 seconds
   - Low-priority checks: 600+ seconds

3. **Configure timeouts**: Always set timeouts for network-based checks to prevent hanging checks.

4. **Use sequential failure thresholds**: Set `requiredSequentialFailures` to avoid false positives from transient issues.

5. **Severity levels**:
   - `alert`: Critical issues requiring immediate attention
   - `warning`: Issues that should be investigated but aren't immediately critical
   - `info`: Informational checks for monitoring trends

6. **Testing with modes**:
   - Use `test` mode for new checks before promoting to `production`
   - Use `disabled` mode to temporarily suspend checks without removing them

7. **Secure credentials**: Store sensitive information securely and avoid committing passwords to version control.

8. **Logical matcher combinations**: Use `<all>`, `<some>`, and `<none>` to create sophisticated validation logic that accurately represents your success criteria.

## Reference

- Implementation: `src/main/scala/metl/model/ServiceCheckConfigurator.scala`
- Matchers: `src/main/scala/metl/model/Matching.scala`
- Example: `monitoringDashboardConfig/services.xml`
