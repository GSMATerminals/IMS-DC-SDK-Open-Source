#!/bin/bash

#
# Copyright 2025-China Telecom Research Institute.
# All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

config=${1-normal}

FLAVOR_NAME_CAPITALIZED="$(tr '[:lower:]' '[:upper:]' <<<"${config:0:1}")${config:1}"
task="assemble${FLAVOR_NAME_CAPITALIZED}Release"

echo "build config $config $task"

SCRIPT_DIR=$(pwd)/$(dirname $0)
RELEASE_DATE=$(date +%Y%m%d)
RELEASE_DIR=$SCRIPT_DIR/$config/$RELEASE_DATE
APK_DIR=$SCRIPT_DIR/../app/build/outputs/apk/${config}/release

mkdir -p $RELEASE_DIR
rm -r $RELEASE_DIR/*

# go to project root
cd $SCRIPT_DIR/../

./gradlew :app:$task

if [ -d "$APK_DIR" ]; then

  cp $APK_DIR/*.apk $RELEASE_DIR
  cp $SCRIPT_DIR/Android.mk $RELEASE_DIR
  cp $SCRIPT_DIR/com.ct.ertclib.dc.xml $RELEASE_DIR
  cp $SCRIPT_DIR/SDK_ReleaseNotes.txt $RELEASE_DIR
  cp $SCRIPT_DIR/newcalllib_aidl_20240228.rar $RELEASE_DIR

  cd $RELEASE_DIR
  FILE_NAME=$(ls | grep CtCallSDK_v*_release.apk)
  FILE_BASE=${FILE_NAME%%_release.apk*}

  cd $SCRIPT_DIR/$config
  tar cfz ${FILE_BASE}_${config}_FC.tgz $RELEASE_DATE

fi
