/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wrmsr.search.dsl;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

public class ComputedScoreQuery
        extends Query
{
    protected final DocSpecific docSpecific;
    protected final Supplier<Float> scoreSupplier;

    protected final Query query;

    protected AtomicReaderContext lastContext;
    protected int lastDocId;

    public ComputedScoreQuery(
            DocSpecific docSpecific,
            Supplier<Float> scoreSupplier,
            Query query)
    {
        this.docSpecific = docSpecific;
        this.scoreSupplier = scoreSupplier;
        this.query = query;
    }

    public Query getQuery()
    {
        return query;
    }

    @Override
    public Query rewrite(IndexReader reader)
            throws IOException
    {
        Query rewritten = query.rewrite(reader);
        if (rewritten != query) {
            rewritten = new ComputedScoreQuery(docSpecific, scoreSupplier, rewritten);
            rewritten.setBoost(this.getBoost());
            return rewritten;
        }
        return this;
    }

    @Override
    public void extractTerms(Set<Term> terms)
    {
        // TODO: OK to not add any terms when wrapped a filter
        // and used with MultiSearcher, but may not be OK for
        // highlighting.
        // If a query was wrapped, we delegate to query.
        query.extractTerms(terms);
    }

    protected class ComputedWeight
            extends Weight
    {
        private final Weight innerWeight;
        private float queryNorm;
        private float queryWeight;

        public ComputedWeight(IndexSearcher searcher)
                throws IOException
        {
            this.innerWeight = (query == null) ? null : query.createWeight(searcher);
        }

        @Override
        public Query getQuery()
        {
            return ComputedScoreQuery.this;
        }

        @Override
        public float getValueForNormalization()
                throws IOException
        {
            // we calculate sumOfSquaredWeights of the inner weight, but ignore it (just to initialize everything)
            if (innerWeight != null) {
                innerWeight.getValueForNormalization();
            }
            queryWeight = getBoost();
            return queryWeight * queryWeight;
        }

        @Override
        public void normalize(float norm, float topLevelBoost)
        {
            this.queryNorm = norm * topLevelBoost;
            queryWeight *= this.queryNorm;
            // we normalize the inner weight, but ignore it (just to initialize everything)
            if (innerWeight != null) {
                innerWeight.normalize(norm, topLevelBoost);
            }
        }

        @Override
        public Scorer scorer(AtomicReaderContext context, Bits acceptDocs)
                throws IOException
        {
            final DocIdSetIterator disi;
            assert query != null && innerWeight != null;
            disi = innerWeight.scorer(context, acceptDocs);
            if (disi == null) {
                return null;
            }
            return new ComputedScorer(context, disi, this, queryWeight);
        }

        @Override
        public boolean scoresDocsOutOfOrder()
        {
            return (innerWeight != null) ? innerWeight.scoresDocsOutOfOrder() : false;
        }

        @Override
        public Explanation explain(AtomicReaderContext context, int doc)
                throws IOException
        {
            final Scorer cs = scorer(context, context.reader().getLiveDocs());
            final boolean exists = (cs != null && cs.advance(doc) == doc);

            final ComplexExplanation result = new ComplexExplanation();
            if (exists) {
                result.setDescription(ComputedScoreQuery.this.toString() + ", product of:");
                result.setValue(queryWeight);
                result.setMatch(Boolean.TRUE);
                result.addDetail(new Explanation(getBoost(), "boost"));
                result.addDetail(new Explanation(queryNorm, "queryNorm"));
            }
            else {
                result.setDescription(ComputedScoreQuery.this.toString() + " doesn't match id " + doc);
                result.setValue(0);
                result.setMatch(Boolean.FALSE);
            }
            return result;
        }
    }

    protected class ComputedScorer
            extends Scorer
    {
        private final AtomicReaderContext context;
        private final DocIdSetIterator docIdSetIterator;
        private final float queryWeight;

        private int docId;

        public ComputedScorer(AtomicReaderContext context, DocIdSetIterator docIdSetIterator, Weight w, float queryWeight)
        {
            super(w);
            this.context = context;
            this.docIdSetIterator = docIdSetIterator;
            this.queryWeight = queryWeight;
        }

        @Override
        public int nextDoc()
                throws IOException
        {
            docId = docIdSetIterator.nextDoc();
            return docId;
        }

        @Override
        public int docID()
        {
            return docIdSetIterator.docID();
        }

        @Override
        public float score()
                throws IOException
        {
            assert docIdSetIterator.docID() != NO_MORE_DOCS;
            if (lastContext != context) {
                docSpecific.setAtomicReaderContext(context);
                lastContext = context;
            }
            if (lastDocId != docId) {
                docSpecific.setDocId(docId);
                lastDocId = docId;
            }
            return scoreSupplier.get() * queryWeight;
        }

        @Override
        public int freq()
                throws IOException
        {
            return 1;
        }

        @Override
        public int advance(int target)
                throws IOException
        {
            docId = docIdSetIterator.advance(target);
            return docId;
        }

        @Override
        public long cost()
        {
            return docIdSetIterator.cost();
        }

        @Override
        public Collection<ChildScorer> getChildren()
        {
            if (query != null) {
                return Collections.singletonList(new ChildScorer((Scorer) docIdSetIterator, "computed"));
            }
            else {
                return Collections.emptyList();
            }
        }
    }

    @Override
    public Weight createWeight(IndexSearcher searcher)
            throws IOException
    {
        return new ComputedScoreQuery.ComputedWeight(searcher);
    }

    @Override
    public String toString(String field)
    {
        return new StringBuilder("ComputedScore(")
                .append(query.toString(field))
                .append(')')
                .append(ToStringUtils.boost(getBoost()))
                .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (o instanceof ComputedScoreQuery) {
            final ComputedScoreQuery other = (ComputedScoreQuery) o;
            return
                    ((this.query == null) ? other.query == null : this.query.equals(other.query));
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() +
                query.hashCode();
    }
}
