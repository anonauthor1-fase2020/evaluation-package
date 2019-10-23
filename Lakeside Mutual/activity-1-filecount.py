#!/usr/bin/env python3
# This script was written and tested with Python 3.6.8

import os
import sys

from functools import reduce

def count_files_per_subdir_in(topLevelDir, filterExtensions=[]):
    results = {}

    subDirs = next(os.walk(topLevelDir))[1]
    for sd in sorted(subDirs):
        subDirPath = os.path.join(topLevelDir, sd)
        sdFilesCount = 0
        for root, dirs, files in os.walk(subDirPath):
            if not filterExtensions:
                sdFilesCount += len(files)
            else:
                sdFilesCount += count_files_with_extensions(files, 
                    filterExtensions)
                
        results[sd] = sdFilesCount
        printLabel = ('Number of files in directory "%s" and its ' + \
            'sub-directories:') % subDirPath
        print('%s %i' % (printLabel.ljust(110), sdFilesCount))

    return results    

def count_files_with_extensions(files, extensions):
    filesWithExts = filter(lambda f: get_file_extension(f) in extensions, files)
    return len(list(filesWithExts))

def get_file_extension(file):
    filePath, extension = os.path.splitext(file)
    if extension:
        extension = extension[1:]
    return extension

def map_results_to_tex_vars(results):
    subdirToVarname = {
        '__modelFileCounts': 'ReconstructedModelsCount',
        'customer-core': 'CustomerCoreFileCount',
        'customer-management-backend': 'CustomerManagementBackendFileCount',
        'customer-self-service-backend': 'CustomerSelfServiceBackendFileCount',
        'policy-management-backend': 'PolicyManagementBackendFileCount'
    }

    texVars = {}
    for (subdir, value) in results.items():
        varName = subdirToVarname[subdir]
        texVars[varName] = value
    return texVars

def write_tex_vars(targetFile, vars):
    with open(targetFile, 'w') as file:
        for (name, value) in vars.items():
            file.write('\def \zeval%s{%s}\n' % (name, str(value)))

if __name__ == '__main__':
    results = count_files_per_subdir_in('Evaluated LM Source Code')

    modelFileExtensions = ["data", "mapping", "services", "operation", 
        "technology"]
    modelFileResults = count_files_per_subdir_in('Reconstructed Models', 
        modelFileExtensions)    
    results['__modelFileCounts'] = reduce(lambda v1, v2: v1 + v2, 
        modelFileResults.values())

    if 'write_tex_vars' in sys.argv:
        texVars = map_results_to_tex_vars(results)
        write_tex_vars(os.path.join('..', 'tex_vars.tex'), texVars)
