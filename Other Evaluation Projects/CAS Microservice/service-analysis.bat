REM You need at least Java 11 in order to run the jar
cd ..\..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects\CAS Microservice\Reconstructed Models\services\CAS.services" -i "Other Evaluation Projects\CAS Microservice\Derived Intermediate Models\service models\CAS.xmi" --invoke_only_specified_phases intermediate_model_validation --service_metrics_set="Hirzalla" --service_metrics_set="Engel" --csv_file="Other Evaluation Projects\CAS Microservice\service-analysis.csv" 
