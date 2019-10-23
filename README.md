## Evaluation Package for our FASE2020 Paper on Architecture Reconstruction of Microservice-based Software Systems
### Quick Steps for Reproducing the Evaluation Results
---
Follow these steps to reproduce the results presented in the Evaluation section for the ``Lakeside Mutual`` application:
1. Clone this repository to your harddrive.
2. Switch to the ``Lakeside Mutual`` folder.
2.  Depending on your operating system, execute the following scripts to reproduce the results of the process activities:  
3.1 **Activity 1**: Run the ``activity-1-filecount.py`` script in a terminal, leveraging an interpreter for Python 3.6.8. On Linux Mint 19.2, this can be achieved with the command ``python3 activity-1-filecount.py``. The results for the file counts mentioned for Activity 1 in the Evaluation section are shown in the terminal.  
3.2 **Activity 2**: Run the ``activity-2-domain-analysis.bat`` (Windows) or ``activity-2-domain-analysis.sh`` script in a terminal, e.g., via ``bash activity-2-domain-analysis.sh``. You will need Java 11 or higher to execute these scripts as they invoke the ``static-analyzer-standalone.jar`` (see below). Alter the ``export_java_home.bat`` (Windows) or ``export_java_home.sh`` in the top-level folder of the repository to specify an alternative path to the ``java`` executable.The scripts print the results of Activity 2. Interim results are printed for each domain model and each script ends with a result summary.  
3.3 **Activity 3**: Run the ``activity-3-6-service-analysis-no-metrics.bat`` (Windows) or ``activity-3-6-service-analysis-no-metrics.sh`` script in a terminal, e.g., via ``bash activity-3-6-service-analysis-no-metrics.sh``. You will need Java 11 or higher to execute these scripts as they invoke the ``static-analyzer-standalone.jar`` (see below). Alter the ``export_java_home.bat`` (Windows) or ``export_java_home.sh`` in the top-level folder of the repository to specify an alternative path to the ``java`` executable. The scripts print the results of Activity 3 excluding the metrics mentioned at the end of the Evaluation section. Interim results are printed for each service model and each script ends with a result summary.  
3.4 **Activity 6**: Run the ``activity-3-6-service-analysis-x-metrics.bat`` (Windows) or ``activity-3-6-service-analysis-x-metrics.sh`` script in a terminal, e.g., via ``bash activity-3-6-service-analysis-x-metrics.sh``. Replace ``x`` with ``engel`` [1] or ``hirzalla`` [2] depending on which metrics set you are interested in (cf. the end of the Evaluation section in the paper). You will need Java 11 or higher to execute these scripts as they invoke the ``static-analyzer-standalone.jar`` (see below). Alter the ``export_java_home.bat`` (Windows) or ``export_java_home.sh`` in the top-level folder of the repository to specify an alternative path to the ``java`` executable. The scripts print the results of Activity 6 for the given metrics set. Interim results are printed for each service model and each script ends with a result summary.  

The scripts and the static analyzer were tested on Linux Mint 19.2 (Kernel 5.0.0.31) and Windows 7 with the following versions of runtime dependencies:
- ``bash`` (Linux): 4.4.20
- ``Java``: 11
- ``Kotlin``: 1.3.50
- ``Python``: 3.6.8

**If for technical reasons the scripts do not work, please refer to the ``Produced Result Files`` sub-folder in the ``Lakeside Mutual`` folder. It contains raw outputs of executing each of the mentioned scripts on Linux Mint 19.2. The raw outputs are stored in the files with the ``out`` extension.**

#### Reviewing the Reconstructed Models of ``Lakeside Mutual``

If you are interested in reviewing the reconstructed models of ``Lakeside Mutual`` expressed in our modeling languages, follow these steps:
1. Download an Eclipse version pre-bundled with the plugins of our modeling languages from the anonymized [GitHub repository](https://github.com/anonauthor1-fase2020/languages) of our languages.
2. Start the Eclipse instance and import the files contained in the ``Reconstructed Models`` sub-folder of the ``Lakeside Mutual`` folder in this repository into a new project.

For further guidance upon importing models expressed in our languages, please refer to the ``README.md`` in the anonymized [GitHub repository](https://github.com/anonauthor1-fase2020/languages) of our languages.

### Detailed Description of Package Contents
---

The package comprises the following folders and files:
- ``Lakeside Mutual`` (folder): Comprises elements related to the main evaluation application.
- ``Other Evaluation Projects`` (folder): Comprises elements related to the six other evaluation projects mentioned in the Evaluation section.
- ``static-analyzer`` (folder): Comprises the source code of our static analyzer mentioned in the Evaluation section.
- ``csv2tex_vars.py``: Python-script for deriving LaTeX variable definitions used in the paper from CSV files (see below).
- ``execute_activities_and_derive_tex_vars.[bat|sh]``: Scripts that execute the scripts for Activities 1, 2, 3, and 6 of the reconstruction process mentioned in the Quick Steps for Reproducing the Evaluation Results. The script produces CSV files, from which it also derives the LaTeX variables used in the paper.
- ``export_java_home.[bat|sh]``: Scripts invoked by other scripts to determine the version of the ``java`` executable to use. They may be altered to point to a version of the ``java`` executable that diverges from the default one.
- ``static-analyzer-standalone.jar``: Pre-compiled, standalone version of the static analyzer (see below). Please note that this JAR archive is 89 MB in size, because it bundles all mandatory dependencies to run standalone.

Please note that all programs and scripts were implemented upon the following versions of programming languages and runtime environments:
- Operating system: Linux Mint 19.2 with Kernel 5.0.0.31
- ``bash``: 4.4.20
- [``cloc``](https://github.com/AlDanial/cloc): 1.84
- ``Gradle``: 5.2.1
- ``Java``: 11
- ``Kotlin``: 1.3.50
- ``Python``: 3.6.8

#### Contents of Folder ``Lakeside Mutual``
---
- ``Derived Intermediate Models`` (folder): The reconstructed models in their serialized, [intermediate form](https://github.com/anonauthor1-fase2020/languages/documentation) (XMI) for static analysis.
- ``Evaluated LM Source Code`` (folder): The source code of ``Lakeside Mutual``, which we examined for evaluating our reconstruction process (cf. the application's [GitHub repository](https://github.com/Microservice-API-Patterns/LakesideMutual) for the current version).
- ``Produced Result Files`` (folder): Files containing redirected raw outputs of scripts (cf. the Quick Steps for Reproducing the Evaluation Results).
- ``Reconstructed Models`` (folder): The reconstructed models of ``Lakeside Mutual`` expressed in our modeling languages. They can be imported into the Eclipse versions bundled with our modeling languages (cf. the anonymized [GitHub repository](https://github.com/anonauthor1-fase2020/languages) of our languages).
- Files starting with the ``activity-x`` prefix: See the Quick Steps for Reproducing the Evaluation Results.
- ``discussion-line-counts.sh``: This bash script produces a CSV file (``linecounts.csv``) that contains the line counts mentioned in the Discussion section of the paper. Therefore, it uses the ``cloc`` tool together with the configuration files ``cloc_modeling_languages_def`` and ``cloc_exclude_application_specific_technology_models`` contained in the folder.
- ``run_activities_quietly_and_write_output_files.[bat|.sh]``: These scripts run all scripts for Activities 2, 3, and 6, and redirect their outputs to the ``Produced Result Files`` folder.

#### Contents of Folder ``Other Evaluation Projects``
---
This folder contains a folder for each of the six Open Source MSA projects mentioned in the Evaluation section of the paper, besides ``Lakeside Mutual``. Their contents are organized similarly to the contents of the ``Lakeside Mutual`` folder (see above). There is a ``Derived Intermediate Models`` and ``Reconstructed Models`` folder, as well as a script for domain and service analysis (including all metrics). A link to the source code of the projects may be found in [this list on GitHub](https://github.com/davidetaibi/Microservices_Project_List).

#### Contents of Folder ``static-analyzer``
---
This folder contains the source code for the static analyzer mentioned throughout the paper. It was developed in Kotlin 1.3.50 with a Java 11 target and uses Gradle 5.2.1 as build tool. Please note that the metamodels of our modeling languages and the intermediate model formats are contained as local JAR dependencies in the ``libs`` folder. In addition, the folder comprises a compiled version of a framework for model processing (``model-processing.jar``).

For the sake of convenience, all JARs represent fat JARs, i.e., they come pre-packaged with all needed dependencies. Unfortunately, this increases the JAR archives' file sizes and the ``libs`` folder is roughly 85 MB in size.

### References
---
[1] Engel, T., Langermeier, M., Bauer, B., Hofmann, A.: Evaluation of microservice architectures: A metric and tool-based approach. In: Mendling, J., Mouratidis, H. (eds.) Information Systems in the Big Data Era. pp. 74–89. Springer (2018)  
[2] Hirzalla, M., Cleland-Huang, J., Arsanjani, A.: A metrics suite for evaluating flexibility and complexity in service oriented architectures. In: Feuerlicht, G., Lamersdorf, W. (eds.) Service-Oriented Computing – ICSOC 2008 Workshops. pp. 41–52. Springer (2009)
