pluginManagement {
    repositories {
        // 阿里云镜像优先(dl.google.com / repo.maven.apache.org 直连在国内间歇性 TLS 断连)
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine")
            name = ""
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine")
            name = ""
        }
    }
}

rootProject.name = "ArmPal"

include(":app")
