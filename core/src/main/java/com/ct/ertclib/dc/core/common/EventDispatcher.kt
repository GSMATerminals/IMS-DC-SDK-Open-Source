/*
 *   Copyright 2025-China Telecom Research Institute.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ct.ertclib.dc.core.common

import com.ct.ertclib.dc.core.data.event.Event
import java.util.concurrent.ConcurrentHashMap

object EventDispatcher {
    private val eventMap = ConcurrentHashMap<Class<*>, MutableList<EventObserver<Event<*>>>>()

    fun <T : Event<*>> registerEventObserver(eventClass: Class<T>, observer: EventObserver<T>) {
        if (eventMap[eventClass] == null) {
            eventMap[eventClass] = mutableListOf()
        }
        eventMap[eventClass]?.add(observer as EventObserver<Event<*>>)
    }

    fun <T : Event<*>> unregisterEventObserver(eventClass: Class<T>, observer: EventObserver<T>) {
        eventMap[eventClass]?.let {
            it.remove(observer as EventObserver<Event<*>>)
            if (it.size == 0) {
                eventMap.remove(eventClass)
            }
        }
    }

    fun dispatchEvent(event: Event<*>) {
        eventMap[event.javaClass]?.let {

            val iterator = it.iterator()
            while (iterator.hasNext()){
                val observer = iterator.next()
                observer.onEvent(event)
            }
        }
    }

    interface EventObserver<T> {
        fun onEvent(t: T)
    }
}