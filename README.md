# MethodTraceRunner
A command line tool to run Android Method Trace   
Powered by [ddmlib](https://android.googlesource.com/platform/tools/base/+/master/ddmlib/)

## Usage
```
mtr
 -h         show help msg
 -p <arg>   package name
 -o <arg>   output file
 -t <arg>   trace time
 -v         verbose log
 ```

```
$ java -jar mtr.jar -p com.your.package -o output.trace -t 10
this cmd will trace app com.your.package for 10 seconds and save result at output.trace.
```

## Download
https://github.com/likaci/MethodTraceRunner/releases

## Refs
https://android.googlesource.com/platform/tools/base/+/master/ddmlib/
https://developer.android.com/studio/profile/am-methodtrace.html
http://blog.csdn.net/eclipsexys/article/details/51316423