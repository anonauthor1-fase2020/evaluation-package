REM You need at least Java 11 in order to run the jar
cd ..\..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects\Beer Catalog\Reconstructed Models\services\BeerCatalog.services" -i "Other Evaluation Projects\Beer Catalog\Derived Intermediate Models\mapping models\BeerCatalog.xmi" --invoke_only_specified_phases intermediate_model_validation --service_metrics_set="Hirzalla" --service_metrics_set="Engel" --csv_file="Other Evaluation Projects\Beer Catalog\service-analysis.csv" 
