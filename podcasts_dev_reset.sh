#!/usr/bin/env bash

rm -rf $HOME/Desktop/pipeline/

for t in   managed_file podcast_episode event_publication ; do
 echo " drop table if exists $t cascade " | PGPASSWORD=mogul psql -U mogul -h localhost mogul
done


