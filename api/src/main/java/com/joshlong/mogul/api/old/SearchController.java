package com.joshlong.mogul.api.old;

import com.joshlong.lucene.DocumentWriteMapper;
import com.joshlong.lucene.LuceneTemplate;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.templates.MarkdownService;
import lombok.SneakyThrows;
import org.apache.lucene.document.*;
import org.apache.lucene.index.Term;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//@Deprecated
//@Controller
//@ResponseBody
//@RequestMapping("/podcasts")
class SearchController {

	/*
	 *
	 * private final Logger log = LoggerFactory.getLogger(getClass());
	 *
	 * private final MarkdownService markdownService;
	 *
	 * private final MogulService podcastRepository;
	 *
	 * private final LuceneTemplate luceneTemplate;
	 *
	 * private final int maxResults = 1000;
	 *
	 * private final Map<String, PodcastView> podcasts = new ConcurrentHashMap<>();
	 *
	 * private final ApplicationEventPublisher publisher;
	 *
	 * SearchController(ApplicationEventPublisher publisher, MarkdownService
	 * markdownService, MogulService podcastRepository, LuceneTemplate luceneTemplate) {
	 * this.podcastRepository = podcastRepository; this.publisher = publisher;
	 * this.markdownService = markdownService; this.luceneTemplate = luceneTemplate; }
	 *
	 * @GetMapping Collection<PodcastView> podcastViews() { return this.podcasts.values();
	 * }
	 *
	 * @GetMapping("/search") Collection<PodcastView> search(@RequestParam
	 * Optional<String> optionalQuery) {
	 *
	 * if (optionalQuery.isEmpty()) return this.podcasts.values();
	 *
	 * var query = optionalQuery.get(); Assert.notNull(query,
	 * "the query must not be null"); log.info("search query: [" + optionalQuery + "]");
	 * var idsThatMatch = this.searchIndex(query, maxResults); return
	 * this.podcasts.values() .stream() .filter(p ->
	 * idsThatMatch.contains(p.podcast().uid())) .collect(Collectors.toList()); }
	 *
	 * private List<String> searchIndex(String queryStr, int maxResults) { return
	 * this.luceneTemplate.search(queryStr, maxResults, document -> document.get("uid"));
	 * }
	 *
	 * @EventListener({ PodbeanEpisodePublishedEvent.class, ApplicationReadyEvent.class,
	 * SearchIndexInvalidatedEvent.class }) public void newPodcast() { this.refresh(); }
	 *
	 * private void refresh() {
	 *
	 * var computedPodcastViews = this.podcastRepository // .getAllPodcasts()//
	 * .stream()// .map(p -> new PodcastView(p,
	 * this.markdownService.convertMarkdownTemplateToHtml(p.description())))// .toList();
	 *
	 * this.luceneTemplate.write(computedPodcastViews, podcastView -> { var doc =
	 * buildPodcastDocument(podcastView); return new DocumentWriteMapper.DocumentWrite(new
	 * Term("uid", podcastView.podcast().uid()), doc); });
	 *
	 * this.publisher.publishEvent(new SearchIndexUpdatedEvent(computedPodcastViews));
	 *
	 * }
	 *
	 * @EventListener void updatedSearch(SearchIndexUpdatedEvent event) { synchronized
	 * (this.podcasts) { this.podcasts.clear(); event.podcastViews().forEach((pv) ->
	 * this.podcasts.put(pv.podcast().uid(), pv)); } }
	 *
	 * @SneakyThrows private Document buildPodcastDocument(PodcastView podcast) { var
	 * document = new Document(); document.add(new StringField("mogulId",
	 * Long.toString(podcast.podcast().mogulId()), Field.Store.YES)); document.add(new
	 * StringField("id", Long.toString(podcast.podcast().id()), Field.Store.YES));
	 * document.add(new StringField("uid", podcast.podcast().uid(), Field.Store.YES));
	 * document.add(new TextField("title", podcast.podcast().title(), Field.Store.YES));
	 * document.add(new TextField("description", html2text(podcast.html()),
	 * Field.Store.YES)); document.add(new LongPoint("time",
	 * podcast.podcast().date().getTime())); return document; }
	 *
	 * private String html2text(String html) { return Jsoup.parse(html).text(); }
	 */

}
