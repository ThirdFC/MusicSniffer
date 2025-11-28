plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// 1. 定义 'shade' 配置组
val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// 2. 让编译时也能看到 shade 里的库
configurations.implementation.get().extendsFrom(shade)

dependencies {
    // 3. 将 JNA 库加入 shade 分组
    add("shade", "net.java.dev.jna:jna:5.13.0")
    add("shade", "net.java.dev.jna:jna-platform:5.13.0")
}

// 4. 配置打包任务
tasks.jar {
    from(shade.map { file ->
        if (file.isDirectory) file else zipTree(file)
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
}