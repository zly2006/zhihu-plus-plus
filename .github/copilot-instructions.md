编写完成代码后，必须使用 ./gradlew assembleLiteDebug 来验证一下有没有报错。

没有报错后。使用 ./gradlew ktlintFormat 格式化代码。


如果我要求你检查重复代码，首先看看你修改了哪些代码片段，然后使用grep工具，检查这些片段是否在其他地方也出现了，如果有的话，依次检查，然后考虑提取出函数、data class、接口等，来复用代码。
