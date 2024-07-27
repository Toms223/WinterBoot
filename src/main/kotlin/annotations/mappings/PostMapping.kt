package annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class PostMapping(val path: String)
