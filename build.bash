#!/bin/bash
# Used to rebuild all the templated docs

#this finds all directories containing README.ftl.md and creates a bash array
doc_locations=($(
find . -type f -name 'README.ftl.md' |sed 's#\(.*\)/.*#\1#' |sort -u
));

echo "Converting ..."

for loc in "${doc_locations[@]}";
do
  echo " $loc/README.ftl.md -> $loc/README.md"
  cat $loc/README.ftl.md | fpp > $loc/README.md
done
