package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Podcast;
import com.joshlong.mogul.api.PodcastDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@Controller
class PodcastsController {

    private final MogulService mogulService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    PodcastsController(MogulService mogulService) {
        this.mogulService = mogulService;
    }

    @MutationMapping
    PodcastDraft createPodcastDraft(@Argument String uid) {
        var pd = this.mogulService.createPodcastDraft(this.mogulService.getCurrentMogul().id(), uid);
        log.debug ( pd.toString());
        return pd;
    }

    @QueryMapping
    Collection<Podcast> podcasts() {
        return this.mogulService.getPodcastsByMogul(this.mogulService.getCurrentMogul().id());
    }

    /* todo look at the original code in v2 of the api to see how we handle this */
    @ResponseBody
    @PostMapping("/podcasts/drafts/{uid}")
    void uploadDataToPodcastDraft(@PathVariable String uid,
                                  @RequestBody MultipartFile file) {
        var draft = this.mogulService.getPodcastDraftByUid(uid);

    }

    @SchemaMapping(typeName = "PodcastDraft")
    String uploadPath(PodcastDraft podcastDraft) {
        return "/podcasts/drafts/" + podcastDraft.uid() + '/';
    }
}

