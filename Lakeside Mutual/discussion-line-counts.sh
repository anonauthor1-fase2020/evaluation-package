#!/usr/bin/env bash
# You need cloc 1.84 (cf. https://github.com/AlDanial/cloc) installed in order 
# to run this script
(
    echo 'lineCountLM;lineCountModels;lineCountReusableTechnology;';
    (
        cloc "Evaluated LM Source Code" --csv | tail -n1 | awk -F "\"*,\"*" '{print $5}';
        cloc "Reconstructed Models" --force-lang-def="cloc_modeling_languages_def" --csv | tail -n1 | awk -F "\"*,\"*" '{print $5}';
        cloc "Reconstructed Models" --force-lang-def="cloc_modeling_languages_def" --match-f=".*\.technology" --exclude-list-file="cloc_exclude_application_specific_technology_models" --csv | tail -n1 | awk -F "\"*,\"*" '{print $5}';
    ) | tr '\n' ';'
) > linecounts.csv
