package edu.djc.crawler;

import edu.djc.common.URLFilter;
import edu.djc.db.dao.ResultDao;
import edu.djc.proxypool.ProxyPool;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

public class CrawlerStart {
    public static URLFilter urlFilter = new URLFilter();//使用了布隆过滤算法的URL去重
    public static ExecutorService crawlerThreadPool = Executors.newFixedThreadPool(30);//爬虫线程池
    public static Queue<Result> resultsQueue= new LinkedList<>();//结果队列，爬到的HTML并解析的结果放在这里

    public static int taskCounter = 0;
    public static final int MAX_TASK_NUM = 1000;

    public static boolean ifUseProxy=false;//是否使用代理池进行爬虫
    //public static Result queueTop = new Result();


    public CrawlerStart initial(String url){//由一个url初始化爬虫
        HtmlParserTool parserTool = new HtmlParserTool();
        Result.getQueue().add(parserTool.getResult(url,ifUseProxy));//将种子爬虫加入结果队列
        urlFilter.add(url);//将种子url加入去重filter
        return this;
    }
    public void launch()
    {
        ResultDao.persistent(Result.getQueue().peek());//将种子爬取结果进行数据持久化
        Result resultInQueueTop = null;
        while (true){
            while((resultInQueueTop = Result.getQueue().poll()) != null)//resultsQueue.size()>0
            {

                ResultDao.persistent(resultInQueueTop);
//            resultInQueueTop = resultsQueue.poll();//从任务队列取出一个结果
                ArrayList<FutureTask<Result>> tasks = new ArrayList<>();//创建任务列表


                //将爬取结果中的链接取出，查询是否爬过，没有则把url加入去重filter并加入任务列表
                for(String link:resultInQueueTop.getUrlLink())//若没有爬取过这个页面
                {
                    if(!urlFilter.contains(link))
                    {
                        urlFilter.add(link);
                        tasks.add(new FutureTask<>(new Crawler(link)));
                    }
                }

                //将任务列表中的任务提交给线程池进行处理
                for(FutureTask<Result> task:tasks)
                {
                    //System.out.println(">>>>>>>>>>Submit");
                    crawlerThreadPool.submit(task);
                }
                //TODO: Not Tested,Maybe Dangerous
                //taskCounter += tasks.size();

                //等待任务列表中的任务全部完成，完成后将结果放入结果队列准备下次取出，并把结果数据持久化
//            for(FutureTask<Result> task:tasks)
//            {
//                try {
//                    Result taskResult = task.get(5, TimeUnit.SECONDS);
//                    taskCounter--;
//                    if(!taskResult.getTitle().equals(""))
//                    {
//                        System.out.println(taskResult.getTitle());
//                        resultsQueue.add(taskResult);
//                        ResultDao.persistent(taskResult);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } catch (ExecutionException e) {
//                    e.printStackTrace();
//                } catch (TimeoutException e) {
//                    e.printStackTrace();
//                }
//            }

            }
            System.out.println("*********Already Crawled: "+Crawler.CrawlerID+" lemmas*********" + new Date());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public static void main(String[] args)
    {
        if(args.length!=0)
        {
            if("EnableProxy".equals(args[0]))
                ifUseProxy=true;
            else if("DisableProxy".equals(args[0]))
                ifUseProxy=false;
        }
        if(ifUseProxy)
        {
            ProxyPool proxyPool=new ProxyPool();
            proxyPool.initProxyPool();
            Thread proxyPoolThread=new Thread(proxyPool);
            proxyPoolThread.start();
        }
        long st = System.currentTimeMillis();
        new CrawlerStart().initial("http://baike.baidu.com/item/四川大学").launch();
        //new CrawlerStart().initial("https://baike.baidu.com/view/21087.html").launch();
        System.out.println("\n爬完总共花费时间："+ (System.currentTimeMillis()-st));
    }

}
