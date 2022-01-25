#!/bin/bash
#
# Fintan wrapper script using a uber-jar instead of the default maven-exec.

# store the path of the fintan-backend/target directory
backend_dir="$(dirname -- "$(realpath -- "$0")")"
target_dir="${backend_dir}/target"
package_jar="${target_dir}/fintan-backend-0.0.1-SNAPSHOT.jar"

# Check for presence of packaged jar
if [ ! -e "${package_jar}" ]; then
    echo "Please make sure to run package.sh first, and verify the jar-with-dependencies is present in the target folder"
    exit 1
fi

/usr/lib/jvm/java-8-openjdk-amd64/bin/java -Dfile.encoding=UTF8 -jar "${package_jar}" "$@"
