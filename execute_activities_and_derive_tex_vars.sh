#!/usr/bin/env bash

#cd "static-analyzer"
#sh gradlew clean allDependencies
#cp build/libs/static-analyzer-0.0.1-SNAPSHOT-all-dependencies.jar ../static-analyzer-standalone.jar

#cd ../"Lakeside Mutual"
cd "Lakeside Mutual"
python3 activity-1-filecount.py write_tex_vars
bash activity-2-domain-analysis.sh
bash activity-3-6-service-analysis-complete.sh
bash discussion-line-counts.sh

cd ..
python3 csv2tex_vars.py "Lakeside Mutual/domain-analysis.csv"
python3 csv2tex_vars.py "Lakeside Mutual/service-analysis-complete.csv"
python3 csv2tex_vars.py "Lakeside Mutual/linecounts.csv"

cd "Lakeside Mutual"
bash run_activities_quietly_and_write_output_files.sh
