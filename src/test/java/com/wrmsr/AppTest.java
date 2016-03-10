package com.wrmsr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wrmsr.search.dsl.AppModule;
import com.wrmsr.search.dsl.SearchService;
import com.wrmsr.search.dsl.query.node.BooleanQueryNode;
import com.wrmsr.search.dsl.query.node.MatchQueryNode;
import com.wrmsr.search.dsl.query.node.QueryNode;
import com.wrmsr.search.dsl.query.node.TermQueryNode;
import com.wrmsr.search.dsl.query.term.StringQueryTerm;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

public class AppTest
        extends TestCase
{
    public AppTest(String testName)
    {
        super(testName);
    }

    public static Test suite()
    {
        return new TestSuite(AppTest.class);
    }

    private final List<List<SearchService.Doc>> docLists = ImmutableList.<List<SearchService.Doc>>builder()
            .add(ImmutableList.<SearchService.Doc>builder()
                    .add(new SearchService.Doc("Lucene in Action", "193398817"))
                    .add(new SearchService.Doc("Lucene for Dummies", "55320055Z"))
                    .build())
            .add(ImmutableList.<SearchService.Doc>builder()
                    .add(new SearchService.Doc("Managing Gigabytes", "55063554A"))
                    .add(new SearchService.Doc("The Art of Computer Science", "9900333X"))
                    .build())
            .build();

    public void testApp()
            throws Throwable
    {
        Injector injector = Guice.createInjector(new AppModule());
        SearchService searchService = injector.getInstance(SearchService.class);

        for (List<SearchService.Doc> docList : docLists) {
            for (SearchService.Doc doc : docList) {
                searchService.addDoc(doc);
            }
        }

        searchService.commit();

        QueryNode queryNode = new BooleanQueryNode(
                ImmutableList.of(
                        new BooleanQueryNode.ShouldClause(new MatchQueryNode("title", "lucene")),
                        new BooleanQueryNode.ShouldClause(new TermQueryNode("isbn", new StringQueryTerm("55063554A")))),
                1);

        /*
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        root = objectMapper.reader().withType(QueryNode.class).readValue(json);
        */

        List<SearchService.Hit> hits = searchService.searchDocs(queryNode, 10);
        for (SearchService.Hit hit : hits) {
            System.out.println(hit);
        }
    }
}
