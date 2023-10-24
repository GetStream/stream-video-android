#!/bin/bash

# Parse named parameters
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --repackaged_webrtc) repackaged_webrtc="$2"; shift ;;
        --webrtc_android) webrtc_android="$2"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# Run the Python command with the parameters
python3 scripts/repackage/repackage.py --repackaged_webrtc "$repackaged_webrtc" --webrtc_android "$webrtc_android"
