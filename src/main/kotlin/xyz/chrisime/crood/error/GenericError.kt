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

package xyz.chrisime.crood.error

/**
 * @author Christian Meyer &lt;christian.meyer@gmail.com&gt;
 */
sealed class GenericError(msg: String?, th: Throwable?) : RuntimeException(msg, th) {
    class Database(msg: String? = "${ErrorTag.DATABASE.msg}: %msg", cause: Throwable? = null) : GenericError(msg, cause)

    class General(msg: String = "${ErrorTag.GENERAL.msg}: %msg", cause: Throwable? = null) : GenericError(msg, cause)
}

enum class ErrorTag(val msg: String) {
    GENERAL("general error"),
    DATABASE("database error"),
}
