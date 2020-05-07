object QueryFileReader {
    fun readSqlFile(packageAndFilePath: String): String {
        return this::class.java.getResource("/db/queries/$packageAndFilePath.sql").readText()
    }
}
