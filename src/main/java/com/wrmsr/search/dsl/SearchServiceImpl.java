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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.wrmsr.search.dsl.lucene.LuceneQueryCompiler;
import com.wrmsr.search.dsl.lucene.QueryTermRenderer;
import com.wrmsr.search.dsl.query.node.QueryNode;
import com.wrmsr.search.dsl.scoring.ScoreSupplier;
import com.wrmsr.search.dsl.utils.ScopeListeners;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.checkState;

public class SearchServiceImpl
        implements SearchService
{
    private final Injector injector;
    private final SearchScope searchScope;
    private final ScopeListeners<SearchScope> searchScopeListeners;

    private final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
    private final Directory directory = new RAMDirectory();

    private final ReadWriteLock indexSearcherLock = new ReentrantReadWriteLock();

    private volatile Optional<IndexWriter> indexWriter = Optional.empty();
    private volatile Optional<IndexSearcher> indexSearcher = Optional.empty();

    private static final FieldType FIELD_TYPE = new FieldType();

    static {
        FIELD_TYPE.setIndexed(true);
        FIELD_TYPE.setStored(true);
        FIELD_TYPE.setTokenized(true);
        FIELD_TYPE.freeze();
    }

    @Inject
    public SearchServiceImpl(
            Injector injector,
            SearchScope searchScope,
            ScopeListeners<SearchScope> searchScopeListeners)
    {
        this.injector = injector;
        this.searchScope = searchScope;
        this.searchScopeListeners = searchScopeListeners;
    }

    @Override
    public synchronized void addDoc(Doc doc)
            throws IOException
    {
        if (!this.indexWriter.isPresent()) {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
            IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
            this.indexWriter = Optional.of(indexWriter);
        }

        IndexWriter indexWriter = this.indexWriter.get();
        Document document = new Document();
        document.add(new Field("title", doc.getTitle(), FIELD_TYPE));
        document.add(new Field("isbn", doc.getIsbn(), FIELD_TYPE));
        indexWriter.addDocument(document);
    }

    @Override
    public synchronized void commit()
            throws IOException
    {
        checkState(this.indexWriter.isPresent());

        Lock lock = indexSearcherLock.writeLock();
        try {
            lock.lock();

            if (this.indexSearcher.isPresent()) {
                IndexSearcher indexSearcher = this.indexSearcher.get();
                indexSearcher.getIndexReader().close();
                this.indexSearcher = Optional.empty();
            }

            IndexWriter indexWriter = this.indexWriter.get();
            indexWriter.commit();
            indexWriter.close();

            IndexReader indexReader = IndexReader.open(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            this.indexSearcher = Optional.of(indexSearcher);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public List<Hit> searchDocs(QueryNode queryNode, int maxHits)
            throws IOException
    {
        Query query = new LuceneQueryCompiler(analyzer, new QueryTermRenderer()).compileQuery(queryNode);
        ScoreSupplier scoreSupplier = () -> 100.0f;

        Lock lock = indexSearcherLock.readLock();
        try {
            lock.lock();

            checkState(this.indexSearcher.isPresent());
            IndexSearcher indexSearcher = this.indexSearcher.get();

            searchScope.enter();
            try {
                searchScope.seed(Key.get(IndexSearcher.class), indexSearcher);
                searchScopeListeners.enter();
                try {
                    final Searcher searcher = injector.getInstance(Searcher.class);
                    ScoreDoc[] scoreDocs = searcher.search(query, scoreSupplier, maxHits);

                    ImmutableList.Builder<Hit> builder = ImmutableList.builder();
                    for (int i = 0; i < scoreDocs.length; ++i) {
                        ScoreDoc scoreDoc = scoreDocs[i];
                        Document document = indexSearcher.doc(scoreDoc.doc);
                        Doc doc = new Doc(document.get("title"), document.get("isbn"));
                        Hit hit = new Hit(doc, scoreDoc.score);
                        builder.add(hit);
                    }
                    return builder.build();
                }
                finally {
                    searchScopeListeners.exit();
                }
            }
            catch (RuntimeException | IOException e) {
                throw Throwables.propagate(e);
            }
            finally {
                searchScope.exit();
            }
        }
        finally {
            lock.unlock();
        }
    }
}
