#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
APP_HOME=`dirname "$PRG"`

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$APP_HOME" ] &&
        APP_HOME=`cygpath --unix "$APP_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$CLASSPATH" ] &&
        CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# Set-up command for nonstop
if $nonstop ; then
    if [ -z "$CLASSPATH" ] ; then
        CLASSPATH=.:
    else
        CLASSPATH=.:$CLASSPATH
    fi
fi

# Read an optional running configuration file
if [ "x$APP_RUN_CONF" = "x" ]; then
    APP_RUN_CONF="$APP_HOME/gradle.conf"
fi
if [ -r "$APP_RUN_CONF" ]; then
    . "$APP_RUN_CONF"
fi

# Add the jar to the classpath
if [ -n "$APP_HOME" ] ; then
  if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ] ; then
    CLASSPATH="$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
  fi
fi

# Set GRADLE_HOME if not already set.
if [ -z "$GRADLE_HOME" ]; then
    GRADLE_HOME="$APP_HOME"
fi

# Add the 'lib' dir to the classpath
if [ -n "$GRADLE_HOME" ] ; then
  if [ -d "$GRADLE_HOME/lib" ] ; then
    for i in "$GRADLE_HOME"/lib/*.jar; do
        # Add to CLASSPATH
        CLASSPATH="$CLASSPATH:$i"
    done
  fi
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    [ -n "$APP_HOME" ] &&
        APP_HOME=`cygpath --path --windows "$APP_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    [ -n "$CLASSPATH" ] &&
        CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
    [ -n "$GRADLE_HOME" ] &&
        GRADLE_HOME=`cygpath --path --windows "$GRADLE_HOME"`
fi

# Set PWD for location of gradle script.
if [ -z "$GRADLE_SCRIPT_PATH" ] ; then
  if $cygwin ; then
    GRADLE_SCRIPT_PATH=`cygpath -w "$PRG"`
  else
    GRADLE_SCRIPT_PATH="$PRG"
  fi
fi

# Find java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum number of open files
if ! $cygwin && ! $msys ; then
    if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
        # Use the maximum available.
        MAX_FD_LIMIT=`ulimit -H -n`
        if [ $? -eq 0 ] ; then
            if [ "$MAX_FD_LIMIT" != 'unlimited' ] ; then
                ulimit -n $MAX_FD_LIMIT
            fi
        else
            warn "Could not query maximum file descriptor limit"
        fi
    else
        ulimit -n $MAX_FD
    fi
fi

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- "$DEFAULT_JVM_OPTS" $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$@"

# Use "exec" to force the Gradle platform process to inherit the same PID as the script
exec "$JAVACMD" "$@"
