package annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class PutMapping(val path: String)
