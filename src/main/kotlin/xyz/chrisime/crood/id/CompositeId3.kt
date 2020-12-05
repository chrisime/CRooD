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

/**
 * @author Christian Meyer <christian.meyer@gmail.com>
 */
data class CompositeId3<ID1 : Any, ID2 : Any, ID3 : Any>(private val id1: ID1,
                                                         private val id2: ID2,
                                                         private val id3: ID3) : CompositeId {

    private val id: Array<out Any> = arrayOf(id1, id2, id3)

    override fun getId(): Array<out Any> = id

}
