#!/bin/bash
set -e

# Usage:
#  --user-home - an alternative user to install into, default /home/hadoop
#  --tmpdir - an alternative temporary directory, default TMPDIR or /tmp if not set
#  --no-bash - do not update .bashrc

LATEST=http://@location@/lingual/@majorVersion@/lingual-client/latest.txt

case "`uname`" in
  Darwin)
    USER_HOME=/Users/${USER};;
  *)
    USER_HOME=/home/${USER};;
esac

# root of install, by default becomes $USER_HOME/.lingual-client
INSTALL_PATH=$USER_HOME

BASH_PROFILE=.bashrc
UPDATE_BASH=true

# don't install twice
[ -z `which lingual` ] && UPDATE_BASH=false

[ -z $TMPDIR ] && TMPDIR=/tmp

error_msg () # msg
{
  echo 1>&2 "Error: $1"
}

error_exit () # <msg> <cod>
{
  error_msg "$1"
  exit ${2:-1}
}

while [ $# -gt 0 ]
do
  case "$1" in
    --user-home)
      shift
      USER_HOME=$1
      ;;
    --tmpdir)
      shift
      TMPDIR=$1
      ;;
    --no-bash)
      UPDATE_BASH=false
      ;;
    -*)
      # do not exit out, just note failure
      error_msg "unrecognized option: $1"
      ;;
    *)
      break;
      ;;
  esac
  shift
done

[ -z ${LINGUAL_HOME} ] && LINGUAL_HOME=${INSTALL_PATH}/.lingual-client

REDIR=$TMPDIR/latest
ARCHIVE=$TMPDIR/archive.tgz
UNARCHIVED=$TMPDIR/unarchived

# find latest lingual client
wget -S -T 10 -t 5 ${LATEST} -O ${REDIR}

[ -f ${LINGUAL_HOME}/latest ] && LINGUAL_CURRENT=`cat ${LINGUAL_HOME}/latest`
# force update if on dev releases
if [ `cat $REDIR` = ${LINGUAL_CURRENT/wip-dev/} ]; then
  echo "no update available"
  exit 0
fi

# download latest
wget -S -T 10 -t 5 -i ${REDIR} -O ${ARCHIVE}

# unpack into /usr/local/<lingual home>
mkdir -p ${UNARCHIVED}
tar -xzf ${ARCHIVE} -C ${UNARCHIVED}

# move existing out of the way
if [ -d "${LINGUAL_HOME}" ]; then
  mv ${LINGUAL_HOME} $TMPDIR
fi

#if LINGUAL_HOME does not exist, create it
if [ ! -d "${LINGUAL_HOME}" ]; then
    mkdir -p ${LINGUAL_HOME}
    chmod a+r ${LINGUAL_HOME}
fi

cp -r ${UNARCHIVED}/lingual-client/* ${LINGUAL_HOME}/

chmod a+x ${LINGUAL_HOME}/bin/*

cp ${REDIR} ${LINGUAL_HOME}/latest

echo "Successfully installed Lingual into \"${LINGUAL_HOME}\"."
echo "See \"${LINGUAL_HOME}/docs\" for documentation."

if [ "${UPDATE_BASH}" = "true" -a -w "${USER_HOME}/${BASH_PROFILE}" ]; then
cat >> ${USER_HOME}/${BASH_PROFILE} <<- EOF

# Lingual - Concurrent, Inc.
# http://www.concurrentinc.com/

export LINGUAL_HOME=${LINGUAL_HOME}

# add lingual tool to PATH
export PATH=\$PATH:\$LINGUAL_HOME/bin

EOF

  echo "Successfully updated ${USER_HOME}/${BASH_PROFILE} with new PATH information."
elif [ -z `which lingual` ]; then

  echo "To complete installation, add \"${LINGUAL_HOME}/bin\" to the PATH."
fi
