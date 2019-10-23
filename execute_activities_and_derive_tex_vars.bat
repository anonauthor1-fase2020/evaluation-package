cd "Lakeside Mutual"
python activity-1-filecount.py write_tex_vars
CALL activity-2-domain-analysis.bat
CALL activity-3-6-service-analysis-complete.bat

cd ..
python csv2tex_vars.py "Lakeside Mutual\domain-analysis.csv"
python csv2tex_vars.py "Lakeside Mutual\service-analysis-complete.csv"
REM python csv2tex_vars.py "Lakeside Mutual\linecounts.csv"

cd "Lakeside Mutual"
CALL run_activities_quietly_and_write_output_files.bat
