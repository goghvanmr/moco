package com.github.dreamhead.moco.setting;

import com.github.dreamhead.moco.*;
import com.github.dreamhead.moco.internal.ActualHttpServer;
import com.github.dreamhead.moco.matcher.AndRequestMatcher;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import static com.google.common.collect.ImmutableList.of;

public class BaseSetting extends Setting implements ConfigApplier<BaseSetting> {
    private final ActualHttpServer httpServer;

    public BaseSetting(ActualHttpServer httpServer, RequestMatcher matcher) {
        super(matcher);
        this.httpServer = httpServer;
    }

    public boolean match(FullHttpRequest request) {
        return this.matcher.match(request);
    }

    public void writeToResponse(FullHttpRequest request, FullHttpResponse response) {
        this.handler.writeToResponse(request, response);
    }

    public BaseSetting apply(final MocoConfig config) {
        RequestMatcher appliedMatcher = this.matcher.apply(config);
        if (config.isFor("uri") && this.matcher == appliedMatcher) {
            appliedMatcher = new AndRequestMatcher(of(appliedMatcher, context(config.apply(""))));
        }

        BaseSetting setting = new BaseSetting(this.httpServer, appliedMatcher);
        setting.handler = this.handler.apply(config);
        return setting;
    }

    @Override
    protected void onResponseAttached(ResponseHandler handler) {
        httpServer.addSetting(this);
    }
}
