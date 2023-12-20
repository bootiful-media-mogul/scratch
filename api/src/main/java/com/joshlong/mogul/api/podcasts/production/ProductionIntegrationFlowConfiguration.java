package com.joshlong.mogul.api.podcasts.production;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.mogul.api.ApiProperties;
import com.joshlong.mogul.api.managedfiles.ManagedFile;
import com.joshlong.mogul.api.podcasts.Episode;
import com.joshlong.mogul.api.utils.IntegrationUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.MessageChannel;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * we have a python processor that will take the audio files we feed it and turn them into a final, produced audio form. this is the integration
 * that gets our {@link  com.joshlong.mogul.api.podcasts.Episode episodes} in and out of that processor.
 */
@Configuration
class ProductionIntegrationFlowConfiguration {


    static final String PRODUCTION_FLOW_REQUESTS = "podcast-episode-production-integration-flow-configuration";

    @Bean(PRODUCTION_FLOW_REQUESTS)
    DirectChannelSpec requests() {
        return MessageChannels.direct();
    }

    // todo is it possible to have some sort of dirty check? eg, if we have already generated the produced audio, and we did it
    //  _after_ we generated all the produced_(graphic|interview|introduction) files, then surely we
    //  can skip this step and just return immediately?

    @Bean
    IntegrationFlow episodeProductionIntegrationFlow(
            ApiProperties properties,
            AmqpTemplate amqpTemplate,
            @Qualifier(PRODUCTION_FLOW_REQUESTS) MessageChannel channel) throws Exception {

        var q = properties.podcasts().processor().amqp().requests();
        return IntegrationFlow
                .from(channel)

                // todo teach the python processor about these headers
                // todo could the assets for the bumpers and so on just be s3 uris as configuration settings in the settings table?
                //  settings that we send along with the request? so the processor would know nothing about our s3 assets
                //  it would get ALL assets from the request itself, which is also nice because we could rework it to support
                //  other users one day, too
                .transform((GenericTransformer<Episode, Map<String, String>>) source -> Map.of(
                        "introduction", source.producedIntroduction().s3Uri().toString(),
                        "interview", source.producedInterview().s3Uri().toString(),
                        "output", source.producedAudio().s3Uri().toString(),
                        "uid", UUID.randomUUID().toString() ,
                        "episodeId", Long.toString(source.id())
                ))
                .transform(new ObjectToJsonTransformer())
                .handle(Amqp.outboundGateway(amqpTemplate)//
                        .routingKey(q)//
                        .exchangeName(q)//
                )//
                //todo does any of this work?
                .transform( new JsonToObjectTransformer( Exported.class) )
                .transform(new GenericTransformer<Exported,Episode>() {
                    @Override
                    public Episode transform(Exported source) {
                        return null;
                    }
                })
                .handle(IntegrationUtils.debugHandler("the response from the processor came back!"))
                // todo we need to make sure we get the audio from s3, download it locally, and then
                //  re-upload it, this time going through the ManagedFileService#write method,
                //  so that we get all the side effects like the content length being updated.
                //  also, the return value from this flow should be the managedFile containing the produced audio, so let's make sure to download that
                .get();
    }

    record Exported (URI exported){}
}
