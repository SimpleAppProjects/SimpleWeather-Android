package com.thewizrd.shared_resources.json

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

inline fun <reified E> Moshi.listAdapter(elementType: Type = E::class.java): JsonAdapter<List<E>> {
    return adapter(listType<E>(elementType))
}

inline fun <reified K, reified V> Moshi.mapAdapter(
    keyType: Type = K::class.java,
    valueType: Type = V::class.java
): JsonAdapter<Map<K, V>> {
    return adapter(mapType<K, V>(keyType, valueType))
}

inline fun <reified E> listType(elementType: Type = E::class.java): Type {
    return Types.newParameterizedType(List::class.java, elementType)
}

inline fun <reified E> mutableListType(elementType: Type = E::class.java): Type {
    return Types.newParameterizedType(MutableList::class.java, elementType)
}

inline fun <reified K, reified V> mapType(
    keyType: Type = K::class.java,
    valueType: Type = V::class.java
): Type {
    return Types.newParameterizedType(Map::class.java, keyType, valueType)
}