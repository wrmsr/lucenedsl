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

import com.google.inject.Inject;
import com.wrmsr.search.dsl.scoring.ScoreSupplier;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;

import java.io.IOException;
import java.util.Set;

public class SearcherImpl
    implements Searcher
{
    private final IndexSearcher indexSearcher;
    private final Set<DocSpecific> docSpecificSet;

    @Inject
    public SearcherImpl(
            IndexSearcher indexSearcher,
            Set<DocSpecific> docSpecificSet)
    {
        this.indexSearcher = indexSearcher;
        this.docSpecificSet = docSpecificSet;
    }

    @Override
    public ScoreDoc[] search(Query query, ScoreSupplier scoreSupplier, int maxHits)
            throws IOException
    {
        Query scoredQuery = new ComputedScoreQuery(new DocSpecific.Composite(docSpecificSet), scoreSupplier, query);
        TopDocsCollector topDocsCollector = TopScoreDocCollector.create(maxHits, true);
        indexSearcher.search(scoredQuery, topDocsCollector);
        return topDocsCollector.topDocs().scoreDocs;
    }
}
