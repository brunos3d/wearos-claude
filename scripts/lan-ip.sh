#!/usr/bin/env bash
#
# Prints the most likely LAN IP for this host. Useful when pasting the
# backend URL into the watch.
ip -4 -o addr show scope global \
    | awk '{print $4}' \
    | cut -d/ -f1 \
    | grep -v '^127\.' \
    | head -n1
