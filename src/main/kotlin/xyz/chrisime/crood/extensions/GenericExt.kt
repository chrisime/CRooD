/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.chrisime.crood.extensions

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 * Taken from http://stackoverflow.com/questions/2434041/instantiating-generics-type-in-java
 *
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
@Throws(RuntimeException::class)
fun <T> Any.newInstance(index: Int = 0): T {
    try {
        return getClassAtIndex<T>(index).getDeclaredConstructor().newInstance()
    } catch (ex: Exception) {
        when (ex) {
            is InstantiationException, is IllegalAccessException,
            is NoSuchMethodException, is InvocationTargetException -> {
                throw RuntimeException("Can't instantiate '${this::class.java.typeName}'.", ex)
            }
            else -> throw RuntimeException("Unknown error: ${ex.localizedMessage}", ex)
        }
    }
}

@Throws(RuntimeException::class)
fun <T> Any.getClassAtIndex(index: Int): Class<T> {
    val superClassType: ParameterizedType = when (this::class.java.genericSuperclass) {
        is ParameterizedType -> this::class.java.genericSuperclass.asType()
        is Class<*> -> this::class.java.genericSuperclass.asType<Class<*>>().genericSuperclass.asType()
        else -> throw RuntimeException("unexpected type ${this::class.java.genericSuperclass}")
    }

    return when (val type = superClassType.actualTypeArguments[index]) {
        is ParameterizedType -> type.rawType.asType()
        is Class<*> -> type.asType()
        else -> throw RuntimeException("unexpected type $type")
    }
}

inline fun <reified T : Any> Any.asType(): T = if (T::class.java.isAssignableFrom(this::class.java)) {
    this as T
} else {
    throw TypeCastException("Value cannot be cast to ${T::class}")
}
