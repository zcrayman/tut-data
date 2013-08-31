#!/bin/sh
cd $(dirname $0)

cd ../2/complete

echo `pwd`
./gradlew clean build
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf build

cd ../../3/complete
echo `pwd`
./gradlew clean build
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf build

cd ../../4/complete
echo `pwd`
./gradlew clean build
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf build

cd ../../5/complete
echo `pwd`
./gradlew clean build
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf build

exit
