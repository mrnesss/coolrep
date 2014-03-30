package com.worldcanvas.app;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;


/**
 * Created by usuario on 3/29/14.
 */
public class ConexionLayer {
    //these are the service that we are going to use
    private static String saveCommentUrl = "saveMessage";
    private static String getRecentLocationCommentsUrl = "message";
    private static String allMessage = "allMessages";
    private static String HEADER = "Content-Type: application/x-www-form-urlencoded";


    //the base url
    private static final String BASE_URL = "http://anspacmty.org.mx/hck/api/";
    private static AsyncHttpClient client = new AsyncHttpClient();


    public static void saveComment(String author,String comment,String[] vPosition, String[] gPosition,AsyncHttpResponseHandler responseHandler){
        RequestParams params = new RequestParams();
        RequestParams paramsVPosition = new RequestParams();
        RequestParams paramsGPosition = new RequestParams();

        String resource = saveCommentUrl;
        params.put("author",author+"");
        params.put("comment",comment+"");
        paramsVPosition.put("x",vPosition[0]+"");
        paramsVPosition.put("y",vPosition[1]+"");
        paramsVPosition.put("z",vPosition[2]+"");
        params.put("vPosition",paramsVPosition);
        paramsGPosition.put("x",gPosition[0]+"");
        paramsGPosition.put("y",gPosition[1]+"");
        paramsGPosition.put("z",gPosition[2]+"");
        params.put("vPosition",paramsGPosition);
        ConexionLayer.post(resource, params, responseHandler);

    }

    public static void getComments(int x,int y,int z,int error,AsyncHttpResponseHandler responseHandler){
        RequestParams params = new RequestParams();

        String resource = getRecentLocationCommentsUrl;
        params.put("x",x+"");
        params.put("y",y+"");
        params.put("z",z+"");
        params.put("e",error+"");
        ConexionLayer.post(resource, params, responseHandler);
    }

    private static void post(String resource, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.addHeader("Content-Type"," application/x-www-form-urlencoded");
        String a = getAbsoluteUrl(resource);
        client.post(a, params, responseHandler);

    }


    private static void post(String resource, AsyncHttpResponseHandler responseHandler) {
        RequestParams params = new RequestParams();
        ConexionLayer.post(resource, params, responseHandler);
    }
    private static String getAbsoluteUrl(String resource) {
        return BASE_URL+resource;
    }



}

