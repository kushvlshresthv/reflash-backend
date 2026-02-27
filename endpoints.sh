#!/bin/bash

USER="8_A_13"
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

  TEXT=$(cat << 'EOF'
The human brain is an extraordinarily complex organ responsible for thought and memory.
It operates through billions of neurons.
Each neuron communicates using electrical signals.
Neural connections form vast networks.
These networks enable learning.
The brain consumes large amounts of energy.
Despite its size, it uses about twenty percent of body energy.
Neuroplasticity allows adaptation.
Learning strengthens neural pathways.
Unused connections weaken.
Memory involves multiple regions.
Sleep supports memory consolidation.
Emotions are processed in the limbic system.
The amygdala regulates fear.
The prefrontal cortex handles decisions.
Damage affects cognition.
Brain development is rapid in childhood.
Environment shapes growth.
Language is often lateralized.
Left hemisphere dominates speech.
Right hemisphere supports spatial skills.
Imaging reveals brain activity.
MRI shows structure.
EEG measures signals.
Neuroscience studies consciousness.
Many disorders remain unexplained.
Research continues worldwide.
Better understanding improves medicine.
Education benefits from brain science.
Ethics arise from cognition research.
The brain remains a scientific mystery.
EOF
)

  jq -n \
    --arg text "$TEXT" \
    --argjson count 50 \
    '{ text: $text, count: $count }' |
  curl -X POST "$BASE_URL/api/ai/generate-flashcards" \
    -u "$USER:$PASS" \
    -H "Content-Type: application/json" \
    -d @- | jq

  echo
}


# generate_flashcards() {
#   echo "Fetching cards..."
#   curl -X POST "$BASE_URL/api/ai/generate-flashcards" -u $USER:$PASS | jq
#   echo
# }


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
