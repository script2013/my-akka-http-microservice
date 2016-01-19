
curl -H "Content-Type: application/json" -XPUT 'http://localhost:9000/inc' -d '{
"user":"user_1",
"page":"page_2"
}'

curl -H "Content-Type: application/json" -XPUT 'http://localhost:9000/inc' -d '{
"user":"user_1",
"page":"page_3"
}'

curl -H "Content-Type: application/json" -XPUT 'http://localhost:9000/inc' -d '{
"user":"user_2",
"page":"page_1"
}'

curl "http://localhost:9000/counters?from=0&to=10"

