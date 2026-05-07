#!/usr/bin/env python3
"""Decode Kafka ConsumerMemberAssignment bytes from group_metadata_members_ds.json
and produce a partition→member map (e.g. "topic-partition-3 => member1").

Format: version(2) + num_topics(4) + [topicLen(2) + topic(N) + num_parts(4) + [part(4)...]] + userDataLen(4)
"""

import json
import sys
from collections import defaultdict


def decode_assignment(hex_bytes: list[str]) -> dict[str, list[int]]:
    """Return {topic: [partition, ...]} decoded from a list of '0xNN' strings."""
    raw = bytes(int(h, 16) for h in hex_bytes)
    i = 0

    def read_int(n, signed=True):
        nonlocal i
        v = int.from_bytes(raw[i:i + n], "big", signed=signed)
        i += n
        return v

    version = read_int(2)
    n_topics = read_int(4)
    result = {}
    for _ in range(n_topics):
        t_len = read_int(2)
        topic = raw[i:i + t_len].decode("utf-8")
        i += t_len
        n_parts = read_int(4)
        partitions = [read_int(4) for _ in range(n_parts)]
        result[topic] = partitions
    return result


def short_member(member_id: str) -> str:
    """Return the first 8 chars after 'rdkafka-' for readability."""
    prefix = "rdkafka-"
    if member_id.startswith(prefix):
        return "rdkafka-" + member_id[len(prefix):len(prefix) + 8]
    return member_id[:16]


def main(path: str) -> None:
    with open(path) as f:
        data = json.load(f)

    group_id = data.get("groupId", "unknown")
    members = data["members"]

    # tp_map: "topic::partition" -> [memberId, ...]
    tp_map: dict[str, list[str]] = defaultdict(list)

    print(f"Group: {group_id}  ({len(members)} members)\n")

    for m in members:
        member_id = m["memberId"]
        short = short_member(member_id)
        assignment_hex = m.get("assignment", [])
        if not assignment_hex:
            print(f"  {short}: <empty assignment>")
            continue
        try:
            topics = decode_assignment(assignment_hex)
        except Exception as e:
            print(f"  {short}: decode error — {e}")
            continue

        print(f"  {short} ({member_id}):")
        for topic, partitions in sorted(topics.items()):
            print(f"    {topic}: {sorted(partitions)}")
            for p in partitions:
                tp_map[f"{topic}::{p}"].append(short)

    print("\n--- Partition → Member map ---")
    # Group by topic for readability
    by_topic: dict[str, dict[int, list[str]]] = defaultdict(dict)
    for key, owners in tp_map.items():
        topic, part = key.rsplit("::", 1)
        by_topic[topic][int(part)] = owners

    for topic in sorted(by_topic):
        print(f"\n{topic}:")
        for part in sorted(by_topic[topic]):
            owners = by_topic[topic][part]
            owner_str = ", ".join(owners) if owners else "<unassigned>"
            flag = "  *** MULTI-OWNER ***" if len(owners) > 1 else ""
            print(f"  partition-{part:3d} => {owner_str}{flag}")

    # Report any gaps
    print("\n--- Coverage check ---")
    for topic in sorted(by_topic):
        parts = sorted(by_topic[topic].keys())
        if parts:
            expected = list(range(parts[-1] + 1))
            missing = [p for p in expected if p not in by_topic[topic]]
            if missing:
                print(f"  {topic}: missing partitions {missing}  ← ORPHANED")
            else:
                print(f"  {topic}: partitions 0-{parts[-1]} fully covered")


if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else \
        "group_metadata_members_ds.json"
    main(path)
