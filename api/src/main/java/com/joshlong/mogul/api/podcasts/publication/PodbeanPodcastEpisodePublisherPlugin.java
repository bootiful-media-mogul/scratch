package com.joshlong.mogul.api.podcasts.publication;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.managedfiles.CommonMediaTypes;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.utils.FileUtils;
import com.joshlong.podbean.EpisodeStatus;
import com.joshlong.podbean.EpisodeType;
import com.joshlong.podbean.PodbeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component(PodbeanPodcastEpisodePublisherPlugin.PLUGIN_NAME)
class PodbeanPodcastEpisodePublisherPlugin implements PodcastEpisodePublisherPlugin, BeanNameAware {

	public static final String PLUGIN_NAME = "podbean";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicReference<String> beanName = new AtomicReference<>();

	private final ManagedFileService managedFileService;

	private final PodbeanClient podbeanClient;

	PodbeanPodcastEpisodePublisherPlugin(ManagedFileService managedFileService, PodbeanClient podbeanClient) {
		this.managedFileService = managedFileService;
		this.podbeanClient = podbeanClient;
	}

	@Override
	public String name() {
		return this.beanName.get();
	}

	@Override
	public void setBeanName(@NonNull String name) {
		this.beanName.set(name);
	}

	@Override
	public boolean supports(Map<String, String> context, Episode payload) {
		return context.containsKey("clientId") && context.containsKey("clientSecret");
	}

	@Override
	public void publish(Map<String, String> context, Episode payload) {
		log.debug("publishing to podbean with context [" + context + "] and payload [" + payload
				+ "]. produced audio is [" + payload.producedAudio() + "]");
		// todo some sort of thread local in which to stash the context
		// to make it available to the multitenant TokenProvider

		var audio = payload.producedAudio();
		var graphic = payload.producedGraphic();

		for (var mf : Set.of(audio, graphic)) {
			Assert.state(mf.written() && mf.size() > 0,
					"the produced managed files must be " + "up-to-date before publication with Podbean");
		}

		log.debug("downloading produced graphic for podbean publication");
		var graphicResource = this.managedFileService.read(graphic.id());
		var tempProducedAudioFile = download(graphicResource, FileUtils.tempFile());

		log.debug("downloading produced audio for podbean publication");
		var producedAudioResource = this.managedFileService.read(audio.id());
		var tempGraphicFile = download(producedAudioResource, FileUtils.tempFile());

		var producedAudioAuthorization = this.podbeanClient.upload(CommonMediaTypes.MP3, tempProducedAudioFile);
		var producedGraphicAuthorization = this.podbeanClient.upload(CommonMediaTypes.JPG, tempGraphicFile);

		var podbeanEpisode = this.podbeanClient.publishEpisode(payload.title(), payload.description(),
				EpisodeStatus.DRAFT, EpisodeType.PUBLIC, producedAudioAuthorization.getFileKey(),
				producedGraphicAuthorization.getFileKey());
		log.debug("published episode to podbean: [" + podbeanEpisode + "]");

	}

	private static File download(Resource resource, File file) {

		try (var bin = new BufferedInputStream(resource.getInputStream());
				var bout = new BufferedOutputStream(new FileOutputStream(file))) {
			FileCopyUtils.copy(bin, bout);
		} //
		catch (Throwable throwable) {
			throw new RuntimeException("could not download a resource ", throwable);
		}
		return file;
	}

	@Override
	public void unpublish(Map<String, String> context, Episode payload) {
		log.debug("un-publishing to podbean with context [" + context + "] and payload [" + payload + "]");
	}

}
