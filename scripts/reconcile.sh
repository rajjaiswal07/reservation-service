#!/bin/bash

set -e

BASE_URL="${BASE_URL:-https://reservation-service-xdd7.onrender.com}"
EVENT_ID="${EVENT_ID:-1}"

echo "======================================"
echo "Reservation Reconciliation"
echo "======================================"

echo
echo "Final seat state:"
echo "--------------------------------------"

curl -s \
    "$BASE_URL/api/events/$EVENT_ID/seats"

echo
echo
echo "Reservation metrics:"
echo "--------------------------------------"

curl -s \
    "$BASE_URL/actuator/prometheus" \
    | grep "^reservation_"

echo
echo "======================================"