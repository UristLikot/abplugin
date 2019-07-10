import org.spongepowered.api.Sponge
import org.spongepowered.api.service.sql.SqlService
import java.lang.Exception

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException
import java.text.SimpleDateFormat
import javax.sql.DataSource;

class DbHandler {
    var sql: SqlService? = null
    @Throws(SQLException::class)
    fun getDataSource(jdbcUrl: String): DataSource {
        if (sql == null) {
            sql = Sponge.getServiceManager().provide(SqlService::class.java).get()
        }
        return sql!!.getDataSource(jdbcUrl)
    }

    @Throws(SQLException::class)
    fun createDb() {
        val uri = Sponge.getServiceManager().provide(SqlService::class.java).get().getConnectionUrlFromAlias("main").get()
        val cr = "CREATE DATABASE IF NOT EXISTS minecraft"
        val sql =
                "create table IF NOT EXISTS minecraft.bans\n" +
                        "(\n" +
                        "    uuid    text     null,\n" +
                        "    name    text     null,\n" +
                        "    created datetime null,\n" +
                        "    source  text     null,\n" +
                        "    expires datetime null,\n" +
                        "    reason  text     null\n" +
                        ");"
        getDataSource(uri).connection.use { conn ->
            conn.prepareStatement(cr).use { stmt ->
                stmt.executeQuery().use { results ->
                    println(results)
                    conn.close()
                }
            }
        }
        getDataSource(uri).connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { results ->
                    println(results)
                    conn.close()
                }
            }
        }

    }

    @Throws(SQLException::class)
    fun addBanToDb(uuid: String, name: String, created: String, source: String, expires: String, reason: String) {
        val uri = Sponge.getServiceManager().provide(SqlService::class.java).get().getConnectionUrlFromAlias("main").get()

        val sql = "insert into minecraft.bans (bans.uuid, bans.name, bans.created, bans.source, bans.expires, bans.reason )values ('$uuid','$name','$created','$source','$expires','$reason')"
        getDataSource(uri).connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                try {
                    stmt.executeQuery().use { _ ->
                        println("already exist")
                        conn.close()
                    }
                } catch (e: SQLIntegrityConstraintViolationException) {
                    println(e.message)
                }
                conn.close()
            }

        }

    }

    @Throws(SQLException::class)
    fun removeFromDb(name: String) {
        val uri = Sponge.getServiceManager().provide(SqlService::class.java).get().getConnectionUrlFromAlias("main").get()
        val sql = "DELETE FROM minecraft.bans where name='$name'"
        getDataSource(uri).connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                try {
                    stmt.executeQuery().use { _ ->
                        println("deleted")
                        conn.close()
                    }
                } catch (e: SQLIntegrityConstraintViolationException) {
                    println("do not exist")
                    conn.close()
                }
                conn.close()
            }

        }
    }

    @Throws(SQLException::class)
    fun refrBansDb(mp: MutableMap<String, String>) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val name = mp.get("name")
        val uuid = mp.get("uuid")
        val created = formatter.format(formatter.parse(mp.get("created")))
        val source = mp.get("source")
        val expires = formatter.format(formatter.parse(mp.get("expires")))
        val reason = mp.get("reason")
        val uri = Sponge.getServiceManager().provide(SqlService::class.java).get().getConnectionUrlFromAlias("main").get()
        val sql = "insert into minecraft.bans (bans.uuid, bans.name, bans.created, bans.source, bans.expires, bans.reason )values ('$uuid','$name','$created','$source','$expires','$reason')"
        getDataSource(uri).connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                try {
                    stmt.executeQuery().use { _ ->
                        println("refresh complete")
                        conn.close()
                    }
                } catch (e: SQLIntegrityConstraintViolationException) {
                    println("already in db")
                    conn.close()
                }
                conn.close()
            }

        }
    }
}