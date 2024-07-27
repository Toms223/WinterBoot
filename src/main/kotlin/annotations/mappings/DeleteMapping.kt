package annotations.mappings

@Target(AnnotationTarget.FUNCTION)
annotation class DeleteMapping(val path: String)
