---
name: group-metadata-members
description: Retrieve and inspect members from an io.streamnative.pulsar.handlers.kop.coordinator.group.GroupMetadata object in a MAT heap dump using OQL queries and the inspect-object command.
license: Apache-2.0
compatibility: opencode
metadata:
  mat-cli: required
---

## Overview

This skill helps you extract consumer group membership information from a heap dump containing `io.streamnative.pulsar.handlers.kop.coordinator.group.GroupMetadata` objects.

## Finding GroupMetadata Objects

Use the `instances` command to list all `GroupMetadata` objects and identify the one you need:

```bash
mat-cli instances <heap-file> --class "io.streamnative.pulsar.handlers.kop.coordinator.group.GroupMetadata"
```

The output shows object addresses — note the address of the group you want to inspect.

## Inspecting GroupMetadata Fields

Once you have the object address, inspect its core fields:

```bash
mat-cli inspect-object <heap-file> --address <address> \
  --field-paths groupId,generationId,protocolType,protocol,members
```

- `groupId` — the consumer group ID (String)
- `generationId` — generation counter (int)
- `protocolType` — protocol type (String, e.g. "consumer")
- `protocol` — the current protocol (String, e.g. "range")
- `members` — a `Map<String, MemberMetadata>` keyed by member ID

## Extracting Member Details

`members` is a `HashMap`. To inspect an individual member, resolve the member ID key from the map and inspect its `MemberMetadata`:

```bash
mat-cli inspect-object <heap-file> --address <member-metadata-address> \
  --field-paths memberId,clientId,clientHost,sessionTimeout,rebalanceTimeout,groupInstanceId,subscription,assignment
```

Key `MemberMetadata` fields:
- `memberId` — unique member identifier (String)
- `clientId` — client ID (String)
- `clientHost` — host the client is running on (String)
- `sessionTimeout` — session timeout in ms (int)
- `rebalanceTimeout` — rebalance timeout in ms (int)
- `groupInstanceId` — static group instance ID, may be null (String)
- `subscription` — subscribed topics (usually a `SubscriptionData` or similar)
- `assignment` — byte array of partition assignment bytes (`byte[]`)

## Dumping All Members via OQL

To extract all members with their IDs and assignment sizes in one shot:

```sql
SELECT m.value.memberId.toString(), m.value.clientId.toString(), m.value.clientHost.toString(), m.value.sessionTimeout, m.value.rebalanceTimeout, m.value.assignment.@length
FROM io.streamnative.pulsar.handlers.kop.coordinator.group.GroupMetadata g
OBJECTS g.members.table m
```

> **Note**: `g.members` is a `HashMap`. The `table` reference gives access to the underlying `HashMap$Node[]`. Each node has fields `key` (String = memberId) and `value` (MemberMetadata). The `m` alias iterates over the array entries.

## Expected Output Format

When dumping group members programmatically, the JSON output follows this structure (see `group_metadata_members_ds.json` for a real example — but use only sanitized reference data):

```json
{
  "groupId": "example-group-id",
  "memberCount": 3,
  "members": [
    {
      "memberId": "consumer-1-example-uuid",
      "clientId": "consumer-1",
      "clientHost": "/10.0.0.1",
      "sessionTimeout": 45000,
      "rebalanceTimeout": 300000,
      "groupInstanceId": null,
      "subscription": {
        "topics": ["topic-a", "topic-b"],
        "userData": null
      },
      "assignmentLength": 42,
      "assignment": [
        "0x00", "0x01", "0x02", "0x03", "0x04"
      ]
    },
    {
      "memberId": "consumer-2-example-uuid",
      "clientId": "consumer-2",
      "clientHost": "/10.0.0.2",
      "sessionTimeout": 45000,
      "rebalanceTimeout": 300000,
      "groupInstanceId": "my-instance-id",
      "subscription": {
        "topics": ["topic-a"],
        "userData": null
      },
      "assignmentLength": 24,
      "assignment": [
        "0x00", "0x01", "0x02"
      ]
    },
    {
      "memberId": "consumer-3-example-uuid",
      "clientId": "consumer-3",
      "clientHost": "/10.0.0.3",
      "sessionTimeout": 30000,
      "rebalanceTimeout": 120000,
      "groupInstanceId": null,
      "subscription": {
        "topics": ["topic-c", "topic-d", "topic-e"],
        "userData": null
      },
      "assignmentLength": 60,
      "assignment": [
        "0x00", "0x01", "0x02", "0x03", "0x04",
        "0x05", "0x06", "0x07"
      ]
    }
  ]
}
```

## Decoding Partition Assignments

The `assignment` field in each member is a Kafka protocol-encoded `ConsumerMemberAssignment` byte array. A helper script is bundled at `decode_assignments.py`:

```bash
python3 .opencode/skills/group-metadata-members/decode_assignments.py <path-to-json>
```

It reads the JSON dump and produces a partitioned view like:

```
Group: my-group  (3 members)

  rdkafka-a1b2c3d4 (rdkafka-...):
    topic-a: [0, 1, 2]
    topic-b: [0, 1, 2, 3, 4]

--- Partition → Member map ---

topic-a:
  partition-  0 => rdkafka-a1b2c3d4
  partition-  1 => rdkafka-a1b2c3d4
  partition-  2 => rdkafka-a1b2c3d4

--- Coverage check ---
  topic-a: partitions 0-2 fully covered
  topic-b: missing partitions [2, 3, 4]  ← ORPHANED
```

The default input path is `group_metadata_members_ds.json` in the project root. Pass a different file as the first argument to override.

## Tips

- Use `mat-cli oql <heap-file> --query "SELECT * FROM io.streamnative.pulsar.handlers.kop.coordinator.group.GroupMetadata"` to verify the class is present in the heap.
- If `members` appears empty (`{}`) but you expect entries, the group may be in a `Dead` or `Empty` state — check `generationId` and protocol state.
