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

package xyz.chrisime.crood.id

import org.jooq.Condition
import org.jooq.TableField
import org.jooq.impl.DSL

// TODO: composite primary key
@JvmInline
value class PrimaryKey(private val id: Any) {
    fun equal(vararg ids: TableField<*, *>): Condition {
        // TODO: add check in CRooDService ~> ID.size == ids.size
        require(ids.size == 1) {
            "Expected only 1 primary key, actual is ${ids.size}."
        }

        require(ids[0].dataType.type.isAssignableFrom(id::class.java)) {
            "${id::class.java.simpleName} is not compatible with type of primary key ${ids[0].dataType.type.simpleName}"
        }

        return DSL.row(*ids).equal(id)
    }
}
