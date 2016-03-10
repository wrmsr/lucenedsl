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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wrmsr.search.dsl.query.node.QueryNode;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public interface SearchService
{
    class Doc
    {
        private final String title;
        private final String isbn;

        @JsonCreator
        public Doc(
                @JsonProperty("title") String title,
                @JsonProperty("isbn") String isbn)
        {
            this.title = title;
            this.isbn = isbn;
        }

        @JsonProperty("title")
        public String getTitle()
        {
            return title;
        }

        @JsonProperty("isbn")
        public String getIsbn()
        {
            return isbn;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Doc doc = (Doc) o;
            return Objects.equals(title, doc.title) &&
                    Objects.equals(isbn, doc.isbn);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(title, isbn);
        }

        @Override
        public String toString()
        {
            return "Doc{" +
                    "title='" + title + '\'' +
                    ", isbn='" + isbn + '\'' +
                    '}';
        }
    }

    void addDoc(Doc doc)
            throws IOException;

    void commit()
            throws IOException;

    class Hit
    {
        private final Doc doc;
        private final float score;

        @JsonCreator
        public Hit(
                @JsonProperty("doc") Doc doc,
                @JsonProperty("score") float score)
        {
            this.doc = doc;
            this.score = score;
        }

        @JsonProperty("doc")
        public Doc getDoc()
        {
            return doc;
        }

        @JsonProperty("score")
        public float getScore()
        {
            return score;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Hit hit = (Hit) o;
            return Float.compare(hit.score, score) == 0 &&
                    Objects.equals(doc, hit.doc);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(doc, score);
        }

        @Override
        public String toString()
        {
            return "Hit{" +
                    "doc=" + doc +
                    ", score=" + score +
                    '}';
        }
    }

    List<Hit> searchDocs(QueryNode queryNode, int maxHits)
            throws IOException;
}
