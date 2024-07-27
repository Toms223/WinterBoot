class ObjectInstantiator {
    fun instantiateObject(clazz: Class<*>, seeds: Map<String, Any>): Any {
        val constructor = clazz.declaredConstructors.firstOrNull { constructor ->
            constructor.parameters.all { parameter ->
                seeds.keys.contains(parameter.name.lowercase())
            }
        } ?:
        try {
            clazz.getDeclaredConstructor()
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException("No seeds found for constructor of class : ${clazz.simpleName}")
        }
        val args = constructor.parameters.map { seeds[it.name.lowercase()] ?: throw IllegalArgumentException("Seed ${it.name} not found") }.toTypedArray()
        return constructor.newInstance(*args)
    }
}