#!/bin/bash
#
# Fintan wrapper script using a uber-jar instead of the default maven-exec.

# store the path of the fintan-backend/target directory
backend_dir="$(dirname -- "$(realpath -- "$0")")"
target_dir="${backend_dir}/target"
package_jar="${target_dir}/fintan-backend-0.0.1-SNAPSHOT.jar"

# Check for presence of packaged jar
if [ ! -e "${package_jar}" ]; then
    echo "Please make sure to run build.sh first, and verify the fintan jar is present in the target folder"
    exit 1
fi

java -Dfile.encoding=UTF8 -jar "${package_jar}" "$@"
