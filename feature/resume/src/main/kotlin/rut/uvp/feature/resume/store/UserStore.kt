package rut.uvp.feature.resume.store

import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TextSplitter
import org.springframework.ai.vectorstore.SimpleVectorStore.SimpleVectorStoreBuilder
import org.springframework.ai.vectorstore.VectorStore
import java.io.File

class UserStoreImpl constructor(
    userId: String,
    private val textSplitter: TextSplitter,
    private val vectorStoreBuilder: SimpleVectorStoreBuilder,
) : UserStore {

    private val userFile: File = File(".${File.separator}$STORE_PATH${File.separator}$userId")

    init {
        if (!userFile.exists()) userFile.mkdirs()
    }

    override fun saveResume(resumeName: String, resumeContent: String): Boolean {
        val vectorStore = vectorStoreBuilder.build()
        val resumeFile = File(userFile, "$resumeName$STORE_FORMAT")
        val document = Document.builder().text(resumeContent).build()

        if (resumeFile.exists()) resumeFile.delete()

        val splitDocuments = textSplitter.split(document)

        vectorStore.add(splitDocuments)
        vectorStore.save(resumeFile)

        return true
    }

    override fun getResume(resumeName: String): VectorStore? {
        val vectorStore = vectorStoreBuilder.build()
        val resumeFile = File(userFile, "$resumeName$STORE_FORMAT")

        if (!resumeFile.exists()) return null

        vectorStore.load(resumeFile)

        return vectorStore
    }

    override fun removeResume(resumeName: String): Boolean {
        val resumeFile = File(userFile, "$resumeName$STORE_FORMAT")

        return resumeFile.delete()
    }

    companion object {
        const val STORE_PATH = "store"
        const val STORE_FORMAT = ".json"
    }
}

interface UserStore {

    fun saveResume(resumeName: String, resumeContent: String): Boolean
    fun getResume(resumeName: String): VectorStore?
    fun removeResume(resumeName: String): Boolean
}
