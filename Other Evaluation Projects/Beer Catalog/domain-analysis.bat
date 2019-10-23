REM You need at least Java 11 in order to run the jar
cd ..\..

CALL export_java_home.bat

java -jar static-analyzer-standalone.jar -s "Other Evaluation Projects\Beer Catalog\Reconstructed Models\domain\BeerCatalog.data" -i "Other Evaluation Projects\Beer Catalog\Derived Intermediate Models\data models\BeerCatalog.xmi" --invoke_only_specified_phases intermediate_model_validation --csv_file="Other Evaluation Projects\Beer Catalog\domain-analysis.csv" 
