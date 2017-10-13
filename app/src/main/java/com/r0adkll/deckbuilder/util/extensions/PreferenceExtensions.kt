package com.r0adkll.deckbuilder.util.extensions


import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.gson.Gson
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


interface RxPreferences {

    val rxSharedPreferences: RxSharedPreferences


    abstract class ReactivePreference<T : Any?>(val key: String) : ReadOnlyProperty<RxPreferences, Preference<T>>


    class ReactiveIntPreference(key: String, val default: Int = 0) : ReactivePreference<Int>(key) {

        override fun getValue(thisRef: RxPreferences, property: KProperty<*>): Preference<Int> {
            return thisRef.rxSharedPreferences.getInteger(key, default)
        }
    }


    class ReactiveBooleanPreference(key: String, val default: Boolean = false) : ReactivePreference<Boolean>(key) {

        override fun getValue(thisRef: RxPreferences, property: KProperty<*>): Preference<Boolean> {
            return thisRef.rxSharedPreferences.getBoolean(key, default)
        }
    }


    class ReactiveStringPreference(key: String, val default: String? = null) : ReactivePreference<String>(key) {

        override fun getValue(thisRef: RxPreferences, property: KProperty<*>): Preference<String> {
            return default?.let {
                return thisRef.rxSharedPreferences.getString(key, it)
            } ?: thisRef.rxSharedPreferences.getString(key)
        }
    }


    class ReactiveStringSetPreference(key: String, val default: Set<String> = HashSet()) : ReactivePreference<Set<String>>(key) {

        override fun getValue(thisRef: RxPreferences, property: KProperty<*>): Preference<Set<String>> {
            return thisRef.rxSharedPreferences.getStringSet(key, default)
        }
    }


    class ReactiveJsonPreference<T : Any>(key: String, val default: T) : ReactivePreference<T>(key) {
        override fun getValue(thisRef: RxPreferences, property: KProperty<*>): Preference<T> {
            return thisRef.rxSharedPreferences.getObject(key, default, GsonConverter<T>(default::class))
        }
    }


    private class GsonConverter<T : Any>(val clazz: KClass<out T>) : Preference.Converter<T> {

        private val gson = Gson()


        override fun deserialize(serialized: String): T {
            return gson.fromJson(serialized, clazz.java)
        }


        override fun serialize(value: T): String {
            return gson.toJson(value)
        }
    }

}