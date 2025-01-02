package com.toms223.winterboot

import java.io.File
import java.net.URL
import java.util.jar.JarFile
import kotlin.reflect.KClass

class ClassFinder {
    private val suffix = ".class"
    fun findAllClasses(classPathUrls: List<URL>, annotations: List<KClass<out Annotation>>): List<KClass<out Any>> =
        classPathUrls.mapNotNull { url ->
            when(url.protocol) {
                "file" -> {
                    val file = File(url.toURI())
                    if (file.isDirectory) {
                        return@mapNotNull findClassesInDirectory(file, annotations)
                    }
                    if (file.isFile && file.name.endsWith(".jar")) {
                        return@mapNotNull findClassesInJar(file, annotations)
                    } else {
                        return@mapNotNull null
                    }
                }
                else -> {
                    return@mapNotNull null
                }
            }
        }.flatten()
    private fun findClassesInDirectory(directory: File, annotations: List<KClass<out Annotation>>): List<KClass<out Any>> =
        directory.walkTopDown().toList().mapNotNull { file ->
            if (file.isFile && file.name.endsWith(suffix)) {
                val className = file.toRelativeString(directory).removeSuffix(suffix).replace(File.separator, ".")
                if(checkAnnotation(className,annotations)){
                    Class.forName(className).kotlin
                } else {
                    null
                }
            } else {
                null
            }
        }

    private fun findClassesInJar(jarFile: File, annotations: List<KClass<out Annotation>>): List<KClass<out Any>> {
        JarFile(jarFile).use { jar ->
            return jar.entries().toList().mapNotNull { entry ->
                if (!entry.isDirectory && entry.name.endsWith(suffix)) {
                    val className = entry.name.removeSuffix(suffix).replace("/", ".")
                    if(checkAnnotation(className,annotations)){
                        println("Could instantiate class: $className")
                        Class.forName(className).kotlin
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    private fun checkAnnotation(className: String, annotations: List<KClass<out Annotation>>): Boolean{
        try {
            return annotations.any { annotation -> Class.forName(className).kotlin.annotations.any { it.annotationClass == annotation } }
        } catch (e: Throwable) {
            return false
        }
    }
}