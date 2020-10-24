package com.ericlam.mc.kotlib.config

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.KotlinPlugin
import com.ericlam.mc.kotlib.config.controller.FileController
import com.ericlam.mc.kotlib.config.controller.MessageGetter
import com.ericlam.mc.kotlib.config.dao.*
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import com.ericlam.mc.kotlib.config.dto.MessageFile
import com.ericlam.mc.kotlib.not
import com.ericlam.mc.kotlib.runIf
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmName


class ConfigBuilder(private val plugin: KotlinPlugin) : ConfigFactory {

    private val list: MutableList<KClass<out ConfigFile>> = mutableListOf()
    private val daoMap: MutableMap<KClass<out DataFile>, KClass<out Dao<out DataFile, *>>> = mutableMapOf()

    override fun <T : ConfigFile> register(config: KClass<T>): ConfigFactory {
        list.add(config).also { return this }
    }

    override fun <T : DataFile, V : Dao<T, *>> registerDao(config: KClass<T>, dao: KClass<V>): ConfigFactory {
        daoMap[config] = dao
        return this
    }

    private operator fun File.get(file: String) = File(this, file)

    override fun dump(): ConfigManager {
        return object : ConfigManager {

            private val map: MutableMap<KClass<out ConfigFile>, ConfigFile> = mutableMapOf()
            private val daoRepoMap: MutableMap<KClass<out Dao<out DataFile, *>>, Dao<out DataFile, *>> = mutableMapOf()
            private val delegateMap: MutableMap<KClass<out DataFile>, CustomDao<out DataFile, *>> = ConcurrentHashMap()

            private val ktModule by lazy {
                KotlinModule().runIf(!KotLib.isBungee(plugin)) { CSObject.setupCSForBukkit(it) }
            }

            private val mapper: ObjectMapper by lazy {
                ObjectMapper(YAMLFactory.builder()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).build())
                        .registerModule(ktModule)
                        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                        .enable(JsonParser.Feature.ALLOW_YAML_COMMENTS)
                        .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                        .enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .skipType(FileController::class.java)
                        .skipType(MessageGetter::class.java)
                        .setDefaultPropertyInclusion(JsonInclude.Value.construct(null, JsonInclude.Include.NON_NULL))

            }

            init {
                list.forEach { reload(it) }
                @Suppress("UNCHECKED_CAST")
                daoMap.forEach { (k, v) -> initDao(k, v as KClass<out Dao<DataFile, *>>) }
            }

            fun ObjectMapper.skipType(type: Class<*>?): ObjectMapper {
                val config = this.configOverride(type)
                config.visibility = JsonAutoDetect.Value.construct(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                config.ignorals = JsonIgnoreProperties.Value.forIgnoreUnknown(true)
                config.isIgnoredType = true
                config.setterInfo = JsonSetter.Value.construct(Nulls.SKIP, Nulls.SKIP)
                return this
                /*
                return this.withConfigOverride(type) { MutableConfigOverride().nullHandling = JsonSetter.Value.construct(Nulls.SKIP, Nulls.SKIP) }.changeDefaultVisibility { vis ->
                    vis.with(JsonAutoDetect.Visibility.NONE)
                }
                 */
            }

            fun customSetter(mapperConsumer: (ObjectMapper) -> Unit) {
                mapperConsumer(mapper)
            }

            private fun <T : ConfigFile> reload(config: KClass<T>) {
                val res = config.findAnnotation<Resource>()
                        ?: throw IllegalStateException("${config.jvmName} is lack of @Resource annotation")
                val f = plugin.kDataFolder[res.copyTo.takeUnless { it.isBlank() } ?: res.locate]
                plugin.saveResource(res.locate, f)
                val ins = mapper.readValue(f, config.java)
                val superCls = config.allSuperclasses.toList()[if (ins is MessageFile) 1 else 0]

                val controller = object : FileController {

                    override fun <T : ConfigFile> save(configFile: T) {
                        mapper.writeValue(f, configFile)
                    }

                    override fun <T : ConfigFile> reload(configFile: T) {
                        reload(config)
                        val latest = getConfig(config)
                        configFile::class.java.declaredFields.forEach { f ->
                            val dataField = latest::class.java.getDeclaredField(f.name)
                            dataField.isAccessible = true
                            val data = dataField.get(latest)
                            f.isAccessible = true
                            f.set(configFile, data)
                        }
                    }
                }

                superCls.java.getDeclaredField("controller").also { field -> field.isAccessible = true; field.set(ins, controller) }
                if (ins is MessageFile) {
                    val pre = config.findAnnotation<Prefix>()
                            ?: throw IllegalStateException("${config.simpleName} is lack of @Prefix annotation")
                    val messageGetter = if (KotLib.isBungee(plugin)) BungeeMessageGetter(pre, f) else BukkitMessageGetter(pre, f)
                    config.superclasses[0].java.getDeclaredField("getter").also { field -> field.isAccessible = true; field.set(ins, messageGetter) }
                }

                map[config] = ins
            }

            private fun <T : DataFile, V : Dao<T, *>> initDao(data: KClass<out T>, dao: KClass<out V>) {
                val folderPath = data.findAnnotation<DataResource>()
                        ?: throw IllegalStateException("Data class $data have not marked @DataResource")
                val folder = plugin.kDataFolder[folderPath.folder]
                folder.mkdirs()
                val primaryFields = data.declaredMemberProperties.filter { p -> p.hasAnnotation<PrimaryKey>() }
                if (primaryFields.size > 1) throw IllegalStateException("Primary key in data class $data has more than one")
                val primary = primaryFields.firstOrNull()
                        ?: throw IllegalStateException("No Primary Key in data class $data")
                primary.isAccessible = true
                if (primary is KMutableProperty<*>) throw IllegalStateException("Primary Key($primary) must be val assigned")
                if (primary.returnType.isMarkedNullable) throw IllegalStateException("Primary Key($primary) cannot be nullable")
                val genericArgs = dao.supertypes[0].arguments.map { it.type }
                if (primary.returnType != genericArgs[1]) throw IllegalStateException("the registered dao ($dao) does not using primary key ($primary): PrimaryType(${primary.returnType}), ControllerType(${genericArgs[1]})")
                val foreignFields = data.declaredMemberProperties.filter { p -> p.hasAnnotation<ForeignKey>() }
                foreignFields.forEach { m ->
                    m.isAccessible = true
                    if (m is KMutableProperty<*> && KotLib.config.foreign_key_unchangeable) throw IllegalStateException("Foreign Key($m) must be val assigned as foreign_key_unchangeable in config.yml is true")
                    if (m.returnType.isMarkedNullable && KotLib.config.foreign_key_mode != KotLib.Config.ForeignKeyMode.NULLABLE) throw IllegalStateException("Foreign Key($m) cannot be nullable when ForeignKeyMode is not nullable")
                    val foreignCls = m.findAnnotation<ForeignKey>()?.link ?: return@forEach
                    if (foreignCls == data) throw IllegalStateException("Foreign key class ($foreignCls) and data class ($data) are the same")
                    foreignCls.declaredMemberProperties
                            .any { mem -> mem.hasAnnotation<PrimaryKey>() && mem.returnType skipNullEqual m.returnType }
                            .not(false)
                            ?: throw IllegalStateException("$foreignCls does not contain PrimaryKey ($m) while ($dao) has a foreign key linked to ($foreignCls)")
                    val foreignObj = data.declaredMemberProperties
                            .find { o -> o.hasAnnotation<ForeignObject>() && o.returnType skipNullEqual foreignCls }
                            ?: throw IllegalStateException("Foreign key ($foreignCls) without Foreign Object")
                    if (!foreignObj.returnType.isMarkedNullable) {
                        throw IllegalStateException("$foreignCls's Foreign Object must be nullable")
                    }
                    if (foreignObj !is KMutableProperty1<out T, *>) {
                        throw IllegalStateException("Foreign Object must be declared as var ($foreignCls)")
                    }
                }
                val foreigns = foreignFields.map {
                    it to data.declaredMemberProperties
                            .filter { o -> o.hasAnnotation<ForeignObject>() }
                            .find { f -> f.returnType skipNullEqual it.findAnnotation<ForeignKey>()!!.link }!! as KMutableProperty1<out T, *>
                }.toMap()

                @Suppress("UNCHECKED_CAST")
                val daoImpl =
                        CustomDao(
                                data as KClass<T>,
                                genericArgs[1]?.javaType as Class<Comparable<Any>>,
                                folder,
                                primary as KProperty1<T, Any>,
                                foreigns as Map<KProperty1<T, *>, KMutableProperty1<T, Any?>>)
                val c = dao.primaryConstructor?.takeIf { it.parameters[0].type.isSubtypeOf(Dao::class.starProjectedType) }
                        ?: throw IllegalStateException("Primary Constructor should have one delegate parameter of ${Dao::class}")
                daoRepoMap[dao] = c.call(daoImpl)
                delegateMap[data] = daoImpl
            }

            override fun <T : ConfigFile> getConfig(config: KClass<T>): T {
                return config.safeCast(map[config]) ?: throw IllegalStateException("${config.simpleName} is not exist.")
            }

            override fun <T : DataFile, V : Dao<T, *>> getDao(dao: KClass<V>): V {
                return dao.java.cast(daoRepoMap[dao]) ?: throw IllegalStateException("Cannot find dao type $dao")
            }

            private inner class CustomDao<T : DataFile, V : Comparable<V>>(
                    private val data: KClass<T>,
                    private val returnType: Class<V>,
                    private val folder: File,
                    private val primary: KProperty1<T, Any>,
                    private val foreigns: Map<KProperty1<T, *>, KMutableProperty1<T, Any?>>
            ) : Dao<T, V> {

                init {
                    plugin.onStop { saveAll() }
                }


                override fun findByForeignId(foreignData: KClass<out DataFile>, id: Any): List<T> {
                    KotLib.debug("findByForeignId($foreignData, $id) in $data")
                    if (foreigns.isEmpty() || foreignData == data) return emptyList()
                    return find {
                        foreigns.keys.any { f ->
                            f.isAccessible = true
                            val foreignCls = f.findAnnotation<ForeignKey>()!!.link
                            foreignData == foreignCls.java && f.get(this).toString() == id.toString()
                        }
                    }
                }

                fun findIdsByForeignId(foreignData: KClass<out DataFile>, id: Any): List<V> {
                    KotLib.debug("findIdsByForeignId($foreignData, $id) in $data")
                    return findByForeignId(foreignData, id).map { returnType.cast(primary.get(it)) }
                }

                private fun deleteForeignData(id: String) {
                    KotLib.debug("deleteForeignData($id) in $data")
                    for (dao in delegateMap.values) {
                        if (dao == this) continue
                        dao.findIdsByForeignId(data, id).forEach { dao.forceDelete(it.toString()) }
                    }
                }

                private fun getForeignObject(foreignField: KProperty1<T, *>, id: Any): Any? {
                    val foreignData = foreignField.findAnnotation<ForeignKey>()!!.link
                    foreignData.declaredMemberProperties.find { f -> f.hasAnnotation<PrimaryKey>() && f.returnType skipNullEqual foreignField.returnType }
                            ?: throw IllegalStateException("foreignKey $foreignData does not have primary key")
                    return delegateMap[foreignData]?.findByIdString(id.toString())
                }

                private var cache: MutableMap<String, T> = ConcurrentHashMap()

                private fun saveAll() {
                    KotLib.debug("saveAll() in $data， cache: $cache")
                    if (cache.isEmpty()) {
                        KotLib.debug("cache is empty, skipped saving")
                        return
                    }
                    cache.values.forEach { save { it } }
                }

                private fun Map<String, T>.updateCaches() {
                    if (this.isEmpty()) return
                    KotLib.debug("updateCaches, before cache: $cache")
                    cache.putAll(this)
                    KotLib.debug("updateCaches, after cache: $cache")
                }

                private fun save(key: String) {
                    cache.remove(key)?.let {
                        KotLib.debug("prepare to save($key), value: $it")
                        save { it }
                    }
                }

                override fun findAll(): List<T> {
                    KotLib.debug("findAll() in $data, updating Caches")
                    val map = gainAll().toMap()
                    map.updateCaches()
                    return map.values.toList()
                }

                private fun gainAll(): List<Pair<String, T>> {
                    KotLib.debug("gainAll() in $data")
                    saveAll()
                    KotLib.debug("folder list files size: ${folder.listFiles()?.size}")
                    KotLib.debug("folder list details: ${folder.listFiles()?.toList() ?: "[]"}")
                    val list = folder.listFiles()?.takeIf { it.isNotEmpty() }?.map { mapper.readValue(it, data.java).assignForeignObject() }
                            ?: emptyList()
                    val keys = folder.listFiles()?.takeIf { it.isNotEmpty() }?.map { it.nameWithoutExtension }
                            ?: emptyList()
                    return (keys zip list)
                }

                override fun findById(id: V): T? {
                    KotLib.debug("findById($id) in $data")
                    save(id.toString())
                    return findByIdString(id.toString())?.also { cache[id.toString()] = it }
                }

                override fun update(id: V, update: T.() -> Unit): T? {
                    KotLib.debug("update($id, $update) in $data")
                    val oldData = findById(id)
                    oldData?.also { update(it); save { it } }
                    return oldData?.also { cache[id.toString()] = it }
                }

                override fun delete(id: V): Boolean {
                    KotLib.debug("delete($id) in $data")
                    return forceDelete(id.toString())
                }

                fun forceDelete(id: String): Boolean {
                    KotLib.debug("forceDelete($id) in $data")
                    val f = File(folder, "$id.yml")
                    return Files.deleteIfExists(f.toPath()).also { if (it) cache.remove(id) }.also {
                        if (KotLib.config.foreign_key_mode == KotLib.Config.ForeignKeyMode.DELETE) {
                            deleteForeignData(id)
                        }
                    }
                }

                override fun find(filter: T.() -> Boolean): List<T> {
                    KotLib.debug("find($filter) in $data")
                    val map = gainAll().filter { it.second.filter() }.toMap()
                    map.updateCaches()
                    return map.values.toList()
                }

                override fun deleteSome(filter: T.() -> Boolean): List<V> {
                    KotLib.debug("deleteSome($filter) in $data")
                    return find(filter).map { returnType.cast(primary.get(it)) }.also { it.forEach { f -> delete(f) } }
                }

                fun findByIdString(id: String): T? {
                    KotLib.debug("findByIdString($id) in $data")
                    val f = File(folder, "$id.yml")
                    return f.takeIf { it.exists() }?.let { mapper.readValue(f, data.java) }?.assignForeignObject()
                }

                override fun save(data: () -> T): V? {
                    val ins = data()
                    KotLib.debug("saving $ins in ${this.data}")
                    var value: Any? = null
                    val unknownForeignData = foreigns.keys.any { f ->
                        f.isAccessible = true
                        val v = f.get(ins)
                        val dFile = f.findAnnotation<ForeignKey>()!!.link
                        delegateMap[dFile]?.findByIdString(v.toString())?.let { false } ?: let {
                            value = v
                            true
                        }
                    }
                    if (unknownForeignData && KotLib.config.yaml_warn_if_foreign_key_unknown) {
                        plugin.warning("In $ins, the value ($value) with foreign key cannot match any data of primary key")
                    }
                    val id = returnType.cast(primary.get(ins))
                    val f = File(folder, "$id.yml")
                    f.createNewFile()
                    mapper.writeValue(f, ins)
                    return id
                }

                fun T.assignForeignObject(): T = this.also { ins ->
                    foreigns.forEach { (f, obj) ->
                        f.isAccessible = true
                        obj.isAccessible = true
                        val foreignObj = f.get(ins)?.let { getForeignObject(f, it) }
                        obj.set(ins, foreignObj)
                    }
                }

            }

            infix fun KType.skipNullEqual(type: KClass<*>): Boolean {
                return this.javaType == type.java
            }

            infix fun KType.skipNullEqual(type: KType): Boolean {
                return this.javaType == type.javaType
            }
        }
    }
}
