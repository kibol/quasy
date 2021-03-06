package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.webservices.bioasq.BioASQService
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline}
import de.tu.dresden.quasy.model.annotation._
import java.text.Normalizer
import de.tu.dresden.quasy.webservices.model.DocumentsResult
import de.tu.dresden.quasy.webservices.nlm.NlmWebservice
import de.tu.dresden.quasy.model.db.AnnotationCache

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:01 PM
 */
class PubmedFetcher(serviceCall:(String,Int)=>DocumentsResult) extends CandidateFetcher{

    def fetch(question: Question, docCount:Int, pipeline:EnhancementPipeline = null) = {
        val query: String = question.context.getAnnotations[Token].filter(_.posTag.matches(PosTag.ANYNOUN_PATTERN +"|"+PosTag.ANYADJECTIVE_PATTERN)).map(_.coveredText).mkString(" ")
            /*question.context.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).
            map(oem => {
                "\""+oem.coveredText+"\""
            }).mkString(" ")   */
        fetchWithQuery(query, docCount, question, pipeline)
    }


    private def fetchWithQuery(query: String, docCount: Int, question: Question, pipeline: EnhancementPipeline): List[AnswerContext] = {
        val documents = serviceCall(query, docCount)

        //Normalization for UMLS enhancer, which can only handle ascii
        val texts = documents.documents.flatMap(document => {
            var result = List[AnnotatedText]()
            if (document.documentAbstract != null)
                result ::= {
                    val id = document.pmid + "-a"
                    val text = Normalizer.normalize(document.documentAbstract.replace("\n", " "), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", " ")
                    AnnotationCache.getCachedAnnotatedTextOrElse( text, new AnnotatedText(id,text))
                }

            if (document.sections != null)
                result ++= document.sections.zipWithIndex.map{
                    case (section,docId) => {
                        val id = document.pmid + "-s" + docId
                        val text = Normalizer.normalize(section.replace("\n", " "), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", " ")
                        AnnotationCache.getCachedAnnotatedTextOrElse( text, new AnnotatedText(id,text))
                    }
                }

            result
        })

        extractAnswerCandidates(texts, question, pipeline)
    }

    def fetchByPmid(question: Question, pmids: List[Int], pipeline: EnhancementPipeline) = {
        fetchWithQuery(pmids.map(_.toString+"[uid]").mkString(" OR "), pmids.size, question, pipeline)
    }
}

class NlmPubmedFetcher extends PubmedFetcher({ val service = new NlmWebservice; service.getPubmedDocuments })
class BioASQPubMedFetcher extends PubmedFetcher({ val service = new BioASQService; service.getPubmedDocuments })

