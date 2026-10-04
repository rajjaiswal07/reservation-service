#!/bin/bash

set -e

BASE_URL="${BASE_URL:-https://reservation-service-xdd7.onrender.com}"
EVENT_ID="${EVENT_ID:-1}"
SEAT_ID="${SEAT_ID:-1}"
REQUESTS="${REQUESTS:-20}"

echo "======================================"
echo "Hot Seat Concurrency Test"
echo "======================================"
echo "Base URL : $BASE_URL"
echo "Event ID : $EVENT_ID"
echo "Seat ID  : $SEAT_ID"
echo "Requests : $REQUESTS"
echo "======================================"

rm -rf /tmp/reservation-test
mkdir -p /tmp/reservation-test

for i in $(seq 1 "$REQUESTS")
do
    (
        USER_ID="burst-user-$i"
        IDEMPOTENCY_KEY="burst-$i"

        curl -s \
            -o "/tmp/reservation-test/response-$i.txt" \
            -w "%{http_code}" \
            -X POST \
            "$BASE_URL/api/reservations" \
            -H "Content-Type: application/json" \
            -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
            -H "X-Correlation-Id: burst-$i" \
            -d "{
                \"eventId\": $EVENT_ID,
                \"userId\": \"$USER_ID\",
                \"seatIds\": [$SEAT_ID]
            }" \
            > "/tmp/reservation-test/status-$i.txt"
    ) &
done

wait

echo
echo "Results:"
echo "--------------------------------------"

SUCCESS=$(grep -l "^201$" /tmp/reservation-test/status-* 2>/dev/null | wc -l | tr -d ' ')
CONFLICT=$(grep -l "^409$" /tmp/reservation-test/status-* 2>/dev/null | wc -l | tr -d ' ')
OTHER=$((REQUESTS - SUCCESS - CONFLICT))

echo "201 Created : $SUCCESS"
echo "409 Conflict: $CONFLICT"
echo "Other       : $OTHER"

echo
echo "Responses:"
echo "--------------------------------------"

for file in /tmp/reservation-test/response-*.txt
do
    echo "[$file]"
    cat "$file"
    echo
done

echo "======================================"

if [ "$SUCCESS" -eq 1 ]; then
    echo "PASS: Exactly one request reserved the hot seat."
else
    echo "FAIL: Expected exactly one successful reservation."
    exit 1
fi