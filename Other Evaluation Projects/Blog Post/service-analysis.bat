REM You need at least Java 11 in order to run the jar
cd ..\..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects\Blog Post\Reconstructed Models\services\BlogPost.services" -i "Other Evaluation Projects\Blog Post\Derived Intermediate Models\service models\BlogPost.xmi" --invoke_only_specified_phases intermediate_model_validation --service_metrics_set="Hirzalla" --service_metrics_set="Engel" --csv_file="Other Evaluation Projects\Blog Post\service-analysis.csv" 
