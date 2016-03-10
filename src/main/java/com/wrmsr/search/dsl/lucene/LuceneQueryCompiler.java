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
package com.wrmsr.search.dsl.lucene;

import com.wrmsr.search.dsl.query.node.BooleanQueryNode;
import com.wrmsr.search.dsl.query.node.BoostedQueryNode;
import com.wrmsr.search.dsl.query.node.ConstantScoreQueryNode;
import com.wrmsr.search.dsl.query.node.MatchAllQueryNode;
import com.wrmsr.search.dsl.query.node.MatchQueryNode;
import com.wrmsr.search.dsl.query.node.QueryNode;
import com.wrmsr.search.dsl.query.QueryNodeCompiler;
import com.wrmsr.search.dsl.query.node.QueryNodeVisitor;
import com.wrmsr.search.dsl.query.node.RangeQueryNode;
import com.wrmsr.search.dsl.query.node.TermQueryNode;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

import java.io.IOException;
import java.util.Objects;

public class LuceneQueryCompiler
        implements QueryNodeCompiler<Query>
{
    protected final Analyzer analyzer;
    protected final QueryTermRenderer queryTermRenderer;

    public LuceneQueryCompiler(Analyzer analyzer, QueryTermRenderer queryTermRenderer)
    {
        this.analyzer = analyzer;
        this.queryTermRenderer = queryTermRenderer;
    }

    @Override
    public Query compileQuery(QueryNode root)
    {
        return root.accept(new Visitor(), new Context());
    }

    protected static class Context
    {
    }

    protected class Visitor
            extends QueryNodeVisitor<Context, Query>
    {
        @Override
        protected Query visitNode(QueryNode node, Context context)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query visitMatchAllQueryNode(MatchAllQueryNode node, Context context)
        {
            Query query = new MatchAllDocsQuery();
            return query;
        }

        @Override
        public Query visitTermQueryNode(TermQueryNode node, Context context)
        {
            return new TermQuery(queryTermRenderer.renderQueryTerm(node.getField(), node.getTerm()));
        }

        @Override
        public Query visitMatchQueryNode(MatchQueryNode node, Context context)
        {
            BooleanQuery query = new BooleanQuery();
            try {
                TokenStream tokenStream = analyzer.tokenStream(node.getField(), node.getText());
                CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
                try {
                    tokenStream.reset();
                    query.setMinimumNumberShouldMatch(1);
                    while (tokenStream.incrementToken()) {
                        query.add(new TermQuery(new Term(node.getField(), termAtt.toString())), BooleanClause.Occur.SHOULD);
                    }
                    return query;
                }
                finally {
                    tokenStream.close();
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Query visitBooleanQueryNode(BooleanQueryNode node, Context context)
        {
            BooleanQuery query = new BooleanQuery();
            query.setMinimumNumberShouldMatch(node.getminimumNumberShouldMatch());
            node.getClauses().stream().forEach(c -> {
                final BooleanClause.Occur occur;
                if (c instanceof BooleanQueryNode.ShouldClause) {
                    occur = BooleanClause.Occur.SHOULD;
                }
                else if (c instanceof BooleanQueryNode.MustClause) {
                    occur = BooleanClause.Occur.MUST;
                }
                else if (c instanceof BooleanQueryNode.MustNotClause) {
                    occur = BooleanClause.Occur.MUST_NOT;
                }
                else {
                    throw new IllegalArgumentException(Objects.toString(c));
                }
                query.add(c.getQuery().accept(this, context), occur);
            });
            return query;
        }

        @Override
        public Query visitConstantScoreQueryNode(ConstantScoreQueryNode node, Context context)
        {
            Query query = new ConstantScoreQuery(node.getQuery().accept(this, context));
            query.setBoost(node.getBoost());
            return query;
        }

        @Override
        public Query visitBoostedQueryNode(BoostedQueryNode node, Context context)
        {
            Query query = node.getQuery().accept(this, context);
            query.setBoost(node.getBoost());
            return query;
        }

        @Override
        public Query visitRangeQueryNode(RangeQueryNode node, Context context)
        {
            return new TermRangeQuery(
                    node.getField(),
                    queryTermRenderer.renderQueryTerm(node.getField(), node.getLower()).bytes(),
                    queryTermRenderer.renderQueryTerm(node.getField(), node.getUpper()).bytes(),
                    node.isIncludeLower(),
                    node.isIncludeUpper());
        }
    }
}
