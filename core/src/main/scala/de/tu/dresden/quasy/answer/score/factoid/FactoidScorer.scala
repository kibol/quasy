package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.model.annotation.OntologyConcept
import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.model.db.ScoreCache

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 10:55 AM
 */
trait FactoidScorer {
    protected val man:Manifest[_ <: FactoidScorer] = Manifest.classType(this.getClass)

    protected def scorable(factoidAnswer:FactoidAnswer) =true

    def score(factoid:FactoidAnswer):Double = {
        val cachedScore = ScoreCache.score(factoid)(man)

        val score =cachedScore.getOrElse(
            if (scorable(factoid))
                scoreInternal(factoid) else 0.0)

        factoid.addScore(score)(man)
        score
    }

    def scoreInternal(factoid:FactoidAnswer):Double

}
