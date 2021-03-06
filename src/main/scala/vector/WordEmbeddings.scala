package vector

import java.io.File

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.lemurproject.galago.core.parse.Document
import org.lemurproject.galago.core.retrieval.RetrievalFactory
import vector.Utils._

import scala.collection.JavaConverters._

object WordEmbeddings {

  // Vector file paths
  private val VECTOR_PATH = "../ir_core/GoogleNews-vectors-negative300.bin.gz"
  private val FASTTEXT_VECTOR_PATH = "../ir_core/crawl-300d-2M.vec/crawl-300d-2M.vec"

  // Load the vector model (can take 4-5 min)
  private val word2Vec: Word2Vec = loadVectors(FASTTEXT_VECTOR_PATH)

  /**
    * Convert query into the tfidf representation
    * @param query
    * @param removeStopwords
    * @return
    */
  def query2Vec(query: String,
                removeStopwords: Boolean = true): Array[Double] = {
    // Get the list of terms
    val terms: List[String] = query
      .split(" +")
      .toList
      .filter(x => !STOPWORDS.contains(x))
      .map(x => normalizeTerm(x))

    createVector(terms)
  }

  /**
    * Convert the document to their tfidf representation
    * @param doc
    * @param removeStopwords
    * @return
    */
  def doc2Vec(doc: Document, removeStopwords: Boolean = true): Array[Double] = {

    val terms: List[String] = doc.terms.asScala.toList
      .filter(x => !STOPWORDS.contains(x))
      .map(x => normalizeTerm(x))

    createVector(terms)
  }

  /**
    * Return the computed vector of the sentence!
    *
    * @param terms
    * @return
    */
  private def createVector(terms: List[String]): Array[Double] = {
    // Create the vector we'll return
    var vec: Array[Double] = new Array[Double](300)

    for (term ← terms) {
      val v = word2Vec.getWordVector(term)
      // Sum the components of the vectors
      vec = (vec, v).zipped.map(_ + _)
    }

    // Return the averaged vector
    vec.map(x => x / terms.size)
  }

  /**
    * Load the vectors
    */
  private def loadVectors(path: String): Word2Vec = {
    val start = System.currentTimeMillis()
    println("Loading...")
    // Load the vectors so we can reuse them
    val serialized =  WordVectorSerializer.loadTxt(new File(path))
    val vectors : Word2Vec = new Word2Vec()
    vectors.setLookupTable(serialized.getLeft)
    vectors.setVocab(serialized.getRight)

    println("Loaded vectors!")
    println(s"It took ${System.currentTimeMillis()-start} ms to load")
    vectors
  }


  /**
  * Main for testing purposes
   *
   * @param args
   */
  // TODO delete the main
  def main(args: Array[String]): Unit = {
    val dc = new Document.DocumentComponents(true, true, true)
    val retrieval = RetrievalFactory.instance(Utils.INDEX_PATH)
    val doc = retrieval.getDocument(retrieval.getDocumentName(28L), dc)
    System.out.println(doc.text)

    val query = "naval air squadron attack"
    println(query)

    // Get the vectors
    val queryVec = query2Vec(query)
    val docVec = doc2Vec(doc)

    // Calculate the cosine similarity between the W2V of query and doc
    println(cosineSimilarity(queryVec, docVec))
  }

}
