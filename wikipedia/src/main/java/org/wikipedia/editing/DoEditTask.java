package org.wikipedia.editing;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.ApiTask;
import org.wikipedia.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.ExecutorService;
import org.wikipedia.page.Section;

import java.util.concurrent.Executor;

public class DoEditTask extends ApiTask<EditingResult> {
    private final PageTitle title;
    private final String sectionWikitext;
    private final int sectionID;

    public DoEditTask(Context context, PageTitle title, String sectionWikitext, int sectionID) {
        super(
                ExecutorService.getSingleton().getExecutor(DoEditTask.class, 1),
                ((WikipediaApp)context.getApplicationContext()).getAPIForSite(title.getSite())
        );
        this.title = title;
        this.sectionWikitext = sectionWikitext;
        this.sectionID = sectionID;
    }

    @Override
    public RequestBuilder buildRequest(Api api) {
        return api.action("edit")
                .param("title", title.getPrefixedText())
                .param("section", String.valueOf(sectionID))
                .param("text", sectionWikitext)
                .param("token", "+\\"); // Anonymous token. Replace properly when we have login
    }

    @Override
    protected ApiResult makeRequest(RequestBuilder builder) {
        return builder.post(); // Editing requires POST requests
    }

    @Override
    public EditingResult processResult(ApiResult result) throws Throwable {
        JSONObject resultJSON = result.asObject();
        Log.d("Wikipedia", resultJSON.toString(4));
        if (resultJSON.has("error")) {
            JSONObject errorJSON = resultJSON.optJSONObject("error");
            throw new EditingException(errorJSON.optString("code"), errorJSON.optString("info"));
        }
        String status = resultJSON.optJSONObject("edit").optString("result");
        if (status.equals("Success")) {
            return new SuccessEditResult();
        }
        // Handle other type of return codes here
        throw new RuntimeException("Failure happens");
    }
}
