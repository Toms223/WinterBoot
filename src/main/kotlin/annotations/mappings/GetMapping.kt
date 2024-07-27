package annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class GetMapping(val path: String)
