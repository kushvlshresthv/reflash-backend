#!/bin/bash

USER="username"
PASS="password"
BASE_URL="http://localhost:8080"

# Login endpoint
login() {
  echo "Calling login..."
  curl -X GET "$BASE_URL/login" -u $USER:$PASS
  echo
}


generate_flashcards() {
  echo "Fetching cards..."
  curl -X POST "$BASE_URL/api/ai/generate-flashcards" -u $USER:$PASS | jq
  echo
}


# # Get incomplete todos
# incomplete_todos() {
#   echo "Fetching incomplete todos..."
#   curl -X GET "$BASE_URL/api/incomplete-todos" -u $USER:$PASS | jq
#   echo
# }
#
# expired_todos() {
#   echo "Fetching expired todos..."
#   curl -X GET "$BASE_URL/api/expired-todos" -u $USER:$PASS | jq
#   echo
# }
#
#
# start_todo() {
#   echo "Starting todo..."
#   curl -X PUT "$BASE_URL/api/start-todo?id=1" -u $USER:$PASS | jq
#   echo
# }
#
# pause_todo() {
#   echo "Pausing todo..."
#   curl -X PUT "$BASE_URL/api/pause-todo?id=1" -u $USER:$PASS | jq
#   echo
# }
#
#
# weekly_leaderboards() {
#   echo "Loading weekly leaderboards..."
#   curl -X GET "$BASE_URL/api/weekly-leaderboards" -u $USER:$PASS | jq
#   echo
# }
#
# activities() {
#   echo "Loading activities..."
#   curl -X GET "$BASE_URL/api/activities" -u $USER:$PASS | jq
#   echo
#
# }
#
# community_summary() {
#   echo "Loading community summary..."
#   curl -X GET "$BASE_URL/api/community-summary" -u $USER:$PASS | jq
#   echo
# }
#
# recent_completions() {
#   echo "Loading recent completions..."
#   curl -X GET "$BASE_URL/api/recent-completions?pageNumber=0" -u $USER:$PASS | jq
#   echo
# }
#
# my_todos() {
#   echo "Loading recent completions..."
#   curl -X GET "$BASE_URL/api/my-todos" -u $USER:$PASS | jq
#   echo
# }
#


## Another endpoint example
#get_all_todos() {
#  echo "Fetching all todos..."
#  curl -X GET "$BASE_URL/todos" -u $USER:$PASS
#  echo
#}

# call functions based on CLI args
for cmd in "$@"; do
  $cmd
done
