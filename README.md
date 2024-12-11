# frida-idea-plugin
Right-click on the Java method and select 'Frida' or 'Frida Trace'.

* Frida script samples:

  ```typescript
  Java.use("java.util.ArrayList").$init.overload("int").implementation = function(arg1: any) {
      DMLog.i("ArrayList", `called $init with args: arg1=${arg1}`);
      this.$init(arg1);
  };
  ```

  ```typescript
  Java.use("java.util.ArrayList").add.overload("E", "java.lang.Object[]", "int").implementation = function(arg1: any, arg2: any, arg3: any) {
      DMLog.i("ArrayList", `called add with args: arg1=${arg1}, arg2=${arg2}, arg3=${arg3}`);
      this.add(arg1, arg2, arg3);
  };
  ```

  ```typescript
  Java.use("java.util.ArrayList").contains.implementation = function(arg1: any) {
      DMLog.i("ArrayList", `called contains with args: arg1=${arg1}`);
      const ret = this.contains(arg1);
      DMLog.i("ArrayList", "called contains return: " + ret);
      return ret;
  };
  ```


* Frida trace command samples:

  ```shell
  frida-trace -U -j 'java.util.ArrayList!$init' -F
  ```

  ```shell
  frida-trace -U -j 'java.util.ArrayList!grow' -F
  ```

  
