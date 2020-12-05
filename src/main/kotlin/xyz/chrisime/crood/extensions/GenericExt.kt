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

import xyz.chrisime.crood.error.GenericError
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 * @author Christian Meyer <christian.meyer@gmail.com>
 * Taken from http://stackoverflow.com/questions/2434041/instantiating-generics-type-in-java
 */
object GenericExt {

    @Throws(RuntimeException::class)
    fun <T> Any.new(index: Int): T {
        try {
            return getClassAtIndex<T>(index).getDeclaredConstructor().newInstance()
        } catch (ex: Exception) {
            when (ex) {
                is InstantiationException, is IllegalAccessException,
                is NoSuchMethodException, is InvocationTargetException -> {
                    throw GenericError.General("Can't instantiate obj of type '${this.javaClass.typeName}'.", ex)
                }

                else                                                   -> {
                    throw GenericError.General("Unknown error: ${ex.localizedMessage}", ex)
                }
            }
        }
    }

    fun <T> Any.getClassAtIndex(index: Int): Class<T> {
        val superClass = this.javaClass.genericSuperclass.asType<ParameterizedType>()

        return when (val type = superClass.actualTypeArguments[index]) {
            is ParameterizedType -> type.rawType.asType()
            else                 -> type.asType()
        }
    }

}
