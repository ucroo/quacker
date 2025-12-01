# Access Control Lists Configuration Guide

## Overview

The Access Control Lists (ACL) configuration allows you to define granular user access permissions for services and servers in the Quacker system. The configuration uses XML format and is parsed by the code in [AccessControlLists.scala](main/scala/metl/model/AccessControlLists.scala).

## Configuration Structure

### Root Element: `validUsers`

The configuration file should contain a `<validUsers>` element that wraps all user access restrictions.

```xml
<validUsers>
  <!-- User access restrictions go here -->
</validUsers>
```

### User Access Restriction: `validUser`

Each user's permissions are defined within a `<validUser>` element. This element is parsed by the `UserAccessRestriction.configureFromXml` method.

**Required child elements:**
- `<authcate>`: The username/authentication identifier for the user

**Optional child elements:**
- `<servicePermissions>`: Container for service-level permissions

### Service Permissions Structure

Service permissions are defined within the `<servicePermissions>` element, which contains one or more `<service>` elements.

Each `<service>` element defines permissions for a specific service and is parsed by `ServicePermission.configureFromXml`.

**Required attributes:**
- `name`: The service name (string)

**Optional child elements:**
- `<allowedServiceCheckModes>`: Whitelist of service check modes
- `<disallowedServiceCheckModes>`: Blacklist of service check modes
- `<allowedServers>`: Whitelist of server names
- `<disallowedServers>`: Blacklist of server names

## Service Check Modes

Valid service check mode values (case-insensitive):
- `staging`
- `production`
- `test`
- `development`
- `operations`

These are defined in `Notifiers.scala` and parsed by the `ServiceCheckMode.parse` method.

## Complete Configuration Example

```xml
<validUsers>
  <!-- User with full access to a service -->
  <validUser>
    <authcate>john.doe@example.com</authcate>
    <servicePermissions>
      <service name="analytics">
        <!-- No restrictions means all servers and modes are allowed -->
      </service>
    </servicePermissions>
  </validUser>

  <!-- User with restricted server access -->
  <validUser>
    <authcate>jane.smith@example.com</authcate>
    <servicePermissions>
      <service name="reporting">
        <allowedServers>
          <server name="server1"/>
          <server name="server2"/>
        </allowedServers>
      </service>
    </servicePermissions>
  </validUser>

  <!-- User with specific service check mode access -->
  <validUser>
    <authcate>dev.user@example.com</authcate>
    <servicePermissions>
      <service name="monitoring">
        <allowedServiceCheckModes>
          <serviceCheckMode level="development"/>
          <serviceCheckMode level="test"/>
        </allowedServiceCheckModes>
      </service>
    </servicePermissions>
  </validUser>

  <!-- User with complex restrictions -->
  <validUser>
    <authcate>ops.user@example.com</authcate>
    <servicePermissions>
      <service name="dashboard">
        <allowedServiceCheckModes>
          <serviceCheckMode level="production"/>
          <serviceCheckMode level="staging"/>
        </allowedServiceCheckModes>
        <disallowedServers>
          <server name="legacy-server"/>
        </disallowedServers>
      </service>
    </servicePermissions>
  </validUser>

  <!-- User with access to multiple services -->
  <validUser>
    <authcate>admin.user@example.com</authcate>
    <servicePermissions>
      <service name="monitoring">
        <allowedServiceCheckModes>
          <serviceCheckMode level="production"/>
          <serviceCheckMode level="staging"/>
          <serviceCheckMode level="development"/>
        </allowedServiceCheckModes>
      </service>
      <service name="analytics">
        <!-- All servers and modes allowed for this service -->
      </service>
    </servicePermissions>
  </validUser>

  <!-- User with no service permissions (no restrictions) -->
  <validUser>
    <authcate>guest.user@example.com</authcate>
    <servicePermissions>
      <!-- Empty means no restrictions - user can access everything -->
    </servicePermissions>
  </validUser>
</validUsers>
```

## Permission Logic

The permission system follows these rules (implemented in `ServicePermission.permit`):

### For Server Access:
1. If `allowedServers` is defined, the server must be in the whitelist
2. If `disallowedServers` is defined, the server must NOT be in the blacklist
3. Both conditions must be satisfied if both lists are defined
4. If neither list is defined, all servers are allowed

### For Service Check Modes:
1. If `allowedServiceCheckModes` is defined, the mode must be in the whitelist
2. If `disallowedServiceCheckModes` is defined, the mode must NOT be in the blacklist
3. Both conditions must be satisfied if both lists are defined
4. If neither list is defined, all modes are allowed

### For User Access:
- If a user has an empty `servicePermissions` list, they have unrestricted access
- If a user has service permissions defined, at least one permission must match for access to be granted

## Integration

The configuration is loaded by the `ValidUsers.configureFromXml` method, which:
1. Parses all `<validUser>` elements
2. Registers them with the global configuration via `Globals.setValidUsers(newUsers)`
3. Returns a list of status messages indicating how many users were loaded

## Best Practices

1. **Whitelist over Blacklist**: Prefer using `allowedServers` and `allowedServiceCheckModes` for explicit control
2. **Least Privilege**: Only grant the minimum permissions necessary for each user
3. **Service Name Matching**: Ensure service names exactly match those defined in your service configurations
4. **Server Name Matching**: Ensure server names exactly match those defined in your server configurations
5. **Empty Permissions**: Be aware that users with no `<servicePermissions>` or an empty list have unrestricted access

## Common Pitfalls

- **Case Sensitivity**: Service check modes are case-insensitive, but service and server names are case-sensitive
- **Unknown Values**: Invalid service check mode values default to `test`
- **Empty Username**: Users with empty `authcate` values are filtered out during configuration
- **Conflicting Rules**: If both whitelist and blacklist are defined, both conditions must be satisfied (intersection, not union)

## Related Documentation

See also:
- [ServiceCheckConfigurationGuide.md](ServiceCheckConfigurationGuide.md) - For configuring service checks and monitoring
- `AccessControlLists.scala` - Source code implementation
- `ConfigFileReader.scala` - XML parsing utilities
- `Notifiers.scala` - Service check mode definitions
