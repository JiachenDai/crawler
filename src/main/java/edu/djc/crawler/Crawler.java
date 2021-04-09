package edu.djc.crawler;

import java.util.concurrent.Callable;

public class Crawler implements Callable<Result> {
    public static int CrawlerID = 0;
    private String url = null;

    public Crawler(String url) {
        this.url = url;
        System.out.println("ok");
    }

    @Override
    public Result call() throws Exception {
        HtmlParserTool parserTool = new HtmlParserTool();
        Result result=parserTool.getResult(url,CrawlerStart.ifUseProxy);
        CrawlerID++;
        if (!result.getTitle().equals("")){
            System.out.println(result.getTitle());
            Result.getQueue().add(result);
        }
        return result;
    }

    public String getLinks() {
        return url;
    }

    public void setLinks(String links) {
        this.url = links;
    }
}
