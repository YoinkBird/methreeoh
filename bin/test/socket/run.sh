#!/bin/bash -xu
scriptdir="$(dirname "$0")"
source "$scriptdir/config.sh"
echo $*
echo $1
echo $2
host=$1
port=$2

#cmd_server=$(env THIS_OPT_PRINTONLY=0 $java_base_cmd $THIS_SERVER $port)
cmd_server="$java_base_cmd $javaOpts $THIS_SERVER $port"
$cmd_server &
rc=$?
java_cmd_pid=$!
# exit if bad compile
if [ $rc -ne 0 ] ;then
  kill $java_cmd_pid
  exit $rc
fi
sleep 1
lsof -i :${port}

#cmd_client=$(env THIS_OPT_PRINTONLY=0 $java_base_cmd $THIS_CLIENT $host $port)
cmd_client="$java_base_cmd $THIS_CLIENT $host $port"

usepipe=0
if [ ! -z ${THIS_TEST_USEPIPE:-} ];then
  usepipe=$THIS_TEST_USEPIPE
fi
if [[ $usepipe -eq 1 ]];then
  # named pipe
  # http://serverfault.com/a/297095 
  inputPipe="/tmp/javaEchoClient_input"
  mkfifo /tmp/javaEchoClient_input
  cat > /tmp/javaEchoClient_input &
  cat_cmd_pid=$!
  echo $cat_cmd_pid > /tmp/javaEchoClient_input-cat-pid
  cat /tmp/javaEchoClient_input | $cmd_client &

  for i in {0..5}; do
    testVal=test${i};
    echo $testVal > $inputPipe
  done
  echo "TERMINATE" > $inputPipe
  # resolves buffer issues in which java output appears after everything is done
else
  set +x
  for i in {0..5}; do
    testVal=test${i};
    jobs
    echo $testVal | $cmd_client
  done
  echo "TERMINATE" | $cmd_client

fi
sleep 2
jobs
kill -9 $java_cmd_pid
if [[ $usepipe -eq 1 ]];then
  kill -9 $cat_cmd_pid
fi
sleep 2
jobs
echo "done"
