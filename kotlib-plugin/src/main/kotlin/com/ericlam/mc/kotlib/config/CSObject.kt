package com.ericlam.mc.kotlib.config

import com.ericlam.mc.kotlib.KotLib
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization

object CSObject {

    fun setupCSForBukkit(module: SimpleModule): SimpleModule {
        return module.setDeserializerModifier(CustomModifierD).setSerializerModifier(CustomModifierS)
    }


    @Suppress("UNCHECKED_CAST")
    object CustomModifierD : BeanDeserializerModifier() {
        override fun modifyDeserializer(config: DeserializationConfig?, beanDesc: BeanDescription?, deserializer: JsonDeserializer<*>?): JsonDeserializer<*> {
            val type = beanDesc!!.beanClass
            if (ConfigurationSerializable::class.java.isAssignableFrom(type)) {
                return Deserializer(beanDesc.beanClass as Class<ConfigurationSerializable>)
            }
            return super.modifyDeserializer(config, beanDesc, deserializer)
        }
    }

    @Suppress("UNCHECKED_CAST")
    object CustomModifierS : BeanSerializerModifier() {
        override fun modifySerializer(config: SerializationConfig?, beanDesc: BeanDescription?, serializer: JsonSerializer<*>?): JsonSerializer<*> {
            val type = beanDesc!!.beanClass
            if (ConfigurationSerializable::class.java.isAssignableFrom(type)) {
                return Serializer(beanDesc.beanClass as Class<ConfigurationSerializable>)
            }
            return super.modifySerializer(config, beanDesc, serializer)
        }
    }


    private class Serializer<T : ConfigurationSerializable>(private val t: Class<T>) : JsonSerializer<T>(), ContextualSerializer {

        override fun createContextual(p0: SerializerProvider?, p1: BeanProperty?): JsonSerializer<*> {
            val type = p1!!.type.rawClass
            KotLib.debug("serializing $type")
            return if (ConfigurationSerializable::class.java.isAssignableFrom(type)) {
                KotLib.debug("$type is a sub class of ConfigurationSerializable, using custom serializer")
                this
            } else {
                p0!!.findValueSerializer(type)
            }
        }

        override fun handledType(): Class<T> {
            return t
        }

        override fun serialize(p0: T, p1: JsonGenerator?, p2: SerializerProvider?) {
            fun <T : ConfigurationSerializable> toMap(o: T): LinkedHashMap<String, Any> {
                return LinkedHashMap<String, Any>()
                        .also { it["hash"] = o.hashCode() }
                        .also { it["=="] = ConfigurationSerialization.getAlias(o::class.java) }
                        .also { m ->
                            m.putAll(o.serialize()
                                    .map { (k, v) ->
                                        val value = if (v is ConfigurationSerializable) {
                                            KotLib.debug("${v::class.simpleName} is a sub class of ConfigurationSerializable, using recursive method")
                                            toMap(v)
                                        } else v
                                        Pair(k, value)
                                    })
                        }
            }

            val m = toMap(p0)
            p2!!.defaultSerializeValue(m, p1)
        }
    }

    private class Deserializer<T : ConfigurationSerializable>(private val t: Class<T>) : JsonDeserializer<T>(), ContextualDeserializer {

        override fun createContextual(p0: DeserializationContext?, p1: BeanProperty?): JsonDeserializer<*> {
            val type = p1?.type ?: p0!!.contextualType
            KotLib.debug("type: ${p1?.type}")
            KotLib.debug("contextualType: ${p0?.contextualType}")
            return if (ConfigurationSerializable::class.java.isAssignableFrom(type.rawClass)) {
                KotLib.debug("$type is a sub class of ConfigurationSerializable, using custom deserializer")
                this
            } else {
                p0!!.findRootValueDeserializer(type)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun deserialize(p0: JsonParser?, p1: DeserializationContext?): T {
            fun <T : ConfigurationSerializable> toSerializableObject(map: Map<String, *>): T {
                KotLib.debug(map.toString())
                val serializeMap = map.map { (k, v) ->
                    val value = if (v is Map<*, *> && v.containsKey("==")) toSerializableObject(v as Map<String, *>) else v
                    k to value
                }.toMap()
                return ConfigurationSerialization.deserializeObject(serializeMap) as T
            }

            val map = p0!!.readValueAs(Map::class.java) as Map<String, *>
            return toSerializableObject(map)
        }

        override fun handledType(): Class<*> {
            return t
        }
    }
}