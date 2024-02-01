#!/bin/bash

ROLES=(ADMIN PM TRANSLATOR USER)

HOST="127.0.0.1:8080"
TOKEN=""
SESSION=""

die() {
    echo "$@"
    exit 1
}

while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--session)
            SESSION="$2"
            shift
            shift
            ;;
        -t|--token)
            TOKEN="$2"
            shift
            shift
            ;;
        -H|--host)
            HOST="$2"
            shift
            shift
            ;;
    esac
done

[ -z "$SESSION" ] && die "The session key was not provided (JSESSIONID cookie)"
[ -z "$TOKEN" ] && die "The X-CSRF-TOKEN was not provided (CSRF_TOKEN in the browser debug console)"

c=0
for i in a b c d e f g h i j k l m n o p q r s t u v w x y z; do
    echo "Creating user $c: $i"
    ROLE=${ROLES[$((c % ${#ROLES[@]}))]}
    cat << EOF > tmp.json
{
    "username":"$i",
    "givenName":"",
    "surname":"",
    "commonName":"$i$i$i",
    "password":"$i",
    "authorities":[
        {"authority":"$ROLE"}
    ]
}
EOF
    (( c++ ))
    curl "http://$HOST/api/users" -X POST -H "X-CSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' -H "Cookie: JSESSIONID=$SESSION" --data '@tmp.json' | jq
    rm tmp.json
done
